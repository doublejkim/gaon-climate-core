package dev.gaonstack.gaonclimatecore.repository

import dev.gaonstack.gaonclimatecore.domain.DeviceMeasurement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface DeviceMeasurementRepository : JpaRepository<DeviceMeasurement, Long> {
    fun findFirstByDeviceDeviceKeyOrderByMeasuredAtDesc(deviceKey: String): DeviceMeasurement?

    fun findByDeviceDeviceKeyAndMeasuredAtBetweenOrderByMeasuredAtAsc(
        deviceKey: String,
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<DeviceMeasurement>

    @Modifying
    @Query("delete from DeviceMeasurement m where m.measuredAt < :threshold")
    fun deleteOlderThan(threshold: LocalDateTime): Int
}

