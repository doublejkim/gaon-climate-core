package dev.gaonstack.gaonclimatecore.service

import dev.gaonstack.gaonclimatecore.api.dto.AdminCreateDeviceRequest
import dev.gaonstack.gaonclimatecore.api.dto.DeviceResponse
import dev.gaonstack.gaonclimatecore.api.dto.RegisterDeviceRequest
import dev.gaonstack.gaonclimatecore.api.dto.RegisterDeviceResponse
import dev.gaonstack.gaonclimatecore.domain.Device
import dev.gaonstack.gaonclimatecore.domain.UserApiKey
import dev.gaonstack.gaonclimatecore.repository.DeviceRepository
import dev.gaonstack.gaonclimatecore.repository.UserApiKeyRepository
import dev.gaonstack.gaonclimatecore.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

@Service
class DeviceService(
    private val deviceRepository: DeviceRepository,
    private val userRepository: UserRepository,
    private val userApiKeyRepository: UserApiKeyRepository,
    private val apiKeyGenerator: ApiKeyGenerator,
) {
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
                createdAt = now,
                updatedAt = now,
            )
        )
        val apiKey = getOrCreateApiKey(device)

        return RegisterDeviceResponse(
            devices = listOf(device.toResponse()),
            apiKeyHash = apiKey.apiKeyHash,
        )
    }

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
                createdAt = now,
                updatedAt = now,
            )
        )
        val apiKey = getOrCreateApiKey(device)

        return RegisterDeviceResponse(
            devices = listOf(device.toResponse()),
            apiKeyHash = apiKey.apiKeyHash,
        )
    }

    private fun getOrCreateApiKey(device: Device): UserApiKey {
        val userId = device.user.id ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "사용자 정보가 올바르지 않습니다.",
        )
        userApiKeyRepository.findFirstByUserIdOrderByIdAsc(userId)?.let { return it }

        val generated = apiKeyGenerator.generate()
        val now = LocalDateTime.now()
        return userApiKeyRepository.save(
            UserApiKey(
                user = device.user,
                apiKeyHash = generated.apiKeyHash,
                keyPrefix = generated.keyPrefix,
                name = device.name,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    private fun Device.toResponse() = DeviceResponse(
        id = id ?: 0L,
        userId = user.id ?: 0L,
        deviceKey = deviceKey,
        name = name,
        locationName = locationName,
        status = status,
        lastSeenAt = lastSeenAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun String?.trimRequired(fieldName: String): String =
    this?.trim()?.takeIf { it.isNotBlank() }
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$fieldName 값이 필요합니다.")
