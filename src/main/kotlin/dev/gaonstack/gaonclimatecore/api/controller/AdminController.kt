package dev.gaonstack.gaonclimatecore.api.controller

import dev.gaonstack.gaonclimatecore.api.dto.AdminCreateDeviceRequest
import dev.gaonstack.gaonclimatecore.api.dto.RegisterDeviceResponse
import dev.gaonstack.gaonclimatecore.service.DeviceService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/admin")
class AdminController(
    private val deviceService: DeviceService,
    @Value("\${app.admin-token:dev-admin-token}")
    private val adminToken: String,
) {
    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.CREATED)
    fun createDevice(
        @RequestHeader("X-Admin-Token", required = false) adminTokenHeader: String?,
        @RequestBody request: AdminCreateDeviceRequest,
    ): RegisterDeviceResponse {
        if (adminTokenHeader != adminToken) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 토큰이 유효하지 않습니다.")
        }

        return deviceService.createFromAdmin(request)
    }
}
