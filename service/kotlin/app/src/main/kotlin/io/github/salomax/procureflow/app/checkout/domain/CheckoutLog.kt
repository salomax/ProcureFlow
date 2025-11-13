package io.github.salomax.procureflow.app.checkout.domain

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
)

enum class CheckoutStatus {
    COMPLETED,
    FAILED
}

