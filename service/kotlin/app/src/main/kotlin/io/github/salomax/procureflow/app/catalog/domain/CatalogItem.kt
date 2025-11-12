package io.github.salomax.procureflow.app.catalog.domain

import io.github.salomax.procureflow.app.catalog.entity.CatalogItemEntity
import java.time.Instant
import java.util.UUID

data class CatalogItem(
    val id: UUID? = null,
    val name: String,
    val category: CatalogItemCategory,
    val priceCents: Long,
    val status: CatalogItemStatus = CatalogItemStatus.PENDING_APPROVAL,
    val description: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long = 0
) {
    fun toEntity(): CatalogItemEntity {
        return CatalogItemEntity(
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

enum class CatalogItemCategory {
    MATERIAL,
    SERVICE
}

enum class CatalogItemStatus {
    ACTIVE,
    PENDING_APPROVAL,
    INACTIVE
}

