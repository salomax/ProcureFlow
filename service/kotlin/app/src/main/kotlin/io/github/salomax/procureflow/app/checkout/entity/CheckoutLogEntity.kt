package io.github.salomax.procureflow.app.checkout.entity

import io.github.salomax.procureflow.app.checkout.domain.CheckoutLog
import io.github.salomax.procureflow.app.checkout.domain.CheckoutStatus
import io.github.salomax.procureflow.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "checkout_logs", schema = "app")
open class CheckoutLogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "uuid")
    override val id: UUID?,

    @Column(name = "user_id", nullable = true)
    open var userId: UUID? = null,

    @OneToMany(mappedBy = "checkoutLog", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    open var items: MutableList<CheckoutLogItemEntity> = mutableListOf(),

    @Column(name = "total_price_cents", nullable = false)
    open var totalPriceCents: Long,

    @Column(name = "item_count", nullable = false)
    open var itemCount: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var status: CheckoutStatus = CheckoutStatus.COMPLETED,

    @Column(name = "created_at", nullable = false)
    open var createdAt: Instant = Instant.now()
) : BaseEntity<UUID?>(id) {
    fun toDomain(): CheckoutLog {
        return CheckoutLog(
            id = this.id,
            userId = this.userId,
            items = this.items.map { it.toDomain() },
            totalPriceCents = this.totalPriceCents,
            itemCount = this.itemCount,
            status = this.status,
            createdAt = this.createdAt
        )
    }
}

