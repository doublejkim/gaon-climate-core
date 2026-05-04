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
@Table(name = "devices")
class Device(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "device_key", nullable = false, unique = true, length = 100)
    var deviceKey: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(name = "location_name", length = 100)
    var locationName: String? = null,

    @Column(nullable = false, length = 30)
    var status: String = STATUS_ACTIVE,

    @Column(name = "last_seen_at")
    var lastSeenAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
    }
}

