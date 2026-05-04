package dev.gaonstack.gaonclimatecore.service

import dev.gaonstack.gaonclimatecore.auth.AuthenticatedApiKey
import dev.gaonstack.gaonclimatecore.api.dto.ClimateCurrentResponse
import dev.gaonstack.gaonclimatecore.api.dto.ClimateHistoryPointResponse
import dev.gaonstack.gaonclimatecore.api.dto.ClimateMeasurementRequest
import dev.gaonstack.gaonclimatecore.api.dto.ClimateMeasurementResponse
import dev.gaonstack.gaonclimatecore.domain.Device
import dev.gaonstack.gaonclimatecore.domain.DeviceMeasurement
import dev.gaonstack.gaonclimatecore.repository.DeviceMeasurementRepository
import dev.gaonstack.gaonclimatecore.repository.DeviceRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class ClimateService(
    private val deviceRepository: DeviceRepository,
    private val measurementRepository: DeviceMeasurementRepository,
    @Value("\${app.climate.history.window-seconds:3600}")
    private val historyWindowSeconds: Long,
    @Value("\${app.climate.history.bucket-seconds:600}")
    private val historyBucketSeconds: Long,
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

        val now = LocalDateTime.now()
        val measurement = measurementRepository.save(
            DeviceMeasurement(
                device = device,
                temperature = request.temperatureC ?: throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "temperature_c 값이 필요합니다.",
                ),
                humidity = request.humidity,
                measuredAt = now,
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
    fun current(authenticatedApiKey: AuthenticatedApiKey, deviceKey: String): ClimateCurrentResponse {
        val device = activeDevice(deviceKey, authenticatedApiKey)
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
    fun lastHour(authenticatedApiKey: AuthenticatedApiKey, deviceKey: String): List<ClimateHistoryPointResponse> {
        val device = activeDevice(deviceKey, authenticatedApiKey)
        val to = LocalDateTime.now()
        val from = to.minusSeconds(historyWindowSeconds)
        val bucketCount = historyBucketCount()
        val measurements = measurementRepository
            .findByDeviceDeviceKeyAndMeasuredAtBetweenOrderByMeasuredAtAsc(device.deviceKey, from, to)

        return (0 until bucketCount).map { index ->
            val bucketFrom = from.plusSeconds(index * historyBucketSeconds)
            val bucketTo = bucketFrom.plusSeconds(historyBucketSeconds)
            val measurement = measurements.lastOrNull {
                !it.measuredAt.isBefore(bucketFrom) && it.measuredAt.isBefore(bucketTo)
            }

            ClimateHistoryPointResponse(
                from = bucketFrom,
                to = bucketTo,
                measuredAt = measurement?.measuredAt,
                temperatureC = measurement?.temperature,
                humidity = measurement?.humidity,
            )
        }
    }

    private fun historyBucketCount(): Int {
        if (historyWindowSeconds <= 0 || historyBucketSeconds <= 0) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "온습도 변동 조회 설정이 올바르지 않습니다.")
        }
        if (historyWindowSeconds % historyBucketSeconds != 0L) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "온습도 변동 조회 구간 설정이 올바르지 않습니다.")
        }

        return (historyWindowSeconds / historyBucketSeconds).toInt()
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

    private fun activeDevice(deviceKey: String, authenticatedApiKey: AuthenticatedApiKey): Device {
        val device = activeDevice(deviceKey)
        if (device.user.id != authenticatedApiKey.userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "API key와 device_key의 사용자가 일치하지 않습니다.")
        }

        return device
    }

}
