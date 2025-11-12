package io.github.salomax.procureflow.app.checkout.repository

import io.github.salomax.procureflow.app.checkout.entity.CheckoutLogEntity
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.UUID

@Repository
interface CheckoutLogRepository : JpaRepository<CheckoutLogEntity, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<CheckoutLogEntity>
    fun findByStatusOrderByCreatedAtDesc(status: io.github.salomax.procureflow.app.checkout.domain.CheckoutStatus): List<CheckoutLogEntity>
}

