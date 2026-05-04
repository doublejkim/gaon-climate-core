package dev.gaonstack.gaonclimatecore.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "user_api_keys")
class UserApiKey(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "api_key_hash", nullable = false, unique = true)
    var apiKeyHash: String,

    @Column(name = "key_prefix", nullable = false, length = 20)
    var keyPrefix: String,

    @Column(length = 100)
    var name: String? = null,

    @Column(nullable = false, length = 30)
    var status: String = STATUS_ACTIVE,

    @Column(name = "last_used_at")
    var lastUsedAt: LocalDateTime? = null,

    @Column(name = "expires_at")
    var expiresAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
    }
}

