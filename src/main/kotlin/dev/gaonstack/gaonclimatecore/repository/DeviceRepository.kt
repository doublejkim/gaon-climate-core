package dev.gaonstack.gaonclimatecore.repository

import dev.gaonstack.gaonclimatecore.domain.Device
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceRepository : JpaRepository<Device, Long> {
    fun existsByDeviceKey(deviceKey: String): Boolean
    fun findByDeviceKey(deviceKey: String): Device?
}

