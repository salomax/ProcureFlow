package io.github.salomax.procureflow.app.checkout.service

import io.github.salomax.procureflow.app.checkout.domain.CheckoutLog
import io.github.salomax.procureflow.app.checkout.repository.CheckoutLogRepository
import io.github.salomax.procureflow.common.logging.LoggingUtils.logAuditData
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import mu.KotlinLogging
import java.util.UUID

@Singleton
open class CheckoutLogService(
    private val repo: CheckoutLogRepository
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    open fun create(log: CheckoutLog): CheckoutLog {
        // Validate input
        require(log.items.isNotEmpty()) { "Checkout log must have at least one item" }
        require(log.totalPriceCents >= 0) { "Total price cannot be negative" }
        require(log.itemCount >= 0) { "Item count cannot be negative" }

        val entity = log.toEntity()
        val saved = repo.save(entity)
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

