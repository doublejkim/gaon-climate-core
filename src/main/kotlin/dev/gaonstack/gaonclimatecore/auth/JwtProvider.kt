package dev.gaonstack.gaonclimatecore.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import dev.gaonstack.gaonclimatecore.api.response.BusinessException
import dev.gaonstack.gaonclimatecore.api.response.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date

@Component
class JwtProvider(
    @Value("\${app.jwt.secret}")
    private val secret: String,
    @Value("\${app.jwt.access-token-expiry-seconds}")
    private val accessTokenExpirySeconds: Long,
) {
    private val secretBytes: ByteArray = secret.toByteArray()

    fun createAccessToken(userId: Long): String {
        val now = Date()
        val expiry = Date(now.time + accessTokenExpirySeconds * 1000L)
        val signer = MACSigner(secretBytes)
        val claims = JWTClaimsSet.Builder()
            .subject(userId.toString())
            .issueTime(now)
            .expirationTime(expiry)
            .build()
        val jwt = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)
        jwt.sign(signer)
        return jwt.serialize()
    }

    fun validateAndGetUserId(authorization: String?): Long {
        val token = extractBearerToken(authorization)
        return try {
            val jwt = SignedJWT.parse(token)
            if (!jwt.verify(MACVerifier(secretBytes))) {
                throw BusinessException(ErrorCode.INVALID_TOKEN)
            }
            val claims = jwt.jwtClaimsSet
            if (claims.expirationTime?.before(Date()) == true) {
                throw BusinessException(ErrorCode.TOKEN_EXPIRED)
            }
            claims.subject.toLong()
        } catch (e: BusinessException) {
            throw e
        } catch (e: Exception) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }
    }

    private fun extractBearerToken(authorization: String?): String {
        val value = authorization?.trim()
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN, "Authorization 헤더가 필요합니다.")
        if (!value.startsWith("Bearer ", ignoreCase = true)) {
            throw BusinessException(ErrorCode.INVALID_TOKEN, "Bearer 토큰이 필요합니다.")
        }
        return value.substringAfter(' ').trim().takeIf { it.isNotBlank() }
            ?: throw BusinessException(ErrorCode.INVALID_TOKEN, "Bearer 토큰이 비어 있습니다.")
    }
}
