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
