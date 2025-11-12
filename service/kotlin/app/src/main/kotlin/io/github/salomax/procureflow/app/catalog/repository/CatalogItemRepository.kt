package io.github.salomax.procureflow.app.catalog.repository

import io.github.salomax.procureflow.app.catalog.entity.CatalogItemEntity
import io.github.salomax.procureflow.app.catalog.domain.CatalogItemCategory
import io.github.salomax.procureflow.app.catalog.domain.CatalogItemStatus
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.UUID

@Repository
interface CatalogItemRepository : JpaRepository<CatalogItemEntity, UUID> {
    fun findByNameContainingIgnoreCase(name: String): List<CatalogItemEntity>
    fun findByCategory(category: CatalogItemCategory): List<CatalogItemEntity>
    fun findByStatus(status: CatalogItemStatus): List<CatalogItemEntity>
}

