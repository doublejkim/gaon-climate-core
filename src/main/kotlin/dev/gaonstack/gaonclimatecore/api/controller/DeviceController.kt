package dev.gaonstack.gaonclimatecore.api.controller

import dev.gaonstack.gaonclimatecore.api.dto.ClaimCodeResponse
import dev.gaonstack.gaonclimatecore.api.dto.DeviceClaimRequest
import dev.gaonstack.gaonclimatecore.api.dto.DeviceRegistrationResponse
import dev.gaonstack.gaonclimatecore.api.dto.RegisterDeviceRequest
import dev.gaonstack.gaonclimatecore.auth.AuthenticatedJwtUser
import dev.gaonstack.gaonclimatecore.auth.AuthenticatedUser
import dev.gaonstack.gaonclimatecore.auth.JwtAuth
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
    // 2.1.1. 디바이스 등록 및 api key 생성: Bearer 토큰으로 유저 식별 후 device_key 기반 디바이스 등록, 신규 발급 시 raw api_key 반환(1회성)
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        user: AuthenticatedUser,
        @RequestBody request: RegisterDeviceRequest,
    ): DeviceRegistrationResponse {
        return deviceService.registerFromDevice(user.userId, request)
    }

    // 2.1.3. 디바이스 클레임 코드 발급: JWT 인증된 유저가 웹에서 일회용 코드를 발급받는다
    @PostMapping("/claim-codes")
    @JwtAuth
    @ResponseStatus(HttpStatus.CREATED)
    fun issueClaimCode(jwtUser: AuthenticatedJwtUser): ClaimCodeResponse {
        return deviceService.issueClaimCode(jwtUser.userId)
    }

    // 2.1.4. 디바이스 클레임: 디바이스가 클레임 코드로 자가 등록(인증 불필요, 코드 자체가 자격)
    @PostMapping("/claim")
    @ResponseStatus(HttpStatus.CREATED)
    fun claim(@RequestBody request: DeviceClaimRequest): DeviceRegistrationResponse {
        return deviceService.claimDevice(request)
    }
}
