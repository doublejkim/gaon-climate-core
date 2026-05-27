package dev.gaonstack.gaonclimatecore.api.controller

import dev.gaonstack.gaonclimatecore.api.dto.RegisterDeviceRequest
import dev.gaonstack.gaonclimatecore.api.dto.RegisterDeviceResponse
import dev.gaonstack.gaonclimatecore.auth.AuthenticatedUser
import dev.gaonstack.gaonclimatecore.service.DeviceService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/devices")
class DeviceController(
    private val deviceService: DeviceService,
) {
    // 2.1.1. 디바이스 등록 및 api key 생성: Bearer 토큰으로 유저 식별 후 device_key 기반 디바이스 등록, api_key_hash 반환
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        user: AuthenticatedUser,
        @RequestBody request: RegisterDeviceRequest,
    ): RegisterDeviceResponse {
        return deviceService.registerFromDevice(user.userId, request)
    }
}
