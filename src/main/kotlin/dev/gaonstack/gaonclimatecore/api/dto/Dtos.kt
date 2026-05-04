package dev.gaonstack.gaonclimatecore.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime

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

data class RegisterDeviceResponse(
    val device: DeviceResponse,
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
    val measuredAt: OffsetDateTime?,
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
    @JsonProperty("temperature_c")
    val temperatureC: BigDecimal,
    val humidity: BigDecimal?,
    val count: Int,
)
