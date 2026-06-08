package dev.gaonstack.gaonclimatecore.api.controller

import dev.gaonstack.gaonclimatecore.api.dto.ClimateCurrentResponse
import dev.gaonstack.gaonclimatecore.api.dto.ClimateHistoryPointResponse
import dev.gaonstack.gaonclimatecore.api.dto.ClimateMeasurementRequest
import dev.gaonstack.gaonclimatecore.api.dto.ClimateMeasurementResponse
import dev.gaonstack.gaonclimatecore.auth.AuthenticatedApiKey
import dev.gaonstack.gaonclimatecore.service.ClimateService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/climate")
class ClimateController(
    private val climateService: ClimateService,
) {
    // 2.1.2. 온도 습도 저장: API key 인증 후 device_key 기준으로 온도/습도 측정값 저장
    @PostMapping("/{deviceKey}")
    @ResponseStatus(HttpStatus.CREATED)
    fun saveMeasurement(
        @PathVariable deviceKey: String,
        apiKey: AuthenticatedApiKey,
        @RequestBody request: ClimateMeasurementRequest,
    ): ClimateMeasurementResponse {
        return climateService.saveMeasurement(apiKey, deviceKey, request)
    }

    // 2.3.1. 현재 온도/습도 조회: API key 인증 후 device_key 기준 최신 측정값 1건 반환
    @GetMapping("/{deviceKey}/current")
    fun current(
        @PathVariable deviceKey: String,
        apiKey: AuthenticatedApiKey,
    ): ClimateCurrentResponse =
        climateService.current(apiKey, deviceKey)

    // 2.3.2. 1시간 기준 온도/습도 변동 조회: API key 인증 후 1시간을 10분 버킷으로 나눠 6개 구간 변동 데이터 반환
    @GetMapping("/{deviceKey}/last-hour")
    fun lastHour(
        @PathVariable deviceKey: String,
        apiKey: AuthenticatedApiKey,
    ): List<ClimateHistoryPointResponse> =
        climateService.lastHour(apiKey, deviceKey)
}
