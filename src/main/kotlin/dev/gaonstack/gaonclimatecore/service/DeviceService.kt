package dev.gaonstack.gaonclimatecore.service

import dev.gaonstack.gaonclimatecore.api.dto.AdminCreateDeviceRequest
import dev.gaonstack.gaonclimatecore.api.dto.ClaimCodeResponse
import dev.gaonstack.gaonclimatecore.api.dto.DeviceClaimRequest
import dev.gaonstack.gaonclimatecore.api.dto.DeviceResponse
import dev.gaonstack.gaonclimatecore.api.dto.RegisterDeviceRequest
import dev.gaonstack.gaonclimatecore.api.dto.RegisterDeviceResponse
import dev.gaonstack.gaonclimatecore.api.response.BusinessException
import dev.gaonstack.gaonclimatecore.api.response.ErrorCode
import dev.gaonstack.gaonclimatecore.domain.Device
import dev.gaonstack.gaonclimatecore.domain.UserApiKey
import dev.gaonstack.gaonclimatecore.repository.DeviceRepository
import dev.gaonstack.gaonclimatecore.repository.UserApiKeyRepository
import dev.gaonstack.gaonclimatecore.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Service
class DeviceService(
    private val deviceRepository: DeviceRepository,
    private val userRepository: UserRepository,
    private val userApiKeyRepository: UserApiKeyRepository,
    private val apiKeyGenerator: ApiKeyGenerator,
    private val claimCodeStore: ClaimCodeStore,
    private val claimCodeGenerator: ClaimCodeGenerator,
    @Value("\${app.device.claim-code.ttl-seconds:600}")
    private val claimCodeTtlSeconds: Long,
) {
    // 2.1.1. 디바이스 등록 및 api key 생성: device_key 중복 확인 후 디바이스 저장, 유저 단위 api key 없으면 신규 발급
    @Transactional
    fun registerFromDevice(userId: Long, request: RegisterDeviceRequest): RegisterDeviceResponse {
        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 사용자입니다.")
        }
        val deviceKey = request.deviceKey.trimRequired("device_key")

        if (deviceRepository.existsByDeviceKey(deviceKey)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 device_key 입니다.")
        }

        val now = LocalDateTime.now()
        val device = deviceRepository.save(
            Device(
                user = user,
                deviceKey = deviceKey,
                name = request.name?.trim()?.takeIf { it.isNotBlank() } ?: deviceKey,
                locationName = request.locationName?.trim()?.takeIf { it.isNotBlank() },
                type = resolveDeviceType(request.type),
                createdAt = now,
                updatedAt = now,
            )
        )
        val issued = getOrCreateApiKey(device)

        return RegisterDeviceResponse(
            devices = listOf(device.toResponse()),
            apiKey = issued.rawKey,
        )
    }

    // 3.1.1. 관리자용 디바이스 생성: email로 유저 조회 후 디바이스 등록, device_key 미입력 시 UUID로 자동 생성
    @Transactional
    fun createFromAdmin(request: AdminCreateDeviceRequest): RegisterDeviceResponse {
        val email = request.email.trimRequired("email")
        val user = userRepository.findByEmail(email) ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "사용자를 찾을 수 없습니다.",
        )
        val deviceKey = request.deviceKey?.trim()?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString().replace("-", "")

        if (deviceRepository.existsByDeviceKey(deviceKey)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 device_key 입니다.")
        }

        val now = LocalDateTime.now()
        val device = deviceRepository.save(
            Device(
                user = user,
                deviceKey = deviceKey,
                name = request.deviceName?.trim()?.takeIf { it.isNotBlank() } ?: "TEST_DEVICE",
                locationName = request.locationName?.trim()?.takeIf { it.isNotBlank() },
                type = resolveDeviceType(request.type),
                createdAt = now,
                updatedAt = now,
            )
        )
        val issued = getOrCreateApiKey(device)

        return RegisterDeviceResponse(
            devices = listOf(device.toResponse()),
            apiKey = issued.rawKey,
        )
    }

    // 2.1.3. 디바이스 클레임 코드 발급: JWT 인증된 유저에게 일회용 코드를 발급한다(디바이스 온보딩용)
    @Transactional(readOnly = true)
    fun issueClaimCode(userId: Long): ClaimCodeResponse {
        // 유효한 유저인지만 확인(코드에는 userId 만 보관)
        if (!userRepository.existsById(userId)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 사용자입니다.")
        }
        val code = generateUniqueClaimCode()
        // 저장 TTL 에는 여유(grace) 시간을 더해, 클라이언트 카운트다운이 0이 되는 순간의
        // 네트워크 지연/막판 제출도 만료로 처리되지 않게 한다.
        claimCodeStore.issue(
            code = code,
            userId = userId,
            ttl = Duration.ofSeconds(claimCodeTtlSeconds + CLAIM_CODE_GRACE_SECONDS),
        )
        // 응답에는 grace 를 제외한 설정 TTL 을 내려, 웹이 설정값(예: 10분)부터 카운트하게 한다
        return ClaimCodeResponse(claimCode = code, expiresIn = claimCodeTtlSeconds)
    }

    // 2.1.4. 디바이스 클레임: 디바이스가 클레임 코드로 자가 등록한다.
    // 코드 1회용 소멸 → device_key 자동 생성 → 디바이스 저장 → api key 발급(유저 단위)
    @Transactional
    fun claimDevice(request: DeviceClaimRequest): RegisterDeviceResponse {
        val code = request.claimCode.trimRequired("claim_code").uppercase()
        // 조회+소멸을 원자적으로 처리. 없거나 만료/이미 사용된 코드는 모두 null → 유효하지 않음
        val userId = claimCodeStore.consume(code)
            ?: throw BusinessException(ErrorCode.INVALID_CLAIM_CODE)
        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 사용자입니다.")
        }

        val now = LocalDateTime.now()
        val deviceKey = generateUniqueDeviceKey()
        val device = deviceRepository.save(
            Device(
                user = user,
                deviceKey = deviceKey,
                name = request.name?.trim()?.takeIf { it.isNotBlank() } ?: deviceKey,
                locationName = request.locationName?.trim()?.takeIf { it.isNotBlank() },
                type = resolveDeviceType(request.type),
                createdAt = now,
                updatedAt = now,
            )
        )
        val issued = getOrCreateApiKey(device)

        return RegisterDeviceResponse(
            devices = listOf(device.toResponse()),
            apiKey = issued.rawKey,
        )
    }

    private fun generateUniqueClaimCode(): String {
        repeat(MAX_GENERATION_ATTEMPTS) {
            val code = claimCodeGenerator.generate()
            if (!claimCodeStore.exists(code)) return code
        }
        throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "클레임 코드 생성에 실패했습니다.")
    }

    private fun generateUniqueDeviceKey(): String {
        repeat(MAX_GENERATION_ATTEMPTS) {
            val key = UUID.randomUUID().toString().replace("-", "")
            if (!deviceRepository.existsByDeviceKey(key)) return key
        }
        throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "device_key 생성에 실패했습니다.")
    }

    // api key는 디바이스 단위로 1개 유지.
    // 디바이스 생성 직후 호출되므로 보통 신규 발급되며 raw 키를 함께 반환(1회성).
    // 이미 키가 있는 디바이스면 raw 는 알 수 없으므로 null.
    private fun getOrCreateApiKey(device: Device): IssuedApiKey {
        val deviceId = device.id ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "디바이스 정보가 올바르지 않습니다.",
        )
        userApiKeyRepository.findByDeviceId(deviceId)?.let {
            return IssuedApiKey(apiKey = it, rawKey = null)
        }

        val generated = apiKeyGenerator.generate()
        val now = LocalDateTime.now()
        val saved = userApiKeyRepository.save(
            UserApiKey(
                user = device.user,
                device = device,
                apiKeyHash = generated.apiKeyHash,
                keyPrefix = generated.keyPrefix,
                name = device.name,
                createdAt = now,
                updatedAt = now,
            )
        )
        return IssuedApiKey(apiKey = saved, rawKey = generated.rawKey)
    }

    // getOrCreateApiKey 결과. rawKey 는 이번 호출에서 새로 발급된 경우에만 채워진다(1회성).
    private data class IssuedApiKey(
        val apiKey: UserApiKey,
        val rawKey: String?,
    )

    private fun Device.toResponse() = DeviceResponse(
        id = id ?: 0L,
        userId = user.id ?: 0L,
        deviceKey = deviceKey,
        name = name,
        locationName = locationName,
        type = type,
        status = status,
        lastSeenAt = lastSeenAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    // 요청의 device type 을 검증해 반환. 미입력 시 기본값(TEMP_HUMIDITY)
    private fun resolveDeviceType(raw: String?): String {
        val type = raw?.trim()?.takeIf { it.isNotBlank() } ?: Device.TYPE_TEMP_HUMIDITY
        if (type !in Device.TYPES) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "지원하지 않는 device type 입니다. (${Device.TYPES.joinToString(", ")})",
            )
        }
        return type
    }

    companion object {
        // 클레임 코드/device_key 고유값 생성 시 충돌 재시도 횟수
        private const val MAX_GENERATION_ATTEMPTS = 10

        // 클레임 코드 저장 만료시각에 더하는 여유(grace) 시간(초).
        // 응답 expires_in(설정 TTL) 보다 실제 만료를 살짝 뒤로 두어 막판 제출을 보호한다.
        private const val CLAIM_CODE_GRACE_SECONDS = 2L
    }
}

fun String?.trimRequired(fieldName: String): String =
    this?.trim()?.takeIf { it.isNotBlank() }
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$fieldName 값이 필요합니다.")
