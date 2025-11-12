package io.github.salomax.procureflow.app.catalog.entity

import io.github.salomax.procureflow.app.catalog.domain.CatalogItem
import io.github.salomax.procureflow.app.catalog.domain.CatalogItemCategory
import io.github.salomax.procureflow.app.catalog.domain.CatalogItemStatus
import io.github.salomax.procureflow.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "catalog_items", schema = "app")
open class CatalogItemEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "uuid")
    override val id: UUID?,

    @Column(nullable = false)
    open var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var category: CatalogItemCategory,

    @Column(name = "price_cents", nullable = false)
    open var priceCents: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var status: CatalogItemStatus = CatalogItemStatus.PENDING_APPROVAL,

    @Column(name = "created_at", nullable = false)
    open var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    open var updatedAt: Instant = Instant.now(),

    @Column(nullable = true)
    open var description: String? = null,

    @Version
    open var version: Long = 0
) : BaseEntity<UUID?>(id) {
    fun toDomain(): CatalogItem {
        return CatalogItem(
            id = this.id,
            name = this.name,
            category = this.category,
            priceCents = this.priceCents,
            status = this.status,
            description = this.description,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            version = this.version
        )
    }
}

