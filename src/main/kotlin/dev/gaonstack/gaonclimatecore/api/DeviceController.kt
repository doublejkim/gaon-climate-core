package dev.gaonstack.gaonclimatecore.api

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
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        user: AuthenticatedUser,
        @RequestBody request: RegisterDeviceRequest,
    ): RegisterDeviceResponse {
        return deviceService.registerFromDevice(user.userId, request)
    }
}
