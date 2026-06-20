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
    // 2.1.2. 온도 습도 저장: API key가 가리키는 디바이스와 path의 device_key 일치 확인 후 측정값 저장, device last_seen_at 갱신
    @Transactional
    fun saveMeasurement(
        authenticatedApiKey: AuthenticatedApiKey,
        deviceKey: String,
        request: ClimateMeasurementRequest,
    ): ClimateMeasurementResponse {
        val device = activeDevice(deviceKey, authenticatedApiKey)

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

    // 2.3.1. 현재 온도/습도 조회: device_key 기준 device_measurements 최신 1건 반환
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

    // 2.3.2. 1시간 변동 조회: 1시간을 10분 버킷으로 나눠 6개 구간의 온도/습도 변화 데이터 반환
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

    // 4. 스케줄링: 매일 01:00(KST)에 7일 초과된 device_measurements 데이터 삭제
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
        // 디바이스당 키 모델: 키가 가리키는 디바이스와 요청 대상 device_key 가 동일해야 한다
        if (device.id != authenticatedApiKey.deviceId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "API key와 device_key가 일치하지 않습니다.")
        }

        return device
    }

}
