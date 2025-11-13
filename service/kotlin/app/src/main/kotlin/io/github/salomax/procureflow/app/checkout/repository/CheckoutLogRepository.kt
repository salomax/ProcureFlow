package io.github.salomax.procureflow.app.checkout.repository

import io.github.salomax.procureflow.app.checkout.entity.CheckoutLogEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

@Repository
interface CheckoutLogRepository : JpaRepository<CheckoutLogEntity, UUID> {
    @Query("SELECT DISTINCT cl FROM CheckoutLogEntity cl LEFT JOIN FETCH cl.items WHERE cl.userId = :userId ORDER BY cl.createdAt DESC")
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<CheckoutLogEntity>
    
    @Query("SELECT DISTINCT cl FROM CheckoutLogEntity cl LEFT JOIN FETCH cl.items WHERE cl.id = :id")
    override fun findById(id: UUID): Optional<CheckoutLogEntity>
}

