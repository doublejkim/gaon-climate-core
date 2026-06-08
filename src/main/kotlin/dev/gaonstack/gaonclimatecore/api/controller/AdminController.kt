package dev.gaonstack.gaonclimatecore.api.controller

import dev.gaonstack.gaonclimatecore.api.dto.AdminCreateDeviceRequest
import dev.gaonstack.gaonclimatecore.api.dto.AdminCreateRequest
import dev.gaonstack.gaonclimatecore.api.dto.AdminCreateResponse
import dev.gaonstack.gaonclimatecore.api.dto.AdminLoginRequest
import dev.gaonstack.gaonclimatecore.api.dto.AdminLoginResponse
import dev.gaonstack.gaonclimatecore.api.dto.AdminUserLookupRequest
import dev.gaonstack.gaonclimatecore.api.dto.AdminUserLookupResponse
import dev.gaonstack.gaonclimatecore.api.dto.RegisterDeviceResponse
import dev.gaonstack.gaonclimatecore.auth.AdminAuth
import dev.gaonstack.gaonclimatecore.service.AdminAuthService
import dev.gaonstack.gaonclimatecore.service.AdminUserService
import dev.gaonstack.gaonclimatecore.service.DeviceService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
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
    private val adminUserService: AdminUserService,
    private val adminAuthService: AdminAuthService,
    // 관리자 계정 부트스트랩 전용 고정 토큰. 최초 관리자 생성에만 사용
    @Value("\${app.admin-token:dev-admin-token}")
    private val adminBootstrapToken: String,
) {
    // 3.3.1. 관리자 계정 생성: X-Admin-Token(부트스트랩 고정 토큰) 검증 후 신규 관리자 등록
    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    fun createAdmin(
        @RequestHeader("X-Admin-Token", required = false) adminTokenHeader: String?,
        @RequestBody request: AdminCreateRequest,
    ): AdminCreateResponse {
        requireBootstrapToken(adminTokenHeader)

        return adminAuthService.createAdmin(request)
    }

    // 3.0.1. 관리자 로그인: email + password 검증 후 관리자 JWT 액세스토큰 발급
    @PostMapping("/login")
    fun login(@RequestBody request: AdminLoginRequest): AdminLoginResponse {
        return adminAuthService.login(request)
    }

    // 3.1.1. 관리자용 디바이스 및 api key 생성: 관리자 JWT 검증 후 email+name 기반 디바이스 등록, api_key_hash 반환
    @PostMapping("/devices")
    @AdminAuth
    @ResponseStatus(HttpStatus.CREATED)
    fun createDevice(
        @RequestBody request: AdminCreateDeviceRequest,
    ): RegisterDeviceResponse {
        return deviceService.createFromAdmin(request)
    }

    // 3.2.1. 관리자용 단일 유저 정보 및 api key 조회: 관리자 JWT 검증 후 user_id 또는 email로 유저+디바이스+api_key 정보 반환
    @GetMapping("/users")
    @AdminAuth
    fun getUser(
        @Valid @ModelAttribute request: AdminUserLookupRequest,
    ): AdminUserLookupResponse {
        return adminUserService.getUser(request)
    }

    private fun requireBootstrapToken(adminTokenHeader: String?) {
        if (adminTokenHeader != adminBootstrapToken) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 토큰이 유효하지 않습니다.")
        }
    }
}
