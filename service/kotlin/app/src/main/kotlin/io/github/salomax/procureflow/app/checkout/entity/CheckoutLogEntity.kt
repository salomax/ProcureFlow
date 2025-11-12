package io.github.salomax.procureflow.app.checkout.entity

import io.github.salomax.procureflow.app.checkout.domain.CheckoutItem
import io.github.salomax.procureflow.app.checkout.domain.CheckoutLog
import io.github.salomax.procureflow.app.checkout.domain.CheckoutStatus
import io.github.salomax.procureflow.common.entity.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "checkout_logs", schema = "app")
open class CheckoutLogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "uuid")
    override val id: UUID?,

    @Column(name = "user_id", nullable = true)
    open var userId: UUID? = null,

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = CheckoutItemsConverter::class)
    open var items: List<CheckoutItem>,

    @Column(name = "total_price_cents", nullable = false)
    open var totalPriceCents: Long,

    @Column(name = "item_count", nullable = false)
    open var itemCount: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var status: CheckoutStatus = CheckoutStatus.COMPLETED,

    @Column(name = "created_at", nullable = false)
    open var createdAt: Instant = Instant.now()
) : BaseEntity<UUID?>(id) {
    fun toDomain(): CheckoutLog {
        return CheckoutLog(
            id = this.id,
            userId = this.userId,
            items = this.items,
            totalPriceCents = this.totalPriceCents,
            itemCount = this.itemCount,
            status = this.status,
            createdAt = this.createdAt
        )
    }
}

@Converter
class CheckoutItemsConverter : AttributeConverter<List<CheckoutItem>, String> {
    private val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())

    override fun convertToDatabaseColumn(attribute: List<CheckoutItem>?): String {
        if (attribute == null) return "[]"
        return try {
            objectMapper.writeValueAsString(attribute)
        } catch (e: Exception) {
            "[]"
        }
    }

    override fun convertToEntityAttribute(dbData: String?): List<CheckoutItem> {
        if (dbData == null || dbData == "[]" || dbData.isBlank()) {
            return emptyList()
        }
        
        return try {
            objectMapper.readValue(
                dbData,
                objectMapper.typeFactory.constructCollectionType(List::class.java, CheckoutItem::class.java)
            )
        } catch (e: Exception) {
            emptyList()
        }
    }
}

