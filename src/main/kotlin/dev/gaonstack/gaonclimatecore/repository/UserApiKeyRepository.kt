package dev.gaonstack.gaonclimatecore.repository

import dev.gaonstack.gaonclimatecore.domain.UserApiKey
import org.springframework.data.jpa.repository.JpaRepository

interface UserApiKeyRepository : JpaRepository<UserApiKey, Long> {
    fun findByApiKeyHash(apiKeyHash: String): UserApiKey?
    fun findFirstByUserIdOrderByIdAsc(userId: Long): UserApiKey?
    fun countByUserId(userId: Long): Long
}
