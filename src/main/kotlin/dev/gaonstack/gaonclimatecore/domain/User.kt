package dev.gaonstack.gaonclimatecore.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 60)
    var password: String? = null,

    @Column(name = "password_key_index", nullable = false)
    var passwordKeyIndex: Int = 0,

    @Column(nullable = false, length = 30)
    var status: String = STATUS_PENDING,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        // 가입 직후 기본 상태. 관리자 승인 등으로 ACTIVE 전환 전까지 로그인 불가
        const val STATUS_PENDING = "PENDING"
        // 정상 활성 상태. 로그인 가능
        const val STATUS_ACTIVE = "ACTIVE"
        // 비활성 상태. 로그인 불가
        const val STATUS_INACTIVE = "INACTIVE"
    }
}

