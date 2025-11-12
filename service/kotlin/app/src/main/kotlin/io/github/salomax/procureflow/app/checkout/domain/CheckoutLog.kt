package io.github.salomax.procureflow.app.checkout.domain

import io.github.salomax.procureflow.app.checkout.entity.CheckoutLogEntity
import java.time.Instant
import java.util.UUID

data class CheckoutItem(
    val catalogItemId: String,
    val name: String,
    val priceCents: Long,
    val quantity: Int
)

data class CheckoutLog(
    val id: UUID? = null,
    val userId: UUID? = null,
    val items: List<CheckoutItem>,
    val totalPriceCents: Long,
    val itemCount: Int,
    val status: CheckoutStatus = CheckoutStatus.COMPLETED,
    val createdAt: Instant = Instant.now()
) {
    fun toEntity(): CheckoutLogEntity {
        return CheckoutLogEntity(
            id = this.id,
            userId = this.userId,
            items = this.items,
            totalPriceCents = this.totalPriceCents,
            itemCount = this.itemCount,
            status = this.status,
            createdAt = this.createdAt
        )
    }
}

enum class CheckoutStatus {
    COMPLETED,
    FAILED
}

