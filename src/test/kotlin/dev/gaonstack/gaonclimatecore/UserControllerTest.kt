package dev.gaonstack.gaonclimatecore

import dev.gaonstack.gaonclimatecore.auth.JwtProvider
import dev.gaonstack.gaonclimatecore.domain.Device
import dev.gaonstack.gaonclimatecore.domain.DeviceMeasurement
import dev.gaonstack.gaonclimatecore.domain.User
import dev.gaonstack.gaonclimatecore.repository.DeviceMeasurementRepository
import dev.gaonstack.gaonclimatecore.repository.DeviceRepository
import dev.gaonstack.gaonclimatecore.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val deviceRepository: DeviceRepository,
    @Autowired private val measurementRepository: DeviceMeasurementRepository,
    @Autowired private val jwtProvider: JwtProvider,
) {
    private fun uid() = UUID.randomUUID().toString().replace("-", "").take(8)
    private fun uniqueEmail() = "${uid()}@ctrl.test"

    private fun savedUser(): User =
        userRepository.save(User(email = uniqueEmail(), name = "테스트"))

    // 로그인 테스트용 — BCrypt 패스워드를 직접 세팅하고 ACTIVE 상태로 저장(가입 기본값 PENDING 이라 로그인 불가)
    private fun savedUserWithPassword(email: String, rawPassword: String): User {
        val hash = BCrypt.hashpw(rawPassword + "test-pepper-key-0", BCrypt.gensalt())
        return userRepository.save(
            User(
                email = email,
                name = "테스트",
                password = hash,
                passwordKeyIndex = 0,
                status = User.STATUS_ACTIVE,
            )
        )
    }

    private fun savedDevice(user: User, locationName: String? = null): Device =
        deviceRepository.save(Device(user = user, deviceKey = uid(), name = "Device-${uid()}", locationName = locationName))

    private fun savedMeasurement(device: Device, temperature: BigDecimal = BigDecimal("22.5")): DeviceMeasurement =
        measurementRepository.save(DeviceMeasurement(device = device, temperature = temperature, measuredAt = LocalDateTime.now()))

    private fun bearerToken(userId: Long) = "Bearer ${jwtProvider.createAccessToken(userId)}"

    // -------------------------------------------------------------------------
    // POST /users/sign-up
    // -------------------------------------------------------------------------

    @Test
    fun `POST sign-up - 정상 가입시 200 OK`() {
        // signUp 은 void 반환으로 응답 바디가 없음 — 상태 코드만 검증
        mockMvc.post("/users/sign-up") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"${uniqueEmail()}","password":"password123"}"""
        }
            .andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `POST sign-up - email 누락시 400`() {
        mockMvc.post("/users/sign-up") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"password":"password123"}"""
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("BAD_REQUEST") }
            }
    }

    @Test
    fun `POST sign-up - password 누락시 400`() {
        mockMvc.post("/users/sign-up") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"${uniqueEmail()}"}"""
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("BAD_REQUEST") }
            }
    }

    @Test
    fun `POST sign-up - 중복 email 이면 409 와 에러 메시지`() {
        val email = uniqueEmail()
        mockMvc.post("/users/sign-up") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"first"}"""
        }
        mockMvc.post("/users/sign-up") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"second"}"""
        }
            .andExpect {
                status { isConflict() }
                jsonPath("$.code") { value("CONFLICT") }
                jsonPath("$.message") { value("이미 가입되어있는 계정입니다.") }
            }
    }

    // -------------------------------------------------------------------------
    // POST /users/login
    // -------------------------------------------------------------------------

    @Test
    fun `POST login - 정상 로그인시 200 과 access_token refresh_token 반환`() {
        val email = uniqueEmail()
        savedUserWithPassword(email, "pass1234")

        mockMvc.post("/users/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"pass1234"}"""
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.code") { value("OK") }
                jsonPath("$.data.access_token") { exists() }
                jsonPath("$.data.refresh_token") { exists() }
            }
    }

    @Test
    fun `POST login - 틀린 비밀번호면 401`() {
        val email = uniqueEmail()
        savedUserWithPassword(email, "correct")

        mockMvc.post("/users/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"wrong"}"""
        }
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("INVALID_CREDENTIALS") }
            }
    }

    @Test
    fun `POST login - 존재하지 않는 이메일이면 401`() {
        mockMvc.post("/users/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"nobody@ctrl.test","password":"pass"}"""
        }
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `POST login - email 누락시 400`() {
        mockMvc.post("/users/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"password":"pass"}"""
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("BAD_REQUEST") }
            }
    }

    @Test
    fun `POST login - password 누락시 400`() {
        mockMvc.post("/users/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"${uniqueEmail()}"}"""
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("BAD_REQUEST") }
            }
    }

    // -------------------------------------------------------------------------
    // GET /users/me/devices
    // -------------------------------------------------------------------------

    @Test
    fun `GET me-devices - Authorization 헤더 없으면 401`() {
        mockMvc.get("/users/me/devices")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET me-devices - 잘못된 JWT 이면 401`() {
        mockMvc.get("/users/me/devices") {
            header("Authorization", "Bearer invalid.token.value")
        }
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET me-devices - 정상 JWT 로 디바이스 목록 반환`() {
        val user = savedUser()
        val device = savedDevice(user, locationName = "거실")

        mockMvc.get("/users/me/devices") {
            header("Authorization", bearerToken(user.id!!))
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.code") { value("OK") }
                jsonPath("$.data[0].id") { value(device.id!!) }
                jsonPath("$.data[0].name") { value(device.name) }
                jsonPath("$.data[0].location_name") { value("거실") }
                jsonPath("$.data[0].status") { value(Device.STATUS_ACTIVE) }
                jsonPath("$.data[0].created_at") { exists() }
            }
    }

    @Test
    fun `GET me-devices - 디바이스 없으면 빈 배열 반환`() {
        val user = savedUser()

        mockMvc.get("/users/me/devices") {
            header("Authorization", bearerToken(user.id!!))
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.data") { isEmpty() }
            }
    }

    // -------------------------------------------------------------------------
    // GET /users/me/devices/{deviceId}/latest-measurement
    // -------------------------------------------------------------------------

    @Test
    fun `GET latest-measurement - Authorization 헤더 없으면 401`() {
        mockMvc.get("/users/me/devices/1/latest-measurement")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET latest-measurement - 정상 요청시 200 과 측정값 반환`() {
        val user = savedUser()
        val device = savedDevice(user)
        savedMeasurement(device, BigDecimal("24.5"))

        mockMvc.get("/users/me/devices/${device.id}/latest-measurement") {
            header("Authorization", bearerToken(user.id!!))
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.code") { value("OK") }
                jsonPath("$.data.device_id") { value(device.id!!) }
                jsonPath("$.data.temperature") { exists() }
                jsonPath("$.data.measured_at") { exists() }
            }
    }

    @Test
    fun `GET latest-measurement - 타인 디바이스에 접근하면 403`() {
        val owner = savedUser()
        val other = savedUser()
        val device = savedDevice(owner)
        savedMeasurement(device)

        mockMvc.get("/users/me/devices/${device.id}/latest-measurement") {
            header("Authorization", bearerToken(other.id!!))
        }
            .andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("FORBIDDEN") }
            }
    }

    @Test
    fun `GET latest-measurement - 존재하지 않는 디바이스면 404`() {
        val user = savedUser()

        mockMvc.get("/users/me/devices/999999/latest-measurement") {
            header("Authorization", bearerToken(user.id!!))
        }
            .andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("NOT_FOUND") }
            }
    }

    @Test
    fun `GET latest-measurement - 측정값이 없으면 404`() {
        val user = savedUser()
        val device = savedDevice(user)

        mockMvc.get("/users/me/devices/${device.id}/latest-measurement") {
            header("Authorization", bearerToken(user.id!!))
        }
            .andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("NOT_FOUND") }
            }
    }
}
