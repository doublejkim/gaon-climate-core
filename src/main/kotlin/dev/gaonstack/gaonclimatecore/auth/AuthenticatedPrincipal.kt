package dev.gaonstack.gaonclimatecore.auth

data class AuthenticatedUser(
    val userId: Long,
)

data class AuthenticatedApiKey(
    val userId: Long,
    val apiKeyHash: String,
)

// JWT 액세스토큰으로 인증된 유저 (2.2.x 유저용 기능에 사용)
data class AuthenticatedJwtUser(
    val userId: Long,
)

object AuthRequestAttributes {
    const val USER = "authenticatedUser"
    const val API_KEY = "authenticatedApiKey"
    const val JWT_USER = "authenticatedJwtUser"
}

