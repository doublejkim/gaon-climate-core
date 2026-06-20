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

// 디바이스 온보딩용 일회용 클레임 코드. 유저가 웹에서 발급받아 디바이스에 입력하면,
// 디바이스가 이 코드로 자신을 등록하고 device_key/api_key 를 자동으로 발급받는다.
@Entity
@Table(name = "device_claim_codes")
class DeviceClaimCode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    // 사람이 읽기 쉬운 코드 (예: GAON-4821-7K3Q). 대문자 정규화 후 저장
    @Column(nullable = false, unique = true, length = 32)
    var code: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    // 사용 완료 시각. null 이면 아직 사용되지 않음(유효)
    @Column(name = "used_at")
    var usedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun isUsed(): Boolean = usedAt != null

    fun isExpired(at: LocalDateTime): Boolean = expiresAt.isBefore(at)
}
