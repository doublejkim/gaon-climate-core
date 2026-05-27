package dev.gaonstack.gaonclimatecore.service

import dev.gaonstack.gaonclimatecore.auth.AuthenticatedApiKey
import dev.gaonstack.gaonclimatecore.domain.User
import dev.gaonstack.gaonclimatecore.domain.UserApiKey
import dev.gaonstack.gaonclimatecore.repository.UserApiKeyRepository
import dev.gaonstack.gaonclimatecore.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val userApiKeyRepository: UserApiKeyRepository,
) {
    // 2.1.1. Bearer 토큰(user id 또는 email)으로 활성 사용자 조회 — 디바이스 등록 시 유저 식별에 사용
    @Transactional(readOnly = true)
    fun requireUser(authorization: String?): User {
        val token = bearerToken(authorization)
        val user = token.toLongOrNull()?.let { userRepository.findById(it).orElse(null) }
            ?: userRepository.findByEmail(token)

        if (user == null || user.status != User.STATUS_ACTIVE) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 사용자 토큰입니다.")
        }

        return user
    }

    // 2.1.2. API key 해시로 유효한 api key 조회 및 last_used_at 갱신 — 온도/습도 저장 및 조회 시 인증에 사용
    @Transactional
    fun requireApiKey(authorization: String?): AuthenticatedApiKey {
        val token = bearerToken(authorization)
        val apiKey = userApiKeyRepository.findByApiKeyHash(token)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 API key 입니다.")

        val now = LocalDateTime.now()
        if (apiKey.status != UserApiKey.STATUS_ACTIVE || apiKey.user.status != User.STATUS_ACTIVE) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "비활성 API key 입니다.")
        }
        if (apiKey.expiresAt != null && apiKey.expiresAt!!.isBefore(now)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "만료된 API key 입니다.")
        }

        apiKey.lastUsedAt = now
        apiKey.updatedAt = now
        userApiKeyRepository.save(apiKey)

        return AuthenticatedApiKey(
            userId = apiKey.user.id ?: throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "API key 사용자 정보가 올바르지 않습니다.",
            ),
            apiKeyHash = apiKey.apiKeyHash,
        )
    }

    private fun bearerToken(authorization: String?): String {
        val value = authorization?.trim()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization 헤더가 필요합니다.")

        if (!value.startsWith("Bearer ", ignoreCase = true)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer 토큰이 필요합니다.")
        }

        return value.substringAfter(' ').trim().takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer 토큰이 비어 있습니다.")
    }
}
