package dev.gaonstack.gaonclimatecore.service

import dev.gaonstack.gaonclimatecore.api.dto.LoginRequest
import dev.gaonstack.gaonclimatecore.api.dto.LoginResponse
import dev.gaonstack.gaonclimatecore.api.dto.SignUpRequest
import dev.gaonstack.gaonclimatecore.api.dto.TokenReissueRequest
import dev.gaonstack.gaonclimatecore.api.response.BusinessException
import dev.gaonstack.gaonclimatecore.api.response.ErrorCode
import dev.gaonstack.gaonclimatecore.api.dto.UserDeviceMeasurementResponse
import dev.gaonstack.gaonclimatecore.api.dto.UserDeviceResponse
import dev.gaonstack.gaonclimatecore.auth.JwtProvider
import dev.gaonstack.gaonclimatecore.domain.RefreshToken
import dev.gaonstack.gaonclimatecore.domain.User
import dev.gaonstack.gaonclimatecore.repository.DeviceMeasurementRepository
import dev.gaonstack.gaonclimatecore.repository.DeviceRepository
import dev.gaonstack.gaonclimatecore.repository.RefreshTokenRepository
import dev.gaonstack.gaonclimatecore.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val deviceRepository: DeviceRepository,
    private val measurementRepository: DeviceMeasurementRepository,
    private val jwtProvider: JwtProvider,
    // 콤마 구분 pepper 키 목록. 인덱스 순서가 곧 롤링 순서
    @Value("\${app.password.peppers}")
    private val peppersRaw: String,
) {
    private val peppers: List<String> by lazy { peppersRaw.split(",").map { it.trim() } }

    // 2.2.1. 회원가입: email + password 수신, bcrypt(password + pepper[latest]) 로 저장
    @Transactional
    fun signUp(request: SignUpRequest) {
        val email = request.email?.trim()?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "email 값이 필요합니다.")
        val password = request.password?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "password 값이 필요합니다.")

        if (userRepository.existsByEmail(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 가입되어있는 계정입니다.")
        }

        val pepperIndex = peppers.size - 1
        val pepper = peppers[pepperIndex]
        val passwordHash = BCrypt.hashpw(password + pepper, BCrypt.gensalt())
        val now = LocalDateTime.now()

        userRepository.save(
            User(
                email = email,
                name = email.substringBefore("@"),
                password = passwordHash,
                passwordKeyIndex = pepperIndex,
                // 가입 직후에는 PENDING 으로 등록. ACTIVE 전환은 추후 관리자 기능에서 처리
                status = User.STATUS_PENDING,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    // 2.2.2. 로그인: email + password 검증 후 JWT 액세스토큰(1일) + 리프레쉬토큰(3개월) 발급
    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val email = request.email?.trim()?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "email 값이 필요합니다.")
        val password = request.password?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "password 값이 필요합니다.")

        val user = userRepository.findByEmail(email)
            ?: throw BusinessException(ErrorCode.INVALID_CREDENTIALS)

        val storedHash = user.password
            ?: throw BusinessException(ErrorCode.INVALID_CREDENTIALS)

        val pepper = peppers.getOrNull(user.passwordKeyIndex)
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "인증 설정 오류입니다.")

        if (!BCrypt.checkpw(password + pepper, storedHash)) {
            throw BusinessException(ErrorCode.INVALID_CREDENTIALS)
        }

        // 자격 증명 확인 후 계정 상태 검증: 비밀번호를 모르는 사용자에게 계정 상태가 노출되지 않도록 비밀번호 검증을 먼저 수행
        when (user.status) {
            User.STATUS_ACTIVE -> Unit
            User.STATUS_PENDING -> throw BusinessException(ErrorCode.ACCOUNT_PENDING)
            else -> throw BusinessException(ErrorCode.ACCOUNT_INACTIVE)
        }

        val userId = user.id ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "사용자 정보가 올바르지 않습니다.")
        val accessToken = jwtProvider.createAccessToken(userId)
        val refreshToken = issueRefreshToken(user)

        return LoginResponse(accessToken = accessToken, refreshToken = refreshToken)
    }

    // 2.2.5. 토큰 재발급: 리프레쉬 토큰 검증 후 액세스토큰 + 리프레쉬토큰 재발급(회전)
    @Transactional
    fun reissue(request: TokenReissueRequest): LoginResponse {
        val token = request.refreshToken?.trim()?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "refresh_token 값이 필요합니다.")

        val stored = refreshTokenRepository.findByRefreshToken(token)
            ?: throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)

        // 만료 검증: 만료된 토큰은 정리 후 거절
        if (stored.expiresAt.isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored)
            throw BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED)
        }

        val user = stored.user
        when (user.status) {
            User.STATUS_ACTIVE -> Unit
            User.STATUS_PENDING -> throw BusinessException(ErrorCode.ACCOUNT_PENDING)
            else -> throw BusinessException(ErrorCode.ACCOUNT_INACTIVE)
        }
        val userId = user.id
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "사용자 정보가 올바르지 않습니다.")

        // 회전: 사용된 리프레쉬 토큰은 폐기하고 새 토큰을 발급해 재사용을 차단한다.
        refreshTokenRepository.delete(stored)
        val accessToken = jwtProvider.createAccessToken(userId)
        val newRefreshToken = issueRefreshToken(user)

        return LoginResponse(accessToken = accessToken, refreshToken = newRefreshToken)
    }

    // 2.2.3. 유저 디바이스 목록 조회: JWT 인증된 유저 소유의 devices 목록 반환
    @Transactional(readOnly = true)
    fun getDevices(userId: Long): List<UserDeviceResponse> {
        return deviceRepository.findAllByUserId(userId).map { device ->
            UserDeviceResponse(
                id = device.id ?: 0L,
                name = device.name,
                locationName = device.locationName,
                type = device.type,
                status = device.status,
                createdAt = device.createdAt,
            )
        }
    }

    // 2.2.4. 유저의 특정 디바이스 온도 조회: device_id 기준 device_measurements 최신 1건 반환
    @Transactional(readOnly = true)
    fun getLatestMeasurement(userId: Long, deviceId: Long): UserDeviceMeasurementResponse {
        val device = deviceRepository.findById(deviceId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "디바이스를 찾을 수 없습니다.")
        }
        if (device.user.id != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "해당 디바이스에 접근할 수 없습니다.")
        }
        val measurement = measurementRepository.findFirstByDeviceIdOrderByMeasuredAtDesc(deviceId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "측정 데이터가 없습니다.")

        return UserDeviceMeasurementResponse(
            deviceId = deviceId,
            temperature = measurement.temperature,
            humidity = measurement.humidity,
            measuredAt = measurement.measuredAt,
        )
    }

    private fun issueRefreshToken(user: User): String {
        val token = UUID.randomUUID().toString().replace("-", "")
        val now = LocalDateTime.now()
        refreshTokenRepository.save(
            RefreshToken(
                user = user,
                refreshToken = token,
                expiresAt = now.plusMonths(3),
                createdAt = now,
            )
        )
        return token
    }
}
