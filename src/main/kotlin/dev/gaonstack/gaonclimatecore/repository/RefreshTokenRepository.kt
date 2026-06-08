package dev.gaonstack.gaonclimatecore.repository

import dev.gaonstack.gaonclimatecore.domain.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByRefreshToken(refreshToken: String): RefreshToken?
}
