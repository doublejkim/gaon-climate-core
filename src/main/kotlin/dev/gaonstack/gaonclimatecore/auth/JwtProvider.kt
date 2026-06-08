package dev.gaonstack.gaonclimatecore.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
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
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 액세스토큰입니다.")
            }
            val claims = jwt.jwtClaimsSet
            if (claims.expirationTime?.before(Date()) == true) {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "만료된 액세스토큰입니다.")
            }
            claims.subject.toLong()
        } catch (e: ResponseStatusException) {
            throw e
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 액세스토큰입니다.")
        }
    }

    private fun extractBearerToken(authorization: String?): String {
        val value = authorization?.trim()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization 헤더가 필요합니다.")
        if (!value.startsWith("Bearer ", ignoreCase = true)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer 토큰이 필요합니다.")
        }
        return value.substringAfter(' ').trim().takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer 토큰이 비어 있습니다.")
    }
}
