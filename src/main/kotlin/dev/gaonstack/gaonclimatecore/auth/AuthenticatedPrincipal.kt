package dev.gaonstack.gaonclimatecore.auth

data class AuthenticatedUser(
    val userId: Long,
)

data class AuthenticatedApiKey(
    val userId: Long,
    val apiKeyHash: String,
)

object AuthRequestAttributes {
    const val USER = "authenticatedUser"
    const val API_KEY = "authenticatedApiKey"
}

