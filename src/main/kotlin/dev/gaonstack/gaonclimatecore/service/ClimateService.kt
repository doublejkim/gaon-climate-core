package dev.gaonstack.gaonclimatecore.service

import dev.gaonstack.gaonclimatecore.auth.AuthenticatedApiKey
import dev.gaonstack.gaonclimatecore.api.ClimateCurrentResponse
import dev.gaonstack.gaonclimatecore.api.ClimateHistoryPointResponse
import dev.gaonstack.gaonclimatecore.api.ClimateMeasurementRequest
import dev.gaonstack.gaonclimatecore.api.ClimateMeasurementResponse
import dev.gaonstack.gaonclimatecore.domain.Device
import dev.gaonstack.gaonclimatecore.domain.DeviceMeasurement
import dev.gaonstack.gaonclimatecore.repository.DeviceMeasurementRepository
import dev.gaonstack.gaonclimatecore.repository.DeviceRepository
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class ClimateService(
    private val deviceRepository: DeviceRepository,
    private val measurementRepository: DeviceMeasurementRepository,
) {
    @Transactional
    fun saveMeasurement(
        authenticatedApiKey: AuthenticatedApiKey,
        deviceKey: String,
        request: ClimateMeasurementRequest,
    ): ClimateMeasurementResponse {
        val device = activeDevice(deviceKey)

        if (device.user.id != authenticatedApiKey.userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "API key와 device_key의 사용자가 일치하지 않습니다.")
        }

        val measuredAt = request.measuredAt?.toUtcLocalDateTime()
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "measured_at 값이 필요합니다.")

        val now = LocalDateTime.now()
        val measurement = measurementRepository.save(
            DeviceMeasurement(
                device = device,
                temperature = request.temperatureC ?: throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "temperature_c 값이 필요합니다.",
                ),
                humidity = request.humidity,
                measuredAt = measuredAt,
                createdAt = now,
            )
        )

        device.lastSeenAt = now
        device.updatedAt = now
        deviceRepository.save(device)

        return ClimateMeasurementResponse(
            id = measurement.id ?: 0L,
            deviceKey = device.deviceKey,
            temperatureC = measurement.temperature,
            humidity = measurement.humidity,
            measuredAt = measurement.measuredAt,
            createdAt = measurement.createdAt,
        )
    }

    @Transactional(readOnly = true)
    fun current(deviceKey: String): ClimateCurrentResponse {
        val device = activeDevice(deviceKey)
        val measurement = measurementRepository.findFirstByDeviceDeviceKeyOrderByMeasuredAtDesc(device.deviceKey)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "측정 데이터가 없습니다.")

        return ClimateCurrentResponse(
            deviceKey = device.deviceKey,
            temperatureC = measurement.temperature,
            humidity = measurement.humidity,
            measuredAt = measurement.measuredAt,
        )
    }

    @Transactional(readOnly = true)
    fun lastHour(deviceKey: String): List<ClimateHistoryPointResponse> {
        val device = activeDevice(deviceKey)
        val to = LocalDateTime.now()
        val from = to.minusHours(1)
        val measurements = measurementRepository
            .findByDeviceDeviceKeyAndMeasuredAtBetweenOrderByMeasuredAtAsc(device.deviceKey, from, to)

        return (0 until 6).mapNotNull { index ->
            val bucketFrom = from.plusMinutes(index * 10L)
            val bucketTo = bucketFrom.plusMinutes(10)
            val bucket = measurements.filter {
                !it.measuredAt.isBefore(bucketFrom) && it.measuredAt.isBefore(bucketTo)
            }

            if (bucket.isEmpty()) {
                null
            } else {
                ClimateHistoryPointResponse(
                    from = bucketFrom,
                    to = bucketTo,
                    temperatureC = bucket.map { it.temperature }.averageDecimal(),
                    humidity = bucket.mapNotNull { it.humidity }.averageDecimalOrNull(),
                    count = bucket.size,
                )
            }
        }
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    @Transactional
    fun deleteOldMeasurements() {
        measurementRepository.deleteOlderThan(LocalDateTime.now().minusDays(7))
    }

    private fun activeDevice(deviceKey: String): Device {
        val device = deviceRepository.findByDeviceKey(deviceKey)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "device_key를 찾을 수 없습니다.")

        if (device.status != Device.STATUS_ACTIVE) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "비활성 device_key 입니다.")
        }

        return device
    }

    private fun OffsetDateTime.toUtcLocalDateTime(): LocalDateTime =
        withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()

    private fun List<BigDecimal>.averageDecimal(): BigDecimal =
        fold(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal(size), 2, RoundingMode.HALF_UP)

    private fun List<BigDecimal>.averageDecimalOrNull(): BigDecimal? =
        takeIf { it.isNotEmpty() }?.averageDecimal()
}
