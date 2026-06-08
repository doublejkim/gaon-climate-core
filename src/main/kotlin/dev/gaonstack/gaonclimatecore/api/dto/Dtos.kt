package dev.gaonstack.gaonclimatecore.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import dev.gaonstack.gaonclimatecore.validation.ValidAdminUserLookup
import org.springframework.web.bind.annotation.BindParam
import java.math.BigDecimal
import java.time.LocalDateTime

data class RegisterDeviceRequest(
    @JsonProperty("device_key")
    val deviceKey: String?,
    val name: String? = null,
    @JsonProperty("location_name")
    val locationName: String? = null,
)

// 3.0.1. 관리자 로그인 요청
data class AdminLoginRequest(
    val email: String?,
    val password: String?,
)

// 3.0.1. 관리자 로그인 응답 (관리자 JWT 액세스토큰)
data class AdminLoginResponse(
    @JsonProperty("access_token")
    val accessToken: String,
)

// 3.3.1. 관리자 계정 생성 요청 (X-Admin-Token 부트스트랩 토큰으로 호출)
data class AdminCreateRequest(
    val email: String?,
    val password: String?,
    // 미지정 시 ADMIN
    val role: String? = null,
)

// 3.3.1. 관리자 계정 생성 응답 (password 는 응답하지 않음)
data class AdminCreateResponse(
    val id: Long,
    val email: String,
    val role: String,
    val status: String,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
)

data class AdminCreateDeviceRequest(
    val email: String?,
    @JsonProperty("device_name")
    val deviceName: String? = null,
    @JsonProperty("device_key")
    val deviceKey: String? = null,
    @JsonProperty("location_name")
    val locationName: String? = null,
)

@ValidAdminUserLookup
data class AdminUserLookupRequest(
    @param:BindParam("user_id")
    val userId: Long? = null,
    val email: String? = null,
)

data class AdminUserLookupResponse(
    val user: UserResponse,
    val devices: List<DeviceResponse>,
    @JsonProperty("api_key")
    val apiKey: ApiKeyResponse?,
)

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val status: String,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime,
)

data class ApiKeyResponse(
    val id: Long,
    @JsonProperty("api_key_hash")
    val apiKeyHash: String,
    @JsonProperty("key_prefix")
    val keyPrefix: String,
    val name: String?,
    val status: String,
    @JsonProperty("last_used_at")
    val lastUsedAt: LocalDateTime?,
    @JsonProperty("expires_at")
    val expiresAt: LocalDateTime?,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime,
)

data class RegisterDeviceResponse(
    val devices: List<DeviceResponse>,
    @JsonProperty("api_key_hash")
    val apiKeyHash: String,
)


data class DeviceResponse(
    val id: Long,
    @JsonProperty("user_id")
    val userId: Long,
    @JsonProperty("device_key")
    val deviceKey: String,
    val name: String,
    @JsonProperty("location_name")
    val locationName: String?,
    val status: String,
    @JsonProperty("last_seen_at")
    val lastSeenAt: LocalDateTime?,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime,
)

data class ClimateMeasurementRequest(
    @JsonProperty("temperature_c")
    val temperatureC: BigDecimal?,
    val humidity: BigDecimal? = null,
    @JsonProperty("measured_at")
    val measuredAt: LocalDateTime? = null,
)

data class ClimateMeasurementResponse(
    val id: Long,
    @JsonProperty("device_key")
    val deviceKey: String,
    @JsonProperty("temperature_c")
    val temperatureC: BigDecimal,
    val humidity: BigDecimal?,
    @JsonProperty("measured_at")
    val measuredAt: LocalDateTime,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
)

data class ClimateCurrentResponse(
    @JsonProperty("device_key")
    val deviceKey: String,
    @JsonProperty("temperature_c")
    val temperatureC: BigDecimal,
    val humidity: BigDecimal?,
    @JsonProperty("measured_at")
    val measuredAt: LocalDateTime,
)

data class ClimateHistoryPointResponse(
    val from: LocalDateTime,
    val to: LocalDateTime,
    @JsonProperty("measured_at")
    val measuredAt: LocalDateTime?,
    @JsonProperty("temperature_c")
    val temperatureC: BigDecimal?,
    val humidity: BigDecimal?,
)

// 2.2.1. 회원가입 요청
data class SignUpRequest(
    val email: String?,
    val password: String?,
)

// 2.2.2. 로그인 요청
data class LoginRequest(
    val email: String?,
    val password: String?,
)

// 2.2.2. 로그인 응답 (액세스토큰 + 리프레쉬토큰)
data class LoginResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("refresh_token")
    val refreshToken: String,
)

// 2.2.3. 유저 디바이스 목록 응답 항목
data class UserDeviceResponse(
    val id: Long,
    val name: String,
    @JsonProperty("location_name")
    val locationName: String?,
    val status: String,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
)

// 2.2.4. 유저의 특정 디바이스 최신 온도/습도 응답
data class UserDeviceMeasurementResponse(
    @JsonProperty("device_id")
    val deviceId: Long,
    val temperature: BigDecimal,
    val humidity: BigDecimal?,
    @JsonProperty("measured_at")
    val measuredAt: LocalDateTime,
)
