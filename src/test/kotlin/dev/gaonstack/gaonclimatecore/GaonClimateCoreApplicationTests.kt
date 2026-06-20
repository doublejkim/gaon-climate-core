package dev.gaonstack.gaonclimatecore

import dev.gaonstack.gaonclimatecore.auth.AdminJwtProvider
import dev.gaonstack.gaonclimatecore.domain.AdminUser
import dev.gaonstack.gaonclimatecore.domain.User
import dev.gaonstack.gaonclimatecore.repository.AdminUserRepository
import dev.gaonstack.gaonclimatecore.repository.DeviceRepository
import dev.gaonstack.gaonclimatecore.repository.UserApiKeyRepository
import dev.gaonstack.gaonclimatecore.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@SpringBootTest
@AutoConfigureMockMvc
class GaonClimateCoreApplicationTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val userApiKeyRepository: UserApiKeyRepository,
    @Autowired private val deviceRepository: DeviceRepository,
    @Autowired private val adminUserRepository: AdminUserRepository,
    @Autowired private val adminJwtProvider: AdminJwtProvider,
) {

    // 활성 관리자 1명을 보장하고 그 관리자 JWT 로 Authorization 헤더 값을 생성
    private fun adminBearer(): String {
        val admin = adminUserRepository.findByEmail("admin@example.com")
            ?: adminUserRepository.save(
                AdminUser(
                    email = "admin@example.com",
                    password = "unused-in-token-path",
                )
            )
        return "Bearer ${adminJwtProvider.createAccessToken(admin.id!!, admin.role)}"
    }

    @Test
    fun contextLoads() {
    }

    @Test
    fun `admin account is created with bootstrap token and can log in`() {
        mockMvc.post("/admin/admins") {
            header("X-Admin-Token", "test-admin-token")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "bootstrap-admin@example.com",
                  "password": "admin-pass-1234"
                }
            """.trimIndent()
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.code") { value("OK") }
                jsonPath("$.data.email") { value("bootstrap-admin@example.com") }
                jsonPath("$.data.role") { value("ADMIN") }
                jsonPath("$.data.status") { value("ACTIVE") }
            }

        mockMvc.post("/admin/login") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "bootstrap-admin@example.com",
                  "password": "admin-pass-1234"
                }
            """.trimIndent()
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.data.access_token") { exists() }
            }
    }

    @Test
    fun `admin account creation rejects wrong bootstrap token`() {
        mockMvc.post("/admin/admins") {
            header("X-Admin-Token", "wrong-token")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "should-not-exist@example.com",
                  "password": "whatever-1234"
                }
            """.trimIndent()
        }
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("UNAUTHORIZED") }
                jsonPath("$.message") { value("관리자 토큰이 유효하지 않습니다.") }
            }
    }

    @Test
    fun `admin device creation response is wrapped`() {
        userRepository.save(
            User(
                email = "test@example.com",
                name = "테스트 사용자",
            )
        )

        mockMvc.post("/admin/devices") {
            header("Authorization", adminBearer())
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "test@example.com",
                  "device_key": "abcdevice",
                  "location_name": "우리집"
                }
            """.trimIndent()
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.code") { value("OK") }
                jsonPath("$.message") { doesNotExist() }
                jsonPath("$.data.devices[0].device_key") { value("abcdevice") }
                jsonPath("$.data.devices[0].name") { value("TEST_DEVICE") }
            }
    }

    @Test
    fun `error response is wrapped`() {
        mockMvc.post("/admin/devices") {
            header("Authorization", "Bearer wrong-token")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "test@example.com",
                  "device_key": "error-device"
                }
            """.trimIndent()
        }
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("UNAUTHORIZED") }
                jsonPath("$.message") { value("유효하지 않은 관리자 토큰입니다.") }
                jsonPath("$.data") { doesNotExist() }
            }
    }

    @Test
    fun `admin user lookup returns user devices and api keys`() {
        userRepository.save(
            User(
                email = "lookup@example.com",
                name = "조회 사용자",
            )
        )

        mockMvc.post("/admin/devices") {
            header("Authorization", adminBearer())
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "lookup@example.com",
                  "device_key": "lookup-device",
                  "device_name": "LOOKUP_DEVICE"
                }
            """.trimIndent()
        }
            .andExpect {
                status { isCreated() }
            }

        mockMvc.get("/admin/users") {
            header("Authorization", adminBearer())
            param("email", "lookup@example.com")
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.code") { value("OK") }
                jsonPath("$.data.user.email") { value("lookup@example.com") }
                jsonPath("$.data.devices[0].device_key") { value("lookup-device") }
                jsonPath("$.data.devices[0].name") { value("LOOKUP_DEVICE") }
                jsonPath("$.data.api_keys[0].api_key_hash") { exists() }
                jsonPath("$.data.api_keys[0].device_id") { exists() }
            }
    }

    @Test
    fun `admin user lookup returns user without devices`() {
        val user = userRepository.save(
            User(
                email = "no-device@example.com",
                name = "디바이스 없는 사용자",
            )
        )

        mockMvc.get("/admin/users") {
            header("Authorization", adminBearer())
            param("user_id", user.id.toString())
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.data.user.email") { value("no-device@example.com") }
                jsonPath("$.data.devices") { isEmpty() }
                jsonPath("$.data.api_keys") { isEmpty() }
            }
    }

    @Test
    fun `admin user lookup validates required identifier and email format`() {
        mockMvc.get("/admin/users") {
            header("Authorization", adminBearer())
            param("email", "invalid-email")
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("BAD_REQUEST") }
            }
    }

    @Test
    fun `admin device creation issues a separate api key per device`() {
        val user = userRepository.save(
            User(
                email = "per-device-key@example.com",
                name = "디바이스별 키 사용자",
            )
        )

        mockMvc.post("/admin/devices") {
            header("Authorization", adminBearer())
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "per-device-key@example.com",
                  "device_key": "per-device-1"
                }
            """.trimIndent()
        }
            .andExpect {
                status { isCreated() }
                // 신규 디바이스마다 raw api key 가 1회성으로 반환된다
                jsonPath("$.data.api_key") { exists() }
            }

        mockMvc.post("/admin/devices") {
            header("Authorization", adminBearer())
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "per-device-key@example.com",
                  "device_key": "per-device-2"
                }
            """.trimIndent()
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.data.api_key") { exists() }
            }

        // 디바이스당 키 1개 → 유저는 디바이스 수만큼(2개) 키를 가진다
        assertEquals(2L, userApiKeyRepository.countByUserId(user.id!!))

        val device1 = deviceRepository.findByDeviceKey("per-device-1")!!
        val device2 = deviceRepository.findByDeviceKey("per-device-2")!!
        val key1 = userApiKeyRepository.findByDeviceId(device1.id!!)!!
        val key2 = userApiKeyRepository.findByDeviceId(device2.id!!)!!
        assertNotEquals(key1.apiKeyHash, key2.apiKeyHash)
    }
}
