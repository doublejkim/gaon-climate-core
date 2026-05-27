package dev.gaonstack.gaonclimatecore.api.controller

import dev.gaonstack.gaonclimatecore.api.dto.LoginRequest
import dev.gaonstack.gaonclimatecore.api.dto.LoginResponse
import dev.gaonstack.gaonclimatecore.api.dto.SignUpRequest
import dev.gaonstack.gaonclimatecore.api.dto.UserDeviceMeasurementResponse
import dev.gaonstack.gaonclimatecore.api.dto.UserDeviceResponse
import dev.gaonstack.gaonclimatecore.auth.AuthenticatedJwtUser
import dev.gaonstack.gaonclimatecore.auth.JwtAuth
import dev.gaonstack.gaonclimatecore.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
) {
    // 2.2.1. 회원가입: email + password 전달받아 신규 사용자 등록
    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.OK)
    fun signUp(@RequestBody request: SignUpRequest) {
        userService.signUp(request)
    }

    // 2.2.2. 로그인: email + password 검증 후 액세스토큰/리프레쉬토큰 발급
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): LoginResponse {
        return userService.login(request)
    }

    // 2.2.3. 유저 디바이스 목록 조회: JWT 인증 필요, 본인 소유 디바이스 목록 반환
    @GetMapping("/me/devices")
    @JwtAuth
    fun getDevices(jwtUser: AuthenticatedJwtUser): List<UserDeviceResponse> {
        return userService.getDevices(jwtUser.userId)
    }

    // 2.2.4. 유저의 특정 디바이스 온도 조회: JWT 인증 필요, device_id 기준 최신 측정값 1건 반환
    @GetMapping("/me/devices/{deviceId}/latest-measurement")
    @JwtAuth
    fun getLatestMeasurement(
        @PathVariable deviceId: Long,
        jwtUser: AuthenticatedJwtUser,
    ): UserDeviceMeasurementResponse {
        return userService.getLatestMeasurement(jwtUser.userId, deviceId)
    }
}
