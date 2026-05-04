package dev.gaonstack.gaonclimatecore.service

import dev.gaonstack.gaonclimatecore.api.dto.AdminUserLookupRequest
import dev.gaonstack.gaonclimatecore.api.dto.AdminUserLookupResponse
import dev.gaonstack.gaonclimatecore.api.dto.ApiKeyResponse
import dev.gaonstack.gaonclimatecore.api.dto.DeviceResponse
import dev.gaonstack.gaonclimatecore.api.dto.UserResponse
import dev.gaonstack.gaonclimatecore.domain.Device
import dev.gaonstack.gaonclimatecore.domain.User
import dev.gaonstack.gaonclimatecore.domain.UserApiKey
import dev.gaonstack.gaonclimatecore.repository.AdminUserLookupRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AdminUserService(
    private val adminUserLookupRepository: AdminUserLookupRepository,
) {
    @Transactional(readOnly = true)
    fun getUser(request: AdminUserLookupRequest): AdminUserLookupResponse {
        val user = adminUserLookupRepository.findUser(request.userId, request.email?.trim())
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")
        val userId = user.id ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 정보가 올바르지 않습니다.")

        return AdminUserLookupResponse(
            user = user.toResponse(),
            devices = adminUserLookupRepository.findDevices(userId).map { it.toResponse() },
            apiKey = adminUserLookupRepository.findApiKey(userId)?.toResponse(),
        )
    }

    private fun User.toResponse() = UserResponse(
        id = id ?: 0L,
        email = email,
        name = name,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

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

    private fun UserApiKey.toResponse() = ApiKeyResponse(
        id = id ?: 0L,
        apiKeyHash = apiKeyHash,
        keyPrefix = keyPrefix,
        name = name,
        status = status,
        lastUsedAt = lastUsedAt,
        expiresAt = expiresAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
