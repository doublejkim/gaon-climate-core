package dev.gaonstack.gaonclimatecore

import dev.gaonstack.gaonclimatecore.api.dto.LoginRequest
import dev.gaonstack.gaonclimatecore.api.dto.SignUpRequest
import dev.gaonstack.gaonclimatecore.api.response.BusinessException
import dev.gaonstack.gaonclimatecore.api.response.ErrorCode
import dev.gaonstack.gaonclimatecore.domain.Device
import dev.gaonstack.gaonclimatecore.domain.DeviceMeasurement
import dev.gaonstack.gaonclimatecore.domain.User
import dev.gaonstack.gaonclimatecore.repository.DeviceMeasurementRepository
import dev.gaonstack.gaonclimatecore.repository.DeviceRepository
import dev.gaonstack.gaonclimatecore.repository.RefreshTokenRepository
import dev.gaonstack.gaonclimatecore.repository.UserRepository
import dev.gaonstack.gaonclimatecore.service.UserService
import org.mindrot.jbcrypt.BCrypt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
class UserServiceTest(
    @Autowired private val userService: UserService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val refreshTokenRepository: RefreshTokenRepository,
    @Autowired private val deviceRepository: DeviceRepository,
    @Autowired private val measurementRepository: DeviceMeasurementRepository,
) {
    private fun uid() = UUID.randomUUID().toString().replace("-", "").take(8)
    private fun uniqueEmail() = "${uid()}@svc.test"

    private fun savedUser(email: String = uniqueEmail()): User =
        userRepository.save(User(email = email, name = "테스트"))

    // 가입 기본 상태는 PENDING 이므로, 로그인 테스트를 위해 ACTIVE 로 전환해 저장
    private fun savedUserWithPassword(email: String = uniqueEmail(), rawPassword: String = "pass1234"): User {
        userService.signUp(SignUpRequest(email = email, password = rawPassword))
        val user = userRepository.findByEmail(email)!!
        user.status = User.STATUS_ACTIVE
        return userRepository.save(user)
    }

    private fun savedDevice(user: User, locationName: String? = null): Device =
        deviceRepository.save(Device(user = user, deviceKey = uid(), name = "Device-${uid()}", locationName = locationName))

    private fun savedMeasurement(
        device: Device,
        temperature: BigDecimal = BigDecimal("22.0"),
        measuredAt: LocalDateTime = LocalDateTime.now(),
    ): DeviceMeasurement =
        measurementRepository.save(DeviceMeasurement(device = device, temperature = temperature, measuredAt = measuredAt))

    // -------------------------------------------------------------------------
    // signUp
    // -------------------------------------------------------------------------

    @Test
    fun `signUp - 정상 가입시 유저가 DB에 저장됨`() {
        val email = uniqueEmail()
        userService.signUp(SignUpRequest(email = email, password = "password123"))

        val saved = userRepository.findByEmail(email)
        assertNotNull(saved)
        assertEquals(email, saved.email)
        assertEquals(email.substringBefore("@"), saved.name)
    }

    @Test
    fun `signUp - 비밀번호는 bcrypt 해시로 저장됨`() {
        val email = uniqueEmail()
        val rawPassword = "mySecret"
        userService.signUp(SignUpRequest(email = email, password = rawPassword))

        val saved = userRepository.findByEmail(email)!!
        assertNotNull(saved.password)
        assertFalse(saved.password == rawPassword, "비밀번호가 평문으로 저장되면 안 됩니다")
        assertTrue(BCrypt.checkpw(rawPassword + "test-pepper-key-0", saved.password!!))
    }

    @Test
    fun `signUp - 중복 이메일이면 CONFLICT 예외`() {
        val email = uniqueEmail()
        userService.signUp(SignUpRequest(email = email, password = "pass123"))

        val ex = assertFailsWith<ResponseStatusException> {
            userService.signUp(SignUpRequest(email = email, password = "other"))
        }
        assertEquals(HttpStatus.CONFLICT, ex.statusCode)
        assertEquals("이미 가입되어있는 계정입니다.", ex.reason)
    }

    @Test
    fun `signUp - email 이 null 이면 BAD_REQUEST`() {
        val ex = assertFailsWith<ResponseStatusException> {
            userService.signUp(SignUpRequest(email = null, password = "pass123"))
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun `signUp - password 가 null 이면 BAD_REQUEST`() {
        val ex = assertFailsWith<ResponseStatusException> {
            userService.signUp(SignUpRequest(email = uniqueEmail(), password = null))
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Test
    fun `login - 정상 로그인시 액세스토큰과 리프레쉬토큰 반환`() {
        val email = uniqueEmail()
        val password = "pass1234"
        savedUserWithPassword(email, password)

        val response = userService.login(LoginRequest(email = email, password = password))

        assertTrue(response.accessToken.isNotBlank())
        assertEquals(32, response.refreshToken.length)
    }

    @Test
    fun `login - 정상 로그인시 리프레쉬토큰이 DB에 저장됨`() {
        val email = uniqueEmail()
        val password = "pass1234"
        val user = savedUserWithPassword(email, password)

        val response = userService.login(LoginRequest(email = email, password = password))

        val saved = refreshTokenRepository.findByRefreshToken(response.refreshToken)
        assertNotNull(saved)
        assertEquals(user.id, saved.user.id)
    }

    @Test
    fun `login - 틀린 비밀번호면 INVALID_CREDENTIALS`() {
        val email = uniqueEmail()
        savedUserWithPassword(email, "correctPass")

        val ex = assertFailsWith<BusinessException> {
            userService.login(LoginRequest(email = email, password = "wrongPass"))
        }
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.errorCode)
    }

    @Test
    fun `login - 존재하지 않는 이메일이면 INVALID_CREDENTIALS`() {
        val ex = assertFailsWith<BusinessException> {
            userService.login(LoginRequest(email = "nobody@svc.test", password = "pass"))
        }
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.errorCode)
    }

    @Test
    fun `login - email 이 null 이면 BAD_REQUEST`() {
        val ex = assertFailsWith<ResponseStatusException> {
            userService.login(LoginRequest(email = null, password = "pass"))
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun `login - password 가 null 이면 BAD_REQUEST`() {
        val ex = assertFailsWith<ResponseStatusException> {
            userService.login(LoginRequest(email = uniqueEmail(), password = null))
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    // -------------------------------------------------------------------------
    // getDevices
    // -------------------------------------------------------------------------

    @Test
    fun `getDevices - 유저 소유 디바이스 목록이 반환됨`() {
        val user = savedUser()
        val d1 = savedDevice(user, locationName = "거실")
        val d2 = savedDevice(user)

        val result = userService.getDevices(user.id!!)

        val ids = result.map { it.id }
        assertTrue(ids.contains(d1.id!!))
        assertTrue(ids.contains(d2.id!!))
    }

    @Test
    fun `getDevices - 응답에 id, name, location_name, status, created_at 포함`() {
        val user = savedUser()
        val device = savedDevice(user, locationName = "침실")

        val result = userService.getDevices(user.id!!)

        val item = result.first { it.id == device.id!! }
        assertEquals(device.name, item.name)
        assertEquals("침실", item.locationName)
        assertEquals(Device.STATUS_ACTIVE, item.status)
        assertNotNull(item.createdAt)
    }

    @Test
    fun `getDevices - 디바이스가 없으면 빈 목록 반환`() {
        val user = savedUser()

        val result = userService.getDevices(user.id!!)

        assertTrue(result.isEmpty())
    }

    // -------------------------------------------------------------------------
    // getLatestMeasurement
    // -------------------------------------------------------------------------

    @Test
    fun `getLatestMeasurement - device_id 와 temperature 가 올바르게 반환됨`() {
        val user = savedUser()
        val device = savedDevice(user)
        savedMeasurement(device, BigDecimal("24.5"))

        val result = userService.getLatestMeasurement(user.id!!, device.id!!)

        assertEquals(device.id, result.deviceId)
        assertEquals(0, BigDecimal("24.5").compareTo(result.temperature))
    }

    @Test
    fun `getLatestMeasurement - 여러 측정값 중 measuredAt 기준 최신 1건 반환`() {
        val user = savedUser()
        val device = savedDevice(user)
        val base = LocalDateTime.now()
        savedMeasurement(device, BigDecimal("20.0"), base.minusHours(1))
        savedMeasurement(device, BigDecimal("25.5"), base)

        val result = userService.getLatestMeasurement(user.id!!, device.id!!)

        assertEquals(0, BigDecimal("25.5").compareTo(result.temperature))
    }

    @Test
    fun `getLatestMeasurement - 타인 디바이스 접근시 FORBIDDEN`() {
        val owner = savedUser()
        val other = savedUser()
        val device = savedDevice(owner)
        savedMeasurement(device)

        val ex = assertFailsWith<ResponseStatusException> {
            userService.getLatestMeasurement(other.id!!, device.id!!)
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
    }

    @Test
    fun `getLatestMeasurement - 존재하지 않는 디바이스면 NOT_FOUND`() {
        val user = savedUser()

        val ex = assertFailsWith<ResponseStatusException> {
            userService.getLatestMeasurement(user.id!!, 999999L)
        }
        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
    }

    @Test
    fun `getLatestMeasurement - 측정값이 없으면 NOT_FOUND`() {
        val user = savedUser()
        val device = savedDevice(user)

        val ex = assertFailsWith<ResponseStatusException> {
            userService.getLatestMeasurement(user.id!!, device.id!!)
        }
        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
    }
}
