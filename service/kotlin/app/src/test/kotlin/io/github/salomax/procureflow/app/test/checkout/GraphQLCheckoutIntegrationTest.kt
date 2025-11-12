package io.github.salomax.procureflow.app.test.checkout

import io.github.salomax.procureflow.app.test.TestDataBuilders
import io.github.salomax.procureflow.common.test.assertions.assertNoErrors
import io.github.salomax.procureflow.common.test.assertions.shouldBeJson
import io.github.salomax.procureflow.common.test.assertions.shouldBeSuccessful
import io.github.salomax.procureflow.common.test.assertions.shouldHaveNonEmptyBody
import io.github.salomax.procureflow.common.test.http.exchangeAsString
import io.github.salomax.procureflow.common.test.integration.BaseIntegrationTest
import io.github.salomax.procureflow.common.test.integration.PostgresIntegrationTest
import io.github.salomax.procureflow.common.test.json.read
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.json.tree.JsonNode
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder

@MicronautTest(startApplication = true)
@DisplayName("GraphQL Checkout Integration Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
@Tag("graphql")
@Tag("checkout")
@TestMethodOrder(MethodOrderer.Random::class)
class GraphQLCheckoutIntegrationTest : BaseIntegrationTest(), PostgresIntegrationTest {

    @Test
    fun `should checkout with single item via GraphQL mutation`() {
        // Arrange - create a catalog item first
        val itemName = "USB-C Cable - 1m"
        val itemPriceCents = 1500L
        val createMutation = TestDataBuilders.saveCatalogItemMutation(
            name = itemName,
            category = "MATERIAL",
            priceCents = itemPriceCents
        )

        val createResponse = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", createMutation).contentType(MediaType.APPLICATION_JSON)
        )
        createResponse.shouldBeSuccessful()
        val createPayload: JsonNode = json.read(createResponse)
        createPayload["errors"].assertNoErrors("Catalog item creation should succeed")

        val createdItem = createPayload["data"]["saveCatalogItem"]
        val catalogItemId = createdItem["id"].stringValue
        val quantity = 1
        val totalPriceCents = itemPriceCents * quantity

        val checkoutMutation = TestDataBuilders.checkoutMutation(
            items = listOf(
                TestDataBuilders.checkoutItemInput(
                    catalogItemId = catalogItemId,
                    name = itemName,
                    priceCents = itemPriceCents,
                    quantity = quantity
                )
            ),
            totalPriceCents = totalPriceCents,
            itemCount = quantity
        )

        val request = HttpRequest.POST("/graphql", checkoutMutation)
            .contentType(MediaType.APPLICATION_JSON)

        // Act
        val response = httpClient.exchangeAsString(request)
        response
            .shouldBeSuccessful()
            .shouldBeJson()
            .shouldHaveNonEmptyBody()

        // Assert
        val payload: JsonNode = json.read(response)
        payload["errors"].assertNoErrors()

        val data = payload["data"]
        Assertions.assertThat(data)
            .describedAs("GraphQL response must contain 'data.checkout'")
            .isNotNull()
        Assertions.assertThat(data["checkout"]).isNotNull

        val checkout: JsonNode = data["checkout"]
        Assertions.assertThat(checkout["id"]).isNotNull
        Assertions.assertThat(checkout["status"].stringValue).isEqualTo("COMPLETED")
        Assertions.assertThat((checkout["totalPriceCents"].numberValue).toLong()).isEqualTo(totalPriceCents)
        Assertions.assertThat((checkout["itemCount"].numberValue).toInt()).isEqualTo(quantity)
        Assertions.assertThat(checkout["createdAt"]).isNotNull

        val items = checkout["items"]
        Assertions.assertThat(items.isArray).isTrue()
        Assertions.assertThat(items.size()).isEqualTo(1)

        val item = items[0]
        Assertions.assertThat(item["catalogItemId"].stringValue).isEqualTo(catalogItemId)
        Assertions.assertThat(item["name"].stringValue).isEqualTo(itemName)
        Assertions.assertThat((item["priceCents"].numberValue).toLong()).isEqualTo(itemPriceCents)
        Assertions.assertThat((item["quantity"].numberValue).toInt()).isEqualTo(quantity)
    }

    @Test
    fun `should checkout with multiple items via GraphQL mutation`() {
        // Arrange - create multiple catalog items
        val item1Name = "USB-C Cable - 1m"
        val item1PriceCents = 1500L
        val item2Name = "Wireless Mouse"
        val item2PriceCents = 2500L
        val item3Name = "Ergonomic Keyboard"
        val item3PriceCents = 8000L

        val createMutation1 = TestDataBuilders.saveCatalogItemMutation(
            name = item1Name,
            category = "MATERIAL",
            priceCents = item1PriceCents
        )
        val createMutation2 = TestDataBuilders.saveCatalogItemMutation(
            name = item2Name,
            category = "MATERIAL",
            priceCents = item2PriceCents
        )
        val createMutation3 = TestDataBuilders.saveCatalogItemMutation(
            name = item3Name,
            category = "MATERIAL",
            priceCents = item3PriceCents
        )

        val createResponse1 = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", createMutation1).contentType(MediaType.APPLICATION_JSON)
        )
        createResponse1.shouldBeSuccessful()
        val createPayload1: JsonNode = json.read(createResponse1)
        createPayload1["errors"].assertNoErrors()

        val createResponse2 = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", createMutation2).contentType(MediaType.APPLICATION_JSON)
        )
        createResponse2.shouldBeSuccessful()
        val createPayload2: JsonNode = json.read(createResponse2)
        createPayload2["errors"].assertNoErrors()

        val createResponse3 = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", createMutation3).contentType(MediaType.APPLICATION_JSON)
        )
        createResponse3.shouldBeSuccessful()
        val createPayload3: JsonNode = json.read(createResponse3)
        createPayload3["errors"].assertNoErrors()

        val catalogItemId1 = createPayload1["data"]["saveCatalogItem"]["id"].stringValue
        val catalogItemId2 = createPayload2["data"]["saveCatalogItem"]["id"].stringValue
        val catalogItemId3 = createPayload3["data"]["saveCatalogItem"]["id"].stringValue

        val quantity1 = 2
        val quantity2 = 1
        val quantity3 = 1
        val totalPriceCents = (item1PriceCents * quantity1) + (item2PriceCents * quantity2) + (item3PriceCents * quantity3)
        val itemCount = quantity1 + quantity2 + quantity3

        val checkoutMutation = TestDataBuilders.checkoutMutation(
            items = listOf(
                TestDataBuilders.checkoutItemInput(
                    catalogItemId = catalogItemId1,
                    name = item1Name,
                    priceCents = item1PriceCents,
                    quantity = quantity1
                ),
                TestDataBuilders.checkoutItemInput(
                    catalogItemId = catalogItemId2,
                    name = item2Name,
                    priceCents = item2PriceCents,
                    quantity = quantity2
                ),
                TestDataBuilders.checkoutItemInput(
                    catalogItemId = catalogItemId3,
                    name = item3Name,
                    priceCents = item3PriceCents,
                    quantity = quantity3
                )
            ),
            totalPriceCents = totalPriceCents,
            itemCount = itemCount
        )

        val request = HttpRequest.POST("/graphql", checkoutMutation)
            .contentType(MediaType.APPLICATION_JSON)

        // Act
        val response = httpClient.exchangeAsString(request)
        response
            .shouldBeSuccessful()
            .shouldBeJson()
            .shouldHaveNonEmptyBody()

        // Assert
        val payload: JsonNode = json.read(response)
        payload["errors"].assertNoErrors()

        val data = payload["data"]
        Assertions.assertThat(data)
            .describedAs("GraphQL response must contain 'data.checkout'")
            .isNotNull()
        Assertions.assertThat(data["checkout"]).isNotNull

        val checkout: JsonNode = data["checkout"]
        Assertions.assertThat(checkout["id"]).isNotNull
        Assertions.assertThat(checkout["status"].stringValue).isEqualTo("COMPLETED")
        Assertions.assertThat((checkout["totalPriceCents"].numberValue).toLong()).isEqualTo(totalPriceCents)
        Assertions.assertThat((checkout["itemCount"].numberValue).toInt()).isEqualTo(itemCount)

        val items = checkout["items"]
        Assertions.assertThat(items.isArray).isTrue()
        Assertions.assertThat(items.size()).isEqualTo(3)
    }

    @Test
    fun `should checkout with item quantity greater than one via GraphQL mutation`() {
        // Arrange - create a catalog item
        val itemName = "USB-C Cable - 1m"
        val itemPriceCents = 1500L
        val createMutation = TestDataBuilders.saveCatalogItemMutation(
            name = itemName,
            category = "MATERIAL",
            priceCents = itemPriceCents
        )

        val createResponse = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", createMutation).contentType(MediaType.APPLICATION_JSON)
        )
        createResponse.shouldBeSuccessful()
        val createPayload: JsonNode = json.read(createResponse)
        createPayload["errors"].assertNoErrors()

        val createdItem = createPayload["data"]["saveCatalogItem"]
        val catalogItemId = createdItem["id"].stringValue
        val quantity = 5
        val totalPriceCents = itemPriceCents * quantity

        val checkoutMutation = TestDataBuilders.checkoutMutation(
            items = listOf(
                TestDataBuilders.checkoutItemInput(
                    catalogItemId = catalogItemId,
                    name = itemName,
                    priceCents = itemPriceCents,
                    quantity = quantity
                )
            ),
            totalPriceCents = totalPriceCents,
            itemCount = quantity
        )

        val request = HttpRequest.POST("/graphql", checkoutMutation)
            .contentType(MediaType.APPLICATION_JSON)

        // Act
        val response = httpClient.exchangeAsString(request)
        response
            .shouldBeSuccessful()
            .shouldBeJson()
            .shouldHaveNonEmptyBody()

        // Assert
        val payload: JsonNode = json.read(response)
        payload["errors"].assertNoErrors()

        val data = payload["data"]
        Assertions.assertThat(data)
            .describedAs("GraphQL response must contain 'data.checkout'")
            .isNotNull()

        val checkout: JsonNode = data["checkout"]
        Assertions.assertThat((checkout["itemCount"].numberValue).toInt()).isEqualTo(quantity)
        Assertions.assertThat((checkout["totalPriceCents"].numberValue).toLong()).isEqualTo(totalPriceCents)

        val items = checkout["items"]
        val item = items[0]
        Assertions.assertThat((item["quantity"].numberValue).toInt()).isEqualTo(quantity)
    }

    @Test
    fun `should handle checkout mutation with validation errors`() {
        // Arrange - invalid input (empty items list)
        val checkoutMutation = TestDataBuilders.checkoutMutation(
            items = emptyList(),
            totalPriceCents = 0L,
            itemCount = 0
        )

        val request = HttpRequest.POST("/graphql", checkoutMutation)
            .contentType(MediaType.APPLICATION_JSON)

        // Act
        val response = httpClient.exchangeAsString(request)
        response
            .shouldBeSuccessful()
            .shouldBeJson()

        // Assert - should have errors
        val payload: JsonNode = json.read(response)
        val errors = payload["errors"]
        Assertions.assertThat(errors)
            .describedAs("GraphQL errors must be present for validation errors")
            .isNotNull()
    }

    @Test
    fun `should handle checkout mutation with negative total price`() {
        // Arrange - create a catalog item
        val itemName = "Test Item"
        val itemPriceCents = 1500L
        val createMutation = TestDataBuilders.saveCatalogItemMutation(
            name = itemName,
            category = "MATERIAL",
            priceCents = itemPriceCents
        )

        val createResponse = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", createMutation).contentType(MediaType.APPLICATION_JSON)
        )
        createResponse.shouldBeSuccessful()
        val createPayload: JsonNode = json.read(createResponse)
        createPayload["errors"].assertNoErrors()

        val createdItem = createPayload["data"]["saveCatalogItem"]
        val catalogItemId = createdItem["id"].stringValue

        // Invalid input - negative total price
        val checkoutMutation = TestDataBuilders.checkoutMutation(
            items = listOf(
                TestDataBuilders.checkoutItemInput(
                    catalogItemId = catalogItemId,
                    name = itemName,
                    priceCents = itemPriceCents,
                    quantity = 1
                )
            ),
            totalPriceCents = -100L,
            itemCount = 1
        )

        val request = HttpRequest.POST("/graphql", checkoutMutation)
            .contentType(MediaType.APPLICATION_JSON)

        // Act
        val response = httpClient.exchangeAsString(request)
        response
            .shouldBeSuccessful()
            .shouldBeJson()

        // Assert - should have errors
        val payload: JsonNode = json.read(response)
        val errors = payload["errors"]
        Assertions.assertThat(errors)
            .describedAs("GraphQL errors must be present for negative total price")
            .isNotNull()
    }

    @Test
    fun `should handle checkout mutation with missing required fields`() {
        // Arrange - missing items field
        val invalidMutation = TestDataBuilders.graphQLQuery(
            """
            mutation Checkout(${'$'}input: CheckoutInput!) {
                checkout(input: ${'$'}input) {
                    id
                    status
                }
            }
            """.trimIndent(),
            mapOf(
                "input" to mapOf(
                    "totalPriceCents" to 1000L,
                    "itemCount" to 1
                    // Missing "items" field
                )
            )
        )

        val request = HttpRequest.POST("/graphql", invalidMutation)
            .contentType(MediaType.APPLICATION_JSON)

        // Act
        val response = httpClient.exchangeAsString(request)
        response
            .shouldBeSuccessful()
            .shouldBeJson()

        // Assert - should have errors
        val payload: JsonNode = json.read(response)
        val errors = payload["errors"]
        Assertions.assertThat(errors)
            .describedAs("GraphQL errors must be present for missing required fields")
            .isNotNull()
    }
}
