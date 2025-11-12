package io.github.salomax.procureflow.assistant.test.tool

import io.github.salomax.procureflow.assistant.graphql.GraphQLClient
import io.github.salomax.procureflow.assistant.graphql.GraphQLResponse
import io.github.salomax.procureflow.assistant.llm.tool.CatalogTool
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

@MicronautTest
class CatalogToolTest {
    
    private lateinit var graphQLClient: GraphQLClient
    private lateinit var catalogTool: CatalogTool
    
    @BeforeEach
    fun setUp() {
        graphQLClient = mock()
        catalogTool = CatalogTool(graphQLClient)
    }
    
    @Test
    fun `should return search function definition`() {
        // Act
        val function = catalogTool.getSearchFunction()
        
        // Assert
        assertThat(function.name).isEqualTo("searchCatalogItems")
        assertThat(function.description).contains("Search for catalog items")
        assertThat(function.parameters.required).contains("query")
    }
    
    @Test
    fun `should execute searchCatalogItems function`() {
        runBlocking {
            // Arrange
            val arguments = mapOf("query" to "USB-C")
            val mockData = mapOf(
                "searchCatalogItems" to listOf(
                    mapOf("id" to "1", "name" to "USB-C Cable - 1m")
                )
            )
            val graphQLResponse = GraphQLResponse(
                data = mockData,
                errors = null
            )
            
            whenever(graphQLClient.execute(any())).thenReturn(graphQLResponse)
            
            // Act
            val result = catalogTool.execute("searchCatalogItems", arguments)
            
            // Assert
            assertThat(result.success).isTrue()
            assertThat(result.data).isNotNull()
            verify(graphQLClient).execute(any())
        }
    }
    
    @Test
    fun `should return error when search query is missing`() {
        runBlocking {
            // Arrange
            val arguments = emptyMap<String, Any?>()
            
            // Act
            val result = catalogTool.execute("searchCatalogItems", arguments)
            
            // Assert
            assertThat(result.success).isFalse()
            assertThat(result.error).contains("Missing 'query' parameter")
        }
    }
    
    @Test
    fun `should execute catalogItem function`() {
        runBlocking {
            // Arrange
            val arguments = mapOf("id" to "item-123")
            val mockData = mapOf(
                "catalogItem" to mapOf(
                    "id" to "item-123",
                    "name" to "USB-C Cable",
                    "priceCents" to 1500
                )
            )
            val graphQLResponse = GraphQLResponse(
                data = mockData,
                errors = null
            )
            
            whenever(graphQLClient.execute(any())).thenReturn(graphQLResponse)
            
            // Act
            val result = catalogTool.execute("catalogItem", arguments)
            
            // Assert
            assertThat(result.success).isTrue()
            assertThat(result.data).isNotNull()
        }
    }
}

