package io.github.salomax.procureflow.app.catalog.service

import io.github.salomax.procureflow.app.catalog.domain.CatalogItem
import io.github.salomax.procureflow.app.catalog.repository.CatalogItemRepository
import io.github.salomax.procureflow.common.logging.LoggingUtils.logAuditData
import io.micronaut.http.server.exceptions.NotFoundException
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import mu.KotlinLogging
import java.util.UUID

@Singleton
open class CatalogItemService(
    private val repo: CatalogItemRepository
) {
    private val logger = KotlinLogging.logger {}

    fun search(query: String): List<CatalogItem> {
        val entities = if (query.isBlank()) {
            repo.findAll()
        } else {
            // Use full-text search that supports name and description, partial matches, and plural forms
            repo.searchByNameOrDescription(query.trim())
        }
        val items = entities.map { it.toDomain() }
        logAuditData("SEARCH", "CatalogItemService", null, "query" to (query.ifBlank { "*" }), "count" to items.size)
        logger.debug { "Search for '${query.ifBlank { "*" }}' returned ${items.size} results" }
        return items
    }

    fun get(id: UUID): CatalogItem? {
        val entity = repo.findById(id).orElse(null)
        val item = entity?.toDomain()
        if (item != null) {
            logAuditData("SELECT_BY_ID", "CatalogItemService", id.toString())
            logger.debug { "Catalog item found: ${item.name} (Category: ${item.category}, Status: ${item.status})" }
        } else {
            logAuditData("SELECT_BY_ID", "CatalogItemService", id.toString(), "result" to "NOT_FOUND")
            logger.debug { "Catalog item not found with ID: $id" }
        }
        return item
    }

    @Transactional
    open fun create(item: CatalogItem): CatalogItem {
        // Validate input
        require(item.name.isNotBlank()) { "Item name cannot be blank" }
        require(item.priceCents >= 0) { "Price cannot be negative" }

        val entity = item.toEntity()
        val saved = repo.save(entity)
        val result = saved.toDomain()
        logAuditData("INSERT", "CatalogItemService", result.id.toString(), 
            "name" to result.name, 
            "category" to result.category.name,
            "status" to result.status.name)
        logger.info { "Catalog item created successfully: ${result.name} (ID: ${result.id}, Status: ${result.status})" }
        return result
    }
}

