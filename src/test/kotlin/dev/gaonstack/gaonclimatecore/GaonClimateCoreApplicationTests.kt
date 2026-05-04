package dev.gaonstack.gaonclimatecore

import dev.gaonstack.gaonclimatecore.domain.User
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

@SpringBootTest
@AutoConfigureMockMvc
class GaonClimateCoreApplicationTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val userApiKeyRepository: UserApiKeyRepository,
) {

    @Test
    fun contextLoads() {
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
            header("X-Admin-Token", "test-admin-token")
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
            header("X-Admin-Token", "wrong-token")
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
                jsonPath("$.message") { value("관리자 토큰이 유효하지 않습니다.") }
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
            header("X-Admin-Token", "test-admin-token")
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
            header("X-Admin-Token", "test-admin-token")
            param("email", "lookup@example.com")
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.code") { value("OK") }
                jsonPath("$.data.user.email") { value("lookup@example.com") }
                jsonPath("$.data.devices[0].device_key") { value("lookup-device") }
                jsonPath("$.data.devices[0].name") { value("LOOKUP_DEVICE") }
                jsonPath("$.data.api_keys[0].api_key_hash") { exists() }
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
            header("X-Admin-Token", "test-admin-token")
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
            header("X-Admin-Token", "test-admin-token")
            param("email", "invalid-email")
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("BAD_REQUEST") }
            }
    }

    @Test
    fun `admin device creation reuses existing user api key`() {
        val user = userRepository.save(
            User(
                email = "reuse-key@example.com",
                name = "키 재사용 사용자",
            )
        )

        mockMvc.post("/admin/devices") {
            header("X-Admin-Token", "test-admin-token")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "reuse-key@example.com",
                  "device_key": "reuse-device-1"
                }
            """.trimIndent()
        }
            .andExpect {
                status { isCreated() }
            }

        val firstApiKey = userApiKeyRepository.findFirstByUserIdOrderByIdAsc(user.id!!)

        mockMvc.post("/admin/devices") {
            header("X-Admin-Token", "test-admin-token")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "reuse-key@example.com",
                  "device_key": "reuse-device-2"
                }
            """.trimIndent()
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.data.api_key_hash") { value(firstApiKey!!.apiKeyHash) }
            }

        assertEquals(1L, userApiKeyRepository.countByUserId(user.id!!))
    }
}
