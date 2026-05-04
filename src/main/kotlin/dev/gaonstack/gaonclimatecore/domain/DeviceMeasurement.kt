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
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "device_measurements")
class DeviceMeasurement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    var device: Device,

    @Column(nullable = false, precision = 5, scale = 2)
    var temperature: BigDecimal,

    @Column(precision = 5, scale = 2)
    var humidity: BigDecimal? = null,

    @Column(name = "measured_at", nullable = false)
    var measuredAt: LocalDateTime,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)

