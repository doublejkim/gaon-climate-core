package dev.gaonstack.gaonclimatecore.service

import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.UUID

@Component
class ApiKeyGenerator {
    // raw 키(클라이언트에 1회만 전달)와 그 sha256 해시(DB 저장용)를 함께 생성한다.
    fun generate(prefix: String = DEFAULT_PREFIX): GeneratedApiKey {
        val rawKey = prefix + UUID.randomUUID().toString().replace("-", "")
        return GeneratedApiKey(
            rawKey = rawKey,
            keyPrefix = prefix,
            apiKeyHash = sha256(rawKey),
        )
    }

    // 인증 시 클라이언트가 보낸 raw 키를 동일한 방식으로 해시해 DB의 저장 해시와 대조하기 위해 사용
    fun hash(rawKey: String): String = sha256(rawKey)

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val DEFAULT_PREFIX = "gck_"
    }
}

data class GeneratedApiKey(
    // 클라이언트(디바이스)가 인증에 사용할 실제 키. DB에는 저장하지 않고 발급 시 1회만 노출
    val rawKey: String,
    val keyPrefix: String,
    // DB 저장용 sha256(rawKey)
    val apiKeyHash: String,
)

