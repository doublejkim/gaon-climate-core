package dev.gaonstack.gaonclimatecore.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import dev.gaonstack.gaonclimatecore.validation.ValidAdminUserLookup
import org.springframework.web.bind.annotation.BindParam
import java.math.BigDecimal
import java.time.LocalDateTime

// 통신 테스트용 ping 응답 (서버 상태와 응답 시각 반환)
data class PingResponse(
    val message: String,
    val timestamp: LocalDateTime,
)

data class RegisterDeviceRequest(
    @JsonProperty("device_key")
    val deviceKey: String?,
    val name: String? = null,
    @JsonProperty("location_name")
    val locationName: String? = null,
    // 디바이스 타입(TEMP_HUMIDITY, MIC). 미입력 시 TEMP_HUMIDITY
    val type: String? = null,
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
    // 디바이스 타입(TEMP_HUMIDITY, MIC). 미입력 시 TEMP_HUMIDITY
    val type: String? = null,
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
    // 디바이스당 api key 1개이므로 유저는 디바이스 수만큼 키를 가질 수 있다
    @JsonProperty("api_keys")
    val apiKeys: List<ApiKeyResponse>,
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
    @JsonProperty("device_id")
    val deviceId: Long,
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

// 관리자용 디바이스 생성 응답. 관리자는 한 번에 여러 디바이스를 다룰 수 있어 목록 형태를 유지한다.
data class RegisterDeviceResponse(
    val devices: List<DeviceResponse>,
    // 신규 발급 시에만 채워지는 raw api key(1회성). 기존 키 재사용 시 null —
    // 이 경우 이미 발급받아 보관 중인 키를 그대로 사용한다.
    @JsonProperty("api_key")
    val apiKey: String?,
)

// 디바이스 단건 등록/클레임 응답(2.1.1, 2.1.4). 항상 디바이스 1개만 다루므로 단수로 응답한다.
data class DeviceRegistrationResponse(
    val device: DeviceResponse,
    // 신규 발급 시에만 채워지는 raw api key(1회성). 기존 키 재사용 시 null —
    // 이 경우 이미 발급받아 보관 중인 키를 그대로 사용한다.
    @JsonProperty("api_key")
    val apiKey: String?,
)

// 2.1.3. 디바이스 클레임 코드 발급 응답 (유저가 웹에서 발급 → 디바이스에 입력)
data class ClaimCodeResponse(
    @JsonProperty("claim_code")
    val claimCode: String,
    // 발급 시점 기준 남은 유효시간(초). 웹에서 이 값으로 카운트다운한다(클라이언트 시계 무관)
    @JsonProperty("expires_in")
    val expiresIn: Long,
)

// 2.1.4. 디바이스 클레임 요청 (디바이스가 클레임 코드로 자가 등록)
data class DeviceClaimRequest(
    @JsonProperty("claim_code")
    val claimCode: String?,
    val name: String? = null,
    @JsonProperty("location_name")
    val locationName: String? = null,
    // 디바이스 타입(TEMP_HUMIDITY, MIC). 미입력 시 TEMP_HUMIDITY
    val type: String? = null,
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
    val type: String,
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

// 2.2.5. 토큰 재발급 요청 (리프레쉬 토큰을 바디로 수신)
data class TokenReissueRequest(
    @JsonProperty("refresh_token")
    val refreshToken: String?,
)

// 2.2.3. 유저 디바이스 목록 응답 항목
data class UserDeviceResponse(
    val id: Long,
    val name: String,
    @JsonProperty("location_name")
    val locationName: String?,
    val type: String,
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
