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
    @PostMapping("/{deviceKey}")
    @ResponseStatus(HttpStatus.CREATED)
    fun saveMeasurement(
        @PathVariable deviceKey: String,
        apiKey: AuthenticatedApiKey,
        @RequestBody request: ClimateMeasurementRequest,
    ): ClimateMeasurementResponse {
        return climateService.saveMeasurement(apiKey, deviceKey, request)
    }

    @GetMapping("/{deviceKey}/current")
    fun current(
        @PathVariable deviceKey: String,
        apiKey: AuthenticatedApiKey,
    ): ClimateCurrentResponse =
        climateService.current(apiKey, deviceKey)

    @GetMapping("/{deviceKey}/last-hour")
    fun lastHour(
        @PathVariable deviceKey: String,
        apiKey: AuthenticatedApiKey,
    ): List<ClimateHistoryPointResponse> =
        climateService.lastHour(apiKey, deviceKey)
}
