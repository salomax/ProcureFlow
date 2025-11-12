package io.github.salomax.procureflow.app.catalog.graphql.dto

import io.github.salomax.procureflow.app.catalog.domain.CatalogItemCategory
import io.github.salomax.procureflow.app.catalog.domain.CatalogItemStatus
import io.github.salomax.procureflow.common.graphql.BaseInputDTO
import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

/**
 * DTOs used for GraphQL inputs. Bean Validation annotations ensure proper constraints.
 */
@Introspected
@Serdeable
data class CatalogItemInputDTO(
    @field:NotBlank(message = "name must not be blank")
    var name: String = "",
    
    var category: CatalogItemCategory = CatalogItemCategory.MATERIAL,
    
    @field:Min(value = 0, message = "priceCents must be >= 0")
    var priceCents: Long = 0,
    
    var status: CatalogItemStatus = CatalogItemStatus.PENDING_APPROVAL,
    
    var description: String? = null
) : BaseInputDTO()

