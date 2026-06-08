package dev.gaonstack.gaonclimatecore.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "admin_users")
class AdminUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false, length = 60)
    var password: String,

    @Column(name = "password_key_index", nullable = false)
    var passwordKeyIndex: Int = 0,

    @Column(nullable = false, length = 30)
    var role: String = ROLE_ADMIN,

    @Column(nullable = false, length = 30)
    var status: String = STATUS_ACTIVE,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val ROLE_ADMIN = "ADMIN"
        const val ROLE_SUPER_ADMIN = "SUPER_ADMIN"
    }
}
