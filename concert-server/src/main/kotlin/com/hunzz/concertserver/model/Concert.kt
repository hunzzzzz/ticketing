package com.hunzz.concertserver.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "concerts")
class Concert(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_id", nullable = false, unique = true)
    val id: Long? = null,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "address", nullable = false)
    val address: String,

    @Column(name = "issued_at", nullable = false)
    val issuedAt: LocalDateTime,

    @Column(name = "started_at", nullable = false)
    val startedAt: LocalDateTime,
)