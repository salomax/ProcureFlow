package io.github.salomax.procureflow.app.catalog.repository

import io.github.salomax.procureflow.app.catalog.entity.CatalogItemEntity
import io.github.salomax.procureflow.app.catalog.domain.CatalogItemCategory
import io.github.salomax.procureflow.app.catalog.domain.CatalogItemStatus
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.UUID

@Repository
interface CatalogItemRepository : JpaRepository<CatalogItemEntity, UUID> {
    fun findByNameContainingIgnoreCase(name: String): List<CatalogItemEntity>
    fun findByCategory(category: CatalogItemCategory): List<CatalogItemEntity>
    fun findByStatus(status: CatalogItemStatus): List<CatalogItemEntity>
    
    /**
     * Full-text search that searches both name and description fields.
     * Supports:
     * - Partial matches (e.g., "paraf" matches "Parafuso")
     * - Plural forms (e.g., "Parafuso" matches "Parafusos") via Portuguese full-text search
     * - Case-insensitive search
     * 
     * Uses a combination of:
     * 1. PostgreSQL full-text search with Portuguese language for plural handling
     * 2. ILIKE for partial substring matching
     */
    @Query(
        value = """
            SELECT DISTINCT c.* FROM app.catalog_items c
            WHERE (
                -- Full-text search with Portuguese language (handles plural forms)
                to_tsvector('portuguese', COALESCE(c.name, '') || ' ' || COALESCE(c.description, '')) 
                @@ plainto_tsquery('portuguese', :query)
                OR
                -- Partial match using ILIKE (handles partial terms like "paraf" matching "Parafuso")
                c.name ILIKE '%' || :query || '%'
                OR
                c.description ILIKE '%' || :query || '%'
            )
        """,
        nativeQuery = true
    )
    fun searchByNameOrDescription(query: String): List<CatalogItemEntity>
}

