package io.github.salomax.procureflow.app.checkout.entity

import io.github.salomax.procureflow.app.catalog.entity.CatalogItemEntity
import io.github.salomax.procureflow.app.checkout.domain.CheckoutItem
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "checkout_log_items", schema = "app")
open class CheckoutLogItemEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "uuid")
    open val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkout_log_id", nullable = false)
    open var checkoutLog: CheckoutLogEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id", nullable = false, insertable = false, updatable = false)
    open var catalogItem: CatalogItemEntity? = null,

    @Column(name = "catalog_item_id", nullable = false)
    open var catalogItemId: UUID,

    @Column(nullable = false)
    open var name: String,

    @Column(name = "price_cents", nullable = false)
    open var priceCents: Long,

    @Column(nullable = false)
    open var quantity: Int
) {
    fun toDomain(): CheckoutItem {
        return CheckoutItem(
            catalogItemId = catalogItemId.toString(),
            name = this.name,
            priceCents = this.priceCents,
            quantity = this.quantity
        )
    }
}

