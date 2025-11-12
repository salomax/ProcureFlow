package io.github.salomax.procureflow.app.test.catalog

import io.github.salomax.procureflow.app.test.TestDataBuilders
import io.github.salomax.procureflow.common.test.assertions.assertNoErrors
import io.github.salomax.procureflow.common.test.assertions.shouldHaveNonEmptyBody
import io.github.salomax.procureflow.common.test.assertions.shouldBeJson
import io.github.salomax.procureflow.common.test.assertions.shouldBeSuccessful
import io.github.salomax.procureflow.common.test.http.exchangeAsString
import io.github.salomax.procureflow.common.test.integration.BaseIntegrationTest
import io.github.salomax.procureflow.common.test.integration.PostgresIntegrationTest
import io.github.salomax.procureflow.common.test.json.read
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.json.tree.JsonNode
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

@MicronautTest(startApplication = true)
@DisplayName("GraphQL CatalogItem Integration Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
@Tag("graphql")
@Tag("catalog")
@TestMethodOrder(MethodOrderer.Random::class)
class GraphQLCatalogItemIntegrationTest : BaseIntegrationTest(), PostgresIntegrationTest {

    private fun uniqueName() = "GraphQL Catalog Item ${System.currentTimeMillis()}-${Thread.currentThread().id}"

    @Test
    fun `should search catalog items via GraphQL`() {
        // Arrange - create test items first
        val item1Name = uniqueName()
        val item2Name = uniqueName()
        
        val createMutation1 = TestDataBuilders.saveCatalogItemMutation(
            name = item1Name,
            category = "MATERIAL",
            priceCents = 1500L
        )
        val createMutation2 = TestDataBuilders.saveCatalogItemMutation(
            name = item2Name,
            category = "SERVICE",
            priceCents = 150000L
        )

        val createResponse1 = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", createMutation1).contentType(MediaType.APPLICATION_JSON)
        )
        createResponse1.shouldBeSuccessful()
        val createPayload1: JsonNode = json.read(createResponse1)
        createPayload1["errors"].assertNoErrors("First create mutation should succeed")
        
        val createResponse2 = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", createMutation2).contentType(MediaType.APPLICATION_JSON)
        )
        createResponse2.shouldBeSuccessful()
        val createPayload2: JsonNode = json.read(createResponse2)
        createPayload2["errors"].assertNoErrors("Second create mutation should succeed")

        // Act - search for items
        val searchQuery = TestDataBuilders.searchCatalogItemsQuery(item1Name.substring(0, 10))
        val request = HttpRequest.POST("/graphql", searchQuery)
            .contentType(MediaType.APPLICATION_JSON)

        val response = httpClient.exchangeAsString(request)
        response
            .shouldBeSuccessful()
            .shouldBeJson()
            .shouldHaveNonEmptyBody()

        // Assert
        val payload: JsonNode = json.read(response)
        payload["errors"].assertNoErrors()

        val data = payload["data"]
        assertThat(data)
            .describedAs("GraphQL response must contain 'data'")
            .isNotNull()

        val results = data["searchCatalogItems"]
        assertThat(results)
            .describedAs("Search results array must be present")
            .isNotNull()
        assertThat(results.isArray)
            .describedAs("Search results must be an array")
            .isTrue()
        assertThat(results.size())
            .describedAs("Should find at least one item")
            .isGreaterThan(0)
    }

    @Test
    fun `should save catalog item via GraphQL mutation`() {
        // Arrange
        val expectedName = uniqueName()
        val expectedCategory = "MATERIAL"
        val expectedPriceCents = 2500L
        val expectedStatus = "PENDING_APPROVAL"

        val mutation = TestDataBuilders.saveCatalogItemMutation(
            name = expectedName,
            category = expectedCategory,
            priceCents = expectedPriceCents,
            status = expectedStatus
        )

        val request = HttpRequest.POST("/graphql", mutation)
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
        assertThat(data)
            .describedAs("GraphQL response must contain 'data.saveCatalogItem'")
            .isNotNull()
        assertThat(data["saveCatalogItem"]).isNotNull

        val created: JsonNode = data["saveCatalogItem"]
        assertThat(created["id"]).isNotNull
        assertThat(created["name"].stringValue).isEqualTo(expectedName)
        assertThat(created["category"].stringValue).isEqualTo(expectedCategory)
        assertThat((created["priceCents"].numberValue).toLong()).isEqualTo(expectedPriceCents)
        assertThat(created["status"].stringValue).isEqualTo("PENDING_APPROVAL")
    }

    @Test
    fun `should enroll catalog item with description via GraphQL mutation`() {
        // Arrange
        val expectedName = uniqueName()
        val expectedCategory = "SERVICE"
        val expectedPriceCents = 50000L
        val expectedDescription = "This is a detailed description of the catalog item with multiple lines of text that can be quite long."

        val mutation = TestDataBuilders.saveCatalogItemMutation(
            name = expectedName,
            category = expectedCategory,
            priceCents = expectedPriceCents,
            description = expectedDescription
        )

        val request = HttpRequest.POST("/graphql", mutation)
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
        assertThat(data)
            .describedAs("GraphQL response must contain 'data.saveCatalogItem'")
            .isNotNull()
        assertThat(data["saveCatalogItem"]).isNotNull

        val created: JsonNode = data["saveCatalogItem"]
        assertThat(created["id"]).isNotNull
        assertThat(created["name"].stringValue).isEqualTo(expectedName)
        assertThat(created["category"].stringValue).isEqualTo(expectedCategory)
        assertThat((created["priceCents"].numberValue).toLong()).isEqualTo(expectedPriceCents)
        assertThat(created["description"].stringValue).isEqualTo(expectedDescription)
        assertThat(created["status"].stringValue).isEqualTo("PENDING_APPROVAL")
    }

    @Test
    fun `should enroll catalog item without description via GraphQL mutation`() {
        // Arrange
        val expectedName = uniqueName()
        val expectedCategory = "MATERIAL"
        val expectedPriceCents = 3000L

        val mutation = TestDataBuilders.saveCatalogItemMutation(
            name = expectedName,
            category = expectedCategory,
            priceCents = expectedPriceCents,
            description = null
        )

        val request = HttpRequest.POST("/graphql", mutation)
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
        assertThat(data)
            .describedAs("GraphQL response must contain 'data.saveCatalogItem'")
            .isNotNull()
        assertThat(data["saveCatalogItem"]).isNotNull

        val created: JsonNode = data["saveCatalogItem"]
        assertThat(created["id"]).isNotNull
        assertThat(created["name"].stringValue).isEqualTo(expectedName)
        // Description should be null or absent
        val descriptionNode = created["description"]
        assertThat(descriptionNode == null || descriptionNode.isNull).isTrue
    }

    @Test
    fun `should get catalog item by id via GraphQL`() {
        // Arrange - create an item first
        val itemName = uniqueName()
        val createMutation = TestDataBuilders.saveCatalogItemMutation(
            name = itemName,
            category = "SERVICE",
            priceCents = 150000L
        )

        val createResponse = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", createMutation).contentType(MediaType.APPLICATION_JSON)
        )
        val createPayload: JsonNode = json.read(createResponse)
        
        // Verify creation was successful
        createPayload["errors"].assertNoErrors("GraphQL errors must be absent when creating item")
        
        val createData = createPayload["data"]
        assertThat(createData)
            .describedAs("GraphQL response must contain 'data'")
            .isNotNull()
        
        val createdItem = createData["saveCatalogItem"]
        assertThat(createdItem)
            .describedAs("Created item must be present")
            .isNotNull()
        
        val itemId = createdItem["id"].stringValue
        assertThat(itemId)
            .describedAs("Item ID must be present")
            .isNotNull()

        // Act - query by ID
        val query = TestDataBuilders.getCatalogItemQuery(itemId)
        val request = HttpRequest.POST("/graphql", query)
            .contentType(MediaType.APPLICATION_JSON)

        val response = httpClient.exchangeAsString(request)
        response
            .shouldBeSuccessful()
            .shouldBeJson()

        // Assert
        val payload: JsonNode = json.read(response)
        payload["errors"].assertNoErrors()

        val data = payload["data"]
        assertThat(data)
            .describedAs("GraphQL response must contain 'data'")
            .isNotNull()

        val item = data["catalogItem"]
        assertThat(item)
            .describedAs("Catalog item should be found when using correct ID")
            .isNotNull()

        assertThat(item["id"].stringValue).isEqualTo(itemId)
        assertThat(item["name"].stringValue).isEqualTo(itemName)
    }

    @Test
    fun `should get catalog item with description by id via GraphQL`() {
        // Arrange - create an item with description first
        val itemName = uniqueName()
        val itemDescription = "A comprehensive description for testing GraphQL retrieval with description field."
        val createMutation = TestDataBuilders.saveCatalogItemMutation(
            name = itemName,
            category = "SERVICE",
            priceCents = 150000L,
            description = itemDescription
        )

        val createResponse = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", createMutation).contentType(MediaType.APPLICATION_JSON)
        )
        val createPayload: JsonNode = json.read(createResponse)
        
        // Verify creation was successful
        createPayload["errors"].assertNoErrors("GraphQL errors must be absent when creating item")
        
        val createData = createPayload["data"]
        assertThat(createData)
            .describedAs("GraphQL response must contain 'data'")
            .isNotNull()
        
        val createdItem = createData["saveCatalogItem"]
        assertThat(createdItem)
            .describedAs("Created item must be present")
            .isNotNull()
        
        val itemId = createdItem["id"].stringValue
        assertThat(itemId)
            .describedAs("Item ID must be present")
            .isNotNull()

        // Act - query by ID
        val query = TestDataBuilders.getCatalogItemQuery(itemId)
        val request = HttpRequest.POST("/graphql", query)
            .contentType(MediaType.APPLICATION_JSON)

        val response = httpClient.exchangeAsString(request)
        response
            .shouldBeSuccessful()
            .shouldBeJson()

        // Assert
        val payload: JsonNode = json.read(response)
        payload["errors"].assertNoErrors()

        val data = payload["data"]
        assertThat(data)
            .describedAs("GraphQL response must contain 'data'")
            .isNotNull()

        val item = data["catalogItem"]
        assertThat(item)
            .describedAs("Catalog item should be found when using correct ID")
            .isNotNull()

        assertThat(item["id"].stringValue).isEqualTo(itemId)
        assertThat(item["name"].stringValue).isEqualTo(itemName)
        assertThat(item["description"].stringValue).isEqualTo(itemDescription)
    }

    @Test
    fun `should return empty results when searching for non-existent item`() {
        // Act
        val searchQuery = TestDataBuilders.searchCatalogItemsQuery("NonExistentItemXYZ123")
        val request = HttpRequest.POST("/graphql", searchQuery)
            .contentType(MediaType.APPLICATION_JSON)

        val response = httpClient.exchangeAsString(request)
        response
            .shouldBeSuccessful()
            .shouldBeJson()

        // Assert
        val payload: JsonNode = json.read(response)
        payload["errors"].assertNoErrors()

        val data = payload["data"]
        assertThat(data)
            .describedAs("GraphQL response must contain 'data'")
            .isNotNull()
        
        val results = data["searchCatalogItems"]
        assertThat(results)
            .describedAs("Search results must be present")
            .isNotNull()
        assertThat(results.isArray).isTrue()
        assertThat(results.size()).isEqualTo(0)
    }

    @Test
    fun `should handle GraphQL mutation with validation errors`() {
        // Arrange - invalid input (empty name, negative price)
        val mutation = TestDataBuilders.saveCatalogItemMutation(
            name = "",
            category = "MATERIAL",
            priceCents = -100L
        )

        val request = HttpRequest.POST("/graphql", mutation)
            .contentType(MediaType.APPLICATION_JSON)

        // Act
        val response = httpClient.exchangeAsString(request)
        response
            .shouldBeSuccessful()
            .shouldBeJson()

        // Assert - should have errors
        val payload: JsonNode = json.read(response)
        val errors = payload["errors"]
        assertThat(errors)
            .describedAs("GraphQL errors must be present for validation errors")
            .isNotNull()
    }

    @Test
    fun `should return all catalog items when search query is empty`() {
        // Arrange - create multiple test items
        val item1Name = uniqueName()
        val item2Name = uniqueName()
        val item3Name = uniqueName()
        
        val createMutation1 = TestDataBuilders.saveCatalogItemMutation(
            name = item1Name,
            category = "MATERIAL",
            priceCents = 1500L
        )
        val createMutation2 = TestDataBuilders.saveCatalogItemMutation(
            name = item2Name,
            category = "SERVICE",
            priceCents = 2000L
        )
        val createMutation3 = TestDataBuilders.saveCatalogItemMutation(
            name = item3Name,
            category = "MATERIAL",
            priceCents = 3000L
        )

        // Create items
        val createResponse1 = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", createMutation1).contentType(MediaType.APPLICATION_JSON)
        )
        createResponse1.shouldBeSuccessful()
        val createPayload1: JsonNode = json.read(createResponse1)
        createPayload1["errors"].assertNoErrors("First create mutation should succeed")
        
        val createResponse2 = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", createMutation2).contentType(MediaType.APPLICATION_JSON)
        )
        createResponse2.shouldBeSuccessful()
        val createPayload2: JsonNode = json.read(createResponse2)
        createPayload2["errors"].assertNoErrors("Second create mutation should succeed")
        
        val createResponse3 = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", createMutation3).contentType(MediaType.APPLICATION_JSON)
        )
        createResponse3.shouldBeSuccessful()
        val createPayload3: JsonNode = json.read(createResponse3)
        createPayload3["errors"].assertNoErrors("Third create mutation should succeed")

        // Get created item IDs for verification
        val createdId1 = createPayload1["data"]["saveCatalogItem"]["id"].stringValue
        val createdId2 = createPayload2["data"]["saveCatalogItem"]["id"].stringValue
        val createdId3 = createPayload3["data"]["saveCatalogItem"]["id"].stringValue

        // Act - search with empty query
        val searchQuery = TestDataBuilders.searchCatalogItemsQuery("")
        val request = HttpRequest.POST("/graphql", searchQuery)
            .contentType(MediaType.APPLICATION_JSON)

        val response = httpClient.exchangeAsString(request)
        response
            .shouldBeSuccessful()
            .shouldBeJson()
            .shouldHaveNonEmptyBody()

        // Assert
        val payload: JsonNode = json.read(response)
        payload["errors"].assertNoErrors()

        val data = payload["data"]
        assertThat(data)
            .describedAs("GraphQL response must contain 'data'")
            .isNotNull()

        val results = data["searchCatalogItems"]
        assertThat(results)
            .describedAs("Search results array must be present")
            .isNotNull()
        assertThat(results.isArray)
            .describedAs("Search results must be an array")
            .isTrue()
        assertThat(results.size())
            .describedAs("Should return at least the 3 created items")
            .isGreaterThanOrEqualTo(3)

        // Verify all created items are in the results
        val resultIds = (0 until results.size()).map { results[it]["id"].stringValue }.toSet()
        assertThat(resultIds)
            .describedAs("Results should contain all created items")
            .contains(createdId1, createdId2, createdId3)
    }

    @Test
    fun `should return all catalog items when search query is whitespace only`() {
        // Arrange - create test items
        val item1Name = uniqueName()
        val item2Name = uniqueName()
        
        val createMutation1 = TestDataBuilders.saveCatalogItemMutation(
            name = item1Name,
            category = "MATERIAL",
            priceCents = 1500L
        )
        val createMutation2 = TestDataBuilders.saveCatalogItemMutation(
            name = item2Name,
            category = "SERVICE",
            priceCents = 2000L
        )

        // Create items
        val createResponse1 = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", createMutation1).contentType(MediaType.APPLICATION_JSON)
        )
        createResponse1.shouldBeSuccessful()
        val createPayload1: JsonNode = json.read(createResponse1)
        createPayload1["errors"].assertNoErrors("First create mutation should succeed")
        
        val createResponse2 = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", createMutation2).contentType(MediaType.APPLICATION_JSON)
        )
        createResponse2.shouldBeSuccessful()
        val createPayload2: JsonNode = json.read(createResponse2)
        createPayload2["errors"].assertNoErrors("Second create mutation should succeed")

        // Get created item IDs for verification
        val createdId1 = createPayload1["data"]["saveCatalogItem"]["id"].stringValue
        val createdId2 = createPayload2["data"]["saveCatalogItem"]["id"].stringValue

        // Act - search with whitespace-only query
        val searchQuery = TestDataBuilders.searchCatalogItemsQuery("   ")
        val request = HttpRequest.POST("/graphql", searchQuery)
            .contentType(MediaType.APPLICATION_JSON)

        val response = httpClient.exchangeAsString(request)
        response
            .shouldBeSuccessful()
            .shouldBeJson()
            .shouldHaveNonEmptyBody()

        // Assert
        val payload: JsonNode = json.read(response)
        payload["errors"].assertNoErrors()

        val data = payload["data"]
        assertThat(data)
            .describedAs("GraphQL response must contain 'data'")
            .isNotNull()

        val results = data["searchCatalogItems"]
        assertThat(results)
            .describedAs("Search results array must be present")
            .isNotNull()
        assertThat(results.isArray)
            .describedAs("Search results must be an array")
            .isTrue()
        assertThat(results.size())
            .describedAs("Should return at least the 2 created items")
            .isGreaterThanOrEqualTo(2)

        // Verify created items are in the results
        val resultIds = (0 until results.size()).map { results[it]["id"].stringValue }.toSet()
        assertThat(resultIds)
            .describedAs("Results should contain all created items")
            .contains(createdId1, createdId2)
    }
}

