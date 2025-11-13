package io.github.salomax.procureflow.app.checkout.service

import io.github.salomax.procureflow.app.catalog.repository.CatalogItemRepository
import io.github.salomax.procureflow.app.checkout.domain.CheckoutLog
import io.github.salomax.procureflow.app.checkout.entity.CheckoutLogEntity
import io.github.salomax.procureflow.app.checkout.entity.CheckoutLogItemEntity
import io.github.salomax.procureflow.app.checkout.repository.CheckoutLogRepository
import io.github.salomax.procureflow.common.logging.LoggingUtils.logAuditData
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import mu.KotlinLogging
import java.util.UUID

@Singleton
open class CheckoutLogService(
    private val repo: CheckoutLogRepository,
    private val catalogItemRepository: CatalogItemRepository
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    open fun create(log: CheckoutLog): CheckoutLog {
        // Validate input
        require(log.items.isNotEmpty()) { "Checkout log must have at least one item" }
        require(log.totalPriceCents >= 0) { "Total price cannot be negative" }
        require(log.itemCount >= 0) { "Item count cannot be negative" }

        // Create CheckoutLogEntity
        val checkoutLogEntity = CheckoutLogEntity(
            id = log.id,
            userId = log.userId,
            items = mutableListOf(),
            totalPriceCents = log.totalPriceCents,
            itemCount = log.itemCount,
            status = log.status,
            createdAt = log.createdAt
        )

        // Create CheckoutLogItemEntity instances for each item
        val checkoutLogItems = log.items.map { checkoutItem ->
            val catalogItemId = UUID.fromString(checkoutItem.catalogItemId)
            // Verify catalog item exists
            val catalogItemEntity = catalogItemRepository.findById(catalogItemId)
                .orElseThrow { IllegalArgumentException("Catalog item not found: ${checkoutItem.catalogItemId}") }

            CheckoutLogItemEntity(
                checkoutLog = checkoutLogEntity,
                catalogItem = catalogItemEntity,
                catalogItemId = catalogItemId,
                name = checkoutItem.name,
                priceCents = checkoutItem.priceCents,
                quantity = checkoutItem.quantity
            )
        }

        // Add items to the checkout log entity
        checkoutLogEntity.items.addAll(checkoutLogItems)

        // Save the entity (cascade will save the items)
        val saved = repo.save(checkoutLogEntity)
        val result = saved.toDomain()
        
        logAuditData("CHECKOUT", "CheckoutLogService", result.id.toString(),
            "userId" to (result.userId?.toString() ?: "null"),
            "itemCount" to result.itemCount.toString(),
            "totalPriceCents" to result.totalPriceCents.toString(),
            "status" to result.status.name)
        logger.info { "Checkout log created successfully: ID=${result.id}, Items=${result.itemCount}, Total=${result.totalPriceCents}, Status=${result.status}" }
        
        return result
    }

    fun get(id: UUID): CheckoutLog? {
        val entity = repo.findById(id).orElse(null)
        return entity?.toDomain()
    }

    fun findByUserId(userId: UUID): List<CheckoutLog> {
        val entities = repo.findByUserIdOrderByCreatedAtDesc(userId)
        return entities.map { it.toDomain() }
    }
}

