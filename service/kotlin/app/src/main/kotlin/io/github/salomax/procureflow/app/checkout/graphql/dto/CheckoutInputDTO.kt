package io.github.salomax.procureflow.app.checkout.graphql.dto

import io.github.salomax.procureflow.common.graphql.BaseInputDTO
import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty

@Introspected
@Serdeable
data class CheckoutItemInputDTO(
    @field:NotEmpty(message = "catalogItemId must not be empty")
    var catalogItemId: String = "",
    @field:NotEmpty(message = "name must not be empty")
    var name: String = "",
    @field:Min(value = 0, message = "priceCents must be >= 0")
    var priceCents: Long = 0,
    @field:Min(value = 1, message = "quantity must be >= 1")
    var quantity: Int = 1
) : BaseInputDTO()

@Introspected
@Serdeable
data class CheckoutInputDTO(
    @field:NotEmpty(message = "items must not be empty")
    @field:Valid
    var items: List<CheckoutItemInputDTO> = emptyList(),
    @field:Min(value = 0, message = "totalPriceCents must be >= 0")
    var totalPriceCents: Long = 0,
    @field:Min(value = 0, message = "itemCount must be >= 0")
    var itemCount: Int = 0
) : BaseInputDTO()

