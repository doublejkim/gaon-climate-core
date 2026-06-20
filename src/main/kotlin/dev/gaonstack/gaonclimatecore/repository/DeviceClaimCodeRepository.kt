package dev.gaonstack.gaonclimatecore.repository

import dev.gaonstack.gaonclimatecore.domain.DeviceClaimCode
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceClaimCodeRepository : JpaRepository<DeviceClaimCode, Long> {
    fun findByCode(code: String): DeviceClaimCode?
    fun existsByCode(code: String): Boolean
}
