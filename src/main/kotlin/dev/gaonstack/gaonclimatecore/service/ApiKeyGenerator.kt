package dev.gaonstack.gaonclimatecore.service

import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.UUID

@Component
class ApiKeyGenerator {
    fun generate(prefix: String = DEFAULT_PREFIX): GeneratedApiKey {
        val rawKey = prefix + UUID.randomUUID().toString().replace("-", "")
        return GeneratedApiKey(
            keyPrefix = prefix,
            apiKeyHash = sha256(rawKey),
        )
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val DEFAULT_PREFIX = "gck_"
    }
}

data class GeneratedApiKey(
    val keyPrefix: String,
    val apiKeyHash: String,
)

