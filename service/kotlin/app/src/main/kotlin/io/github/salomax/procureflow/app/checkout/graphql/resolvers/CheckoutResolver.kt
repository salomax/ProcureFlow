package io.github.salomax.procureflow.app.checkout.graphql.resolvers

import io.github.salomax.procureflow.app.checkout.domain.CheckoutItem
import io.github.salomax.procureflow.app.checkout.domain.CheckoutLog
import io.github.salomax.procureflow.app.checkout.domain.CheckoutStatus
import io.github.salomax.procureflow.app.checkout.graphql.dto.CheckoutInputDTO
import io.github.salomax.procureflow.app.checkout.graphql.dto.CheckoutItemInputDTO
import io.github.salomax.procureflow.app.checkout.service.CheckoutLogService
import io.micronaut.json.tree.JsonNode
import jakarta.inject.Singleton
import jakarta.validation.Validator

/**
 * Checkout resolver for GraphQL operations
 */
@Singleton
class CheckoutResolver(
    private val checkoutLogService: CheckoutLogService,
    private val validator: Validator
) {
    
    fun checkout(input: Map<String, Any?>): CheckoutLog {
        // Map GraphQL input to DTO
        val dto = mapToInputDTO(input)
        
        if (dto.items.isEmpty()) {
            throw IllegalArgumentException("Validation failed: items must not be empty")
        }
        
        // Validate non-collection fields using Bean Validation
        val totalPriceViolations = validator.validateProperty(dto, "totalPriceCents")
        val itemCountViolations = validator.validateProperty(dto, "itemCount")
        
        val allViolations = totalPriceViolations + itemCountViolations
        if (allViolations.isNotEmpty()) {
            val messages = allViolations.joinToString(", ") { it.message }
            throw IllegalArgumentException("Validation failed: $messages")
        }
        
        // Map DTO to domain entity
        val items = dto.items.map { itemDto ->
            CheckoutItem(
                catalogItemId = itemDto.catalogItemId,
                name = itemDto.name,
                priceCents = itemDto.priceCents,
                quantity = itemDto.quantity
            )
        }
        
        val log = CheckoutLog(
            userId = null, // TODO: Get from authentication context
            items = items,
            totalPriceCents = dto.totalPriceCents,
            itemCount = dto.itemCount,
            status = CheckoutStatus.COMPLETED
        )
        
        // Create via service
        return checkoutLogService.create(log)
    }
    
    private fun mapToInputDTO(input: Map<String, Any?>): CheckoutInputDTO {
        val itemsRaw = input["items"] ?: throw IllegalArgumentException("Field 'items' is required")
        val totalPriceCents = (input["totalPriceCents"] as? Number)?.toLong() ?: throw IllegalArgumentException("Field 'totalPriceCents' is required")
        val itemCount = (input["itemCount"] as? Number)?.toInt() ?: throw IllegalArgumentException("Field 'itemCount' is required")
        
        // Convert items from JsonNode (array) or List to List<Map<String, Any?>>
        val itemsList: List<Map<String, Any?>> = when {
            itemsRaw is JsonNode && itemsRaw.isArray -> {
                // Handle JsonNode that is an array
                (0 until itemsRaw.size()).mapNotNull { index ->
                    val jsonNode = itemsRaw[index]
                    if (jsonNode.isObject) {
                        jsonNode.entries().associate { entry ->
                            entry.key to convertJsonNode(entry.value)
                        }
                    } else {
                        null
                    }
                }
            }
            itemsRaw is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                itemsRaw as? List<Map<String, Any?>> ?: throw IllegalArgumentException("Field 'items' must be a list of objects")
            }
            else -> throw IllegalArgumentException("Field 'items' must be a list")
        }
        
        val items: List<CheckoutItemInputDTO> = itemsList.map { itemMap ->
            val catalogItemId = itemMap["catalogItemId"] as? String ?: throw IllegalArgumentException("Field 'catalogItemId' is required in item")
            val name = itemMap["name"] as? String ?: throw IllegalArgumentException("Field 'name' is required in item")
            val priceCents = (itemMap["priceCents"] as? Number)?.toLong() ?: throw IllegalArgumentException("Field 'priceCents' is required in item")
            val quantity = (itemMap["quantity"] as? Number)?.toInt() ?: throw IllegalArgumentException("Field 'quantity' is required in item")

            CheckoutItemInputDTO(
                catalogItemId = catalogItemId,
                name = name,
                priceCents = priceCents,
                quantity = quantity
            )
        }
        
        // Validate each item individually to ensure proper validation
        items.forEach { item ->
            val itemViolations = validator.validate(item)
            if (itemViolations.isNotEmpty()) {
                val messages = itemViolations.joinToString(", ") { it.message }
                throw IllegalArgumentException("Validation failed for item: $messages")
            }
        }
        
        return CheckoutInputDTO(
            items = items,
            totalPriceCents = totalPriceCents,
            itemCount = itemCount
        )
    }
    
    private fun convertJsonNode(node: JsonNode): Any? {
        return when {
            node.isNull -> null
            node.isNumber -> node.numberValue
            node.isBoolean -> node.booleanValue
            node.isString -> node.stringValue
            node.isArray -> {
                node.values().map { convertJsonNode(it) }
            }
            node.isObject -> {
                node.entries().associate { entry ->
                    entry.key to convertJsonNode(entry.value)
                }
            }
            else -> null
        }
    }
}

