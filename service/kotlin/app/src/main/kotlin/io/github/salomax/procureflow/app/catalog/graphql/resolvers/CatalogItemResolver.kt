package io.github.salomax.procureflow.app.catalog.graphql.resolvers

import io.github.salomax.procureflow.app.catalog.domain.CatalogItem
import io.github.salomax.procureflow.app.catalog.domain.CatalogItemCategory
import io.github.salomax.procureflow.app.catalog.domain.CatalogItemStatus
import io.github.salomax.procureflow.app.catalog.graphql.dto.CatalogItemInputDTO
import io.github.salomax.procureflow.app.catalog.service.CatalogItemService
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import jakarta.inject.Singleton
import jakarta.validation.Validator
import mu.KotlinLogging
import java.util.*

/**
 * CatalogItem resolver for GraphQL operations
 */
@Singleton
class CatalogItemResolver(
    private val catalogItemService: CatalogItemService,
    private val validator: Validator
) {
    private val logger = KotlinLogging.logger {}
    
    @ExecuteOn(TaskExecutors.BLOCKING)
    fun searchCatalogItems(query: String): List<CatalogItem> {
        return catalogItemService.search(query)
    }
    
    @ExecuteOn(TaskExecutors.BLOCKING)
    fun catalogItem(id: String): CatalogItem? {
        return try {
            val uuid = UUID.fromString(id)
            catalogItemService.get(uuid)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
    
    @ExecuteOn(TaskExecutors.BLOCKING)
    fun saveCatalogItem(input: Map<String, Any?>): CatalogItem {
        // Map GraphQL input to DTO
        val dto = mapToInputDTO(input)
        
        // Validate DTO
        val violations = validator.validate(dto)
        if (violations.isNotEmpty()) {
            val messages = violations.joinToString(", ") { it.message }
            throw IllegalArgumentException("Validation failed: $messages")
        }
        
        // Map DTO to domain entity
        val item = CatalogItem(
            name = dto.name.trim(),
            category = dto.category,
            priceCents = dto.priceCents,
            description = dto.description?.trim()?.takeIf { it.isNotBlank() },
            status = dto.status
        )
        
        // Create via service
        return catalogItemService.create(item)
    }
    
    private fun mapToInputDTO(input: Map<String, Any?>): CatalogItemInputDTO {
        val name = extractField<String>(input, "name")
        val categoryStr = extractField<String>(input, "category")
        val priceCents = extractField<Number>(input, "priceCents")?.toLong() ?: 0L
        val statusStr = extractField<String>(input, "status")
        val description = input["description"] as? String?
        
        val category = try {
            CatalogItemCategory.valueOf(categoryStr)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid category: $categoryStr. Must be one of: ${CatalogItemCategory.values().joinToString(", ")}")
        }
        
        val status = try {
            CatalogItemStatus.valueOf(statusStr)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid status: $statusStr. Must be one of: ${CatalogItemStatus.values().joinToString(", ")}")
        }
        
        return CatalogItemInputDTO(
            name = name,
            category = category,
            priceCents = priceCents,
            status = status,
            description = description
        )
    }
    
    private fun <T> extractField(input: Map<String, Any?>, name: String): T {
        @Suppress("UNCHECKED_CAST")
        return input[name] as? T ?: throw IllegalArgumentException("Field '$name' is required")
    }
}

