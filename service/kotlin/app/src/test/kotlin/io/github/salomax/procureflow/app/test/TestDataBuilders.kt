package io.github.salomax.procureflow.app.test

import io.github.salomax.procureflow.app.catalog.domain.CatalogItem
import io.github.salomax.procureflow.app.catalog.domain.CatalogItemCategory
import io.github.salomax.procureflow.app.catalog.domain.CatalogItemStatus
import java.time.Instant
import java.util.UUID

/**
 * Test data builders for creating test entities with sensible defaults
 */
object TestDataBuilders {

  fun graphQLQuery(query: String, variables: Map<String, Any>? = null): Map<String, Any> =
    mapOf("query" to query) + if (variables != null) mapOf("variables" to variables) else emptyMap()

  fun catalogItem(
      id: UUID? = null,
      name: String = "Test Catalog Item",
      category: CatalogItemCategory = CatalogItemCategory.MATERIAL,
      priceCents: Long = 10000L,
      status: CatalogItemStatus = CatalogItemStatus.PENDING_APPROVAL,
      description: String? = null,
      createdAt: Instant = Instant.now(),
      updatedAt: Instant = Instant.now()
    ): CatalogItem = CatalogItem(
      id = id,
      name = name,
      category = category,
      priceCents = priceCents,
      status = status,
      description = description,
      createdAt = createdAt,
      updatedAt = updatedAt
  )

    fun catalogItemInput(
        name: String = "Test Catalog Item",
        category: String = "MATERIAL",
        priceCents: Long = 10000L,
        status: String = "PENDING_APPROVAL",
        description: String? = null
    ): Map<String, Any> {
        val input = mutableMapOf<String, Any>(
            "name" to name,
            "category" to category,
            "priceCents" to priceCents,
            "status" to status
        )
        if (description != null) {
            input["description"] = description
        }
        return input
    }

    fun searchCatalogItemsQuery(query: String): Map<String, Any> = graphQLQuery(
        """
        query SearchCatalogItems(${'$'}query: String!) {
            searchCatalogItems(query: ${'$'}query) {
                id
                name
                category
                priceCents
                status
                description
                createdAt
                updatedAt
            }
        }
        """.trimIndent(),
        mapOf("query" to query)
    )

    fun saveCatalogItemMutation(
        name: String = "Test Catalog Item",
        category: String = "MATERIAL",
        priceCents: Long = 10000L,
        status: String = "PENDING_APPROVAL",
        description: String? = null
    ): Map<String, Any> = graphQLQuery(
        """
        mutation SaveCatalogItem(${'$'}input: CatalogItemInput!) {
            saveCatalogItem(input: ${'$'}input) {
                id
                name
                category
                priceCents
                status
                description
                createdAt
                updatedAt
            }
        }
        """.trimIndent(),
        mapOf("input" to catalogItemInput(name, category, priceCents, status, description))
    )

    fun getCatalogItemQuery(id: String): Map<String, Any> = graphQLQuery(
        """
        query GetCatalogItem(${'$'}id: ID!) {
            catalogItem(id: ${'$'}id) {
                id
                name
                category
                priceCents
                status
                description
                createdAt
                updatedAt
            }
        }
        """.trimIndent(),
        mapOf("id" to id)
    )

    fun checkoutItemInput(
        catalogItemId: String,
        name: String,
        priceCents: Long,
        quantity: Int = 1
    ): Map<String, Any> = mapOf(
        "catalogItemId" to catalogItemId,
        "name" to name,
        "priceCents" to priceCents,
        "quantity" to quantity
    )

    fun checkoutMutation(
        items: List<Map<String, Any>>,
        totalPriceCents: Long,
        itemCount: Int
    ): Map<String, Any> = graphQLQuery(
        """
        mutation Checkout(${'$'}input: CheckoutInput!) {
            checkout(input: ${'$'}input) {
                id
                userId
                items {
                    catalogItemId
                    name
                    priceCents
                    quantity
                }
                totalPriceCents
                itemCount
                status
                createdAt
            }
        }
        """.trimIndent(),
        mapOf(
            "input" to mapOf(
                "items" to items,
                "totalPriceCents" to totalPriceCents,
                "itemCount" to itemCount
            )
        )
    )
}
