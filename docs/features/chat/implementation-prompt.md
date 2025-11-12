# Chat-Based Catalog Assistant - Implementation Prompt

## Overview

Implement a chat-based catalog assistant that uses an LLM with Function Calling (Tool Calling) to interact with the federated GraphQL endpoint. The assistant will allow users to search catalog items, get item details, and checkout items directly through natural language conversations.

## Feature Reference

See `docs/features/chat/catalog-chat-assistant.feature` for complete feature specifications.

## Architecture

### High-Level Design

```
User Message → Assistant Service (Port 8082)
                ↓
         LLM (Gemini) with Function Calling
                ↓
         Tool Definitions (4 tools)
                ↓
         GraphQL Client → Federated GraphQL Endpoint
                ↓
         Natural Language Response → User
```

### Technology Stack

- **Backend Framework**: Micronaut 4.9.3 (Kotlin)
- **LLM Provider**: Google Gemini (with abstraction layer for future providers)
- **GraphQL Client**: HTTP-based GraphQL client for federated endpoint
- **Port**: 8082 (separate from app:8081 and security:8080)

## Implementation Steps

### Phase 1: Create Assistant Module Structure

#### 1.1 Update settings.gradle.kts

**Location**: `service/kotlin/settings.gradle.kts`

Add the assistant module to the project:

```kotlin
rootProject.name = "procureflow-service"
include(":common", ":security", ":app", ":assistant")
```

#### 1.2 Create Assistant Module Directory Structure

**Location**: `service/kotlin/assistant/`

Create the following structure:
```
assistant/
├── build.gradle.kts
├── Dockerfile
└── src/
    ├── main/
    │   ├── kotlin/
    │   │   └── io/github/salomax/procureflow/assistant/
    │   │       ├── Application.kt
    │   │       ├── config/
    │   │       │   ├── AssistantConfig.kt
    │   │       │   └── GraphQLClientConfig.kt
    │   │       ├── llm/
    │   │       │   ├── LLMProvider.kt (interface)
    │   │       │   ├── LLMRequest.kt
    │   │       │   ├── LLMResponse.kt
    │   │       │   ├── FunctionDefinition.kt
    │   │       │   ├── FunctionCall.kt
    │   │       │   ├── gemini/
    │   │       │   │   ├── GeminiProvider.kt
    │   │       │   │   └── GeminiConfig.kt
    │   │       │   └── tool/
    │   │       │       ├── ToolRegistry.kt
    │   │       │       ├── CatalogTool.kt
    │   │       │       └── CheckoutTool.kt
    │   │       ├── graphql/
    │   │       │   ├── GraphQLClient.kt
    │   │       │   └── GraphQLRequest.kt
    │   │       ├── agent/
    │   │       │   ├── AssistantAgent.kt
    │   │       │   └── ConversationContext.kt
    │   │       └── api/
    │   │           └── ChatController.kt
    │   └── resources/
    │       ├── application.yml
    │       └── logback.xml
    └── test/
        └── kotlin/
            └── io/github/salomax/procureflow/assistant/
```

#### 1.3 Create build.gradle.kts

**Location**: `service/kotlin/assistant/build.gradle.kts`

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.micronaut.application")
    id("io.micronaut.aot")
    id("com.google.devtools.ksp")
    id("com.gradleup.shadow")
}

micronaut {
    version("4.9.3")
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("io.github.salomax.procureflow.assistant.*")
    }
}

repositories { 
    mavenCentral()
    google()
}

dependencies {
    // Project dependencies
    implementation(project(":common"))

    // KSP processors
    ksp("io.micronaut:micronaut-inject-kotlin")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    kspTest("io.micronaut:micronaut-inject-kotlin")

    // HTTP Client for GraphQL
    implementation("io.micronaut:micronaut-http-client")
    
    // GraphQL dependencies
    implementation("io.micronaut.graphql:micronaut-graphql")
    implementation("com.graphql-java:graphql-java")
    
    // JSON processing
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    
    // Gemini SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.2.2")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
}

application {
    mainClass.set("io.github.salomax.procureflow.assistant.Application")
}

tasks.test {
    systemProperty("ryuk.disabled", "true")
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
}
```

#### 1.4 Create Application.kt

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/Application.kt`

```kotlin
package io.github.salomax.procureflow.assistant

import io.micronaut.runtime.Micronaut

object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build(*args)
            .packages("io.github.salomax.procureflow.assistant")
            .start()
    }
}
```

#### 1.5 Create application.yml

**Location**: `service/kotlin/assistant/src/main/resources/application.yml`

```yaml
micronaut:
  application:
    name: procureflow-assistant
  server:
    port: 8082
    cors:
      enabled: true
      configurations:
        web:
          allowed-origins:
            - http://localhost:3000
            - http://127.0.0.1:3000
          allowed-methods:
            - GET
            - POST
            - OPTIONS
          allowed-headers:
            - "*"
          allow-credentials: true
          max-age: 3600

# LLM Configuration
llm:
  provider: ${LLM_PROVIDER:gemini}
  gemini:
    api-key: ${GEMINI_API_KEY:}
    model: ${GEMINI_MODEL:gemini-1.5-pro}
    temperature: ${GEMINI_TEMPERATURE:0.7}
    max-tokens: ${GEMINI_MAX_TOKENS:2048}

# GraphQL Client Configuration
graphql:
  endpoint: ${GRAPHQL_ENDPOINT:http://localhost:4000/graphql}
  timeout-seconds: ${GRAPHQL_TIMEOUT:30}
```

### Phase 2: LLM Abstraction Layer

#### 2.1 Create LLM Provider Interface

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/llm/LLMProvider.kt`

```kotlin
package io.github.salomax.procureflow.assistant.llm

import io.micronaut.core.annotation.NonNull

/**
 * Abstract interface for LLM providers.
 * Allows swapping between different LLM providers (Gemini, OpenAI, etc.)
 */
interface LLMProvider {
    
    /**
     * Send a chat message with function calling support
     * 
     * @param request The LLM request containing messages and function definitions
     * @return LLM response with text and optional function calls
     */
    suspend fun chatWithFunctions(request: LLMRequest): LLMResponse
    
    /**
     * Get the model name being used
     */
    fun getModelName(): String
}
```

#### 2.2 Create LLM Request/Response Models

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/llm/LLMRequest.kt`

```kotlin
package io.github.salomax.procureflow.assistant.llm

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class LLMRequest(
    val messages: List<ChatMessage>,
    val functions: List<FunctionDefinition>,
    val temperature: Double = 0.7,
    val maxTokens: Int = 2048
)

@Serdeable
data class ChatMessage(
    val role: MessageRole,
    val content: String
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    FUNCTION
}

@Serdeable
data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: FunctionParameters
)

@Serdeable
data class FunctionParameters(
    val type: String = "object",
    val properties: Map<String, PropertyDefinition>,
    val required: List<String> = emptyList()
)

@Serdeable
data class PropertyDefinition(
    val type: String,
    val description: String,
    val enum: List<String>? = null
)
```

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/llm/LLMResponse.kt`

```kotlin
package io.github.salomax.procureflow.assistant.llm

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class LLMResponse(
    val text: String?,
    val functionCalls: List<FunctionCall> = emptyList(),
    val finishReason: FinishReason
)

@Serdeable
data class FunctionCall(
    val name: String,
    val arguments: Map<String, Any?>
)

enum class FinishReason {
    STOP,
    FUNCTION_CALL,
    LENGTH,
    ERROR
}
```

#### 2.3 Create Gemini Implementation

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/llm/gemini/GeminiConfig.kt`

```kotlin
package io.github.salomax.procureflow.assistant.llm.gemini

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("llm.gemini")
data class GeminiConfig(
    val apiKey: String,
    val model: String = "gemini-1.5-pro",
    val temperature: Double = 0.7,
    val maxTokens: Int = 2048
)
```

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/llm/gemini/GeminiProvider.kt`

```kotlin
package io.github.salomax.procureflow.assistant.llm.gemini

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.*
import io.github.salomax.procureflow.assistant.llm.*
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class GeminiProvider(
    private val config: GeminiConfig
) : LLMProvider {
    
    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = config.model,
            apiKey = config.apiKey
        )
    }
    
    override suspend fun chatWithFunctions(request: LLMRequest): LLMResponse {
        return withContext(Dispatchers.IO) {
            try {
                // Convert function definitions to Gemini format
                val tools = request.functions.map { func ->
                    FunctionDeclaration(
                        name = func.name,
                        description = func.description,
                        parameters = convertParameters(func.parameters)
                    )
                }
                
                // Convert messages to Gemini format
                val chat = model.startChat(
                    tools = listOf(Tool(tools)),
                    generationConfig = generationConfig {
                        temperature = config.temperature
                        maxOutputTokens = config.maxTokens
                    }
                )
                
                // Get the last user message
                val lastMessage = request.messages.lastOrNull { it.role == MessageRole.USER }
                    ?: throw IllegalArgumentException("No user message found")
                
                val response = chat.sendMessage(lastMessage.content)
                
                // Parse response
                val text = response.text
                val functionCalls = response.functionCalls?.map { call ->
                    FunctionCall(
                        name = call.name,
                        arguments = call.args?.toMap() ?: emptyMap()
                    )
                } ?: emptyList()
                
                LLMResponse(
                    text = text,
                    functionCalls = functionCalls,
                    finishReason = when {
                        functionCalls.isNotEmpty() -> FinishReason.FUNCTION_CALL
                        text != null -> FinishReason.STOP
                        else -> FinishReason.ERROR
                    }
                )
            } catch (e: Exception) {
                LLMResponse(
                    text = null,
                    functionCalls = emptyList(),
                    finishReason = FinishReason.ERROR
                )
            }
        }
    }
    
    override fun getModelName(): String = config.model
    
    private fun convertParameters(params: FunctionParameters): Schema {
        // Convert FunctionParameters to Gemini Schema
        // Implementation details for schema conversion
        // This is a simplified version - actual implementation should handle all types
        return Schema(
            type = SchemaType.OBJECT,
            properties = params.properties.mapValues { (_, prop) ->
                Schema(
                    type = when (prop.type) {
                        "string" -> SchemaType.STRING
                        "integer" -> SchemaType.INTEGER
                        "number" -> SchemaType.NUMBER
                        "boolean" -> SchemaType.BOOLEAN
                        "array" -> SchemaType.ARRAY
                        else -> SchemaType.STRING
                    },
                    description = prop.description,
                    enumValues = prop.enum
                )
            },
            required = params.required
        )
    }
}
```

### Phase 3: GraphQL Client

#### 3.1 Create GraphQL Client

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/graphql/GraphQLClient.kt`

```kotlin
package io.github.salomax.procureflow.assistant.graphql

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class GraphQLClient(
    @Client("\${graphql.endpoint}") private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper
) {
    
    suspend fun execute(request: GraphQLRequest): GraphQLResponse {
        return withContext(Dispatchers.IO) {
            val httpRequest = HttpRequest.POST("/graphql", request)
                .contentType(MediaType.APPLICATION_JSON)
            
            val response = httpClient.toBlocking().exchange(httpRequest, Map::class.java)
            val body = response.body() as? Map<*, *> ?: emptyMap<Any, Any>()
            
            GraphQLResponse(
                data = body["data"] as? Map<*, *>,
                errors = (body["errors"] as? List<*>)?.mapNotNull { it as? Map<*, *> }
            )
        }
    }
}
```

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/graphql/GraphQLRequest.kt`

```kotlin
package io.github.salomax.procureflow.assistant.graphql

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GraphQLRequest(
    val query: String,
    val variables: Map<String, Any?> = emptyMap(),
    val operationName: String? = null
)

@Serdeable
data class GraphQLResponse(
    val data: Map<*, *>?,
    val errors: List<Map<*, *>>? = null
)
```

### Phase 4: Tool Definitions

#### 4.1 Create Tool Registry

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/llm/tool/ToolRegistry.kt`

```kotlin
package io.github.salomax.procureflow.assistant.llm.tool

import io.github.salomax.procureflow.assistant.llm.FunctionDefinition
import jakarta.inject.Singleton

@Singleton
class ToolRegistry(
    private val catalogTool: CatalogTool,
    private val checkoutTool: CheckoutTool
) {
    
    fun getAllFunctionDefinitions(): List<FunctionDefinition> {
        return listOf(
            catalogTool.getSearchFunction(),
            catalogTool.getItemFunction(),
            catalogTool.getSaveFunction(),
            checkoutTool.getCheckoutFunction()
        )
    }
    
    fun getToolByName(name: String): Tool? {
        return when (name) {
            "searchCatalogItems" -> catalogTool
            "catalogItem" -> catalogTool
            "saveCatalogItem" -> catalogTool
            "checkout" -> checkoutTool
            else -> null
        }
    }
}

interface Tool {
    fun execute(functionName: String, arguments: Map<String, Any?>): ToolResult
}

data class ToolResult(
    val success: Boolean,
    val data: Any? = null,
    val error: String? = null
)
```

#### 4.2 Create Catalog Tool

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/llm/tool/CatalogTool.kt`

```kotlin
package io.github.salomax.procureflow.assistant.llm.tool

import io.github.salomax.procureflow.assistant.graphql.GraphQLClient
import io.github.salomax.procureflow.assistant.graphql.GraphQLRequest
import io.github.salomax.procureflow.assistant.llm.FunctionDefinition
import io.github.salomax.procureflow.assistant.llm.FunctionParameters
import io.github.salomax.procureflow.assistant.llm.PropertyDefinition
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class CatalogTool(
    private val graphQLClient: GraphQLClient
) : Tool {
    
    fun getSearchFunction(): FunctionDefinition {
        return FunctionDefinition(
            name = "searchCatalogItems",
            description = "Search for catalog items by name or query string. Use this when the user wants to find items in the catalog.",
            parameters = FunctionParameters(
                properties = mapOf(
                    "query" to PropertyDefinition(
                        type = "string",
                        description = "The search query string to find catalog items"
                    )
                ),
                required = listOf("query")
            )
        )
    }
    
    fun getItemFunction(): FunctionDefinition {
        return FunctionDefinition(
            name = "catalogItem",
            description = "Get detailed information about a specific catalog item by its ID. Use this when the user asks about a specific item's details, price, status, or description.",
            parameters = FunctionParameters(
                properties = mapOf(
                    "id" to PropertyDefinition(
                        type = "string",
                        description = "The unique identifier (ID) of the catalog item"
                    )
                ),
                required = listOf("id")
            )
        )
    }
    
    fun getSaveFunction(): FunctionDefinition {
        return FunctionDefinition(
            name = "saveCatalogItem",
            description = "Save or enroll a new catalog item. Use this when the user wants to add a new item to the catalog.",
            parameters = FunctionParameters(
                properties = mapOf(
                    "name" to PropertyDefinition(
                        type = "string",
                        description = "The name of the catalog item"
                    ),
                    "category" to PropertyDefinition(
                        type = "string",
                        description = "The category of the item",
                        enum = listOf("MATERIAL", "SERVICE")
                    ),
                    "priceCents" to PropertyDefinition(
                        type = "integer",
                        description = "The price of the item in cents"
                    ),
                    "status" to PropertyDefinition(
                        type = "string",
                        description = "The status of the item",
                        enum = listOf("ACTIVE", "PENDING_APPROVAL", "INACTIVE")
                    ),
                    "description" to PropertyDefinition(
                        type = "string",
                        description = "Optional description of the item"
                    )
                ),
                required = listOf("name", "category", "priceCents", "status")
            )
        )
    }
    
    override fun execute(functionName: String, arguments: Map<String, Any?>): ToolResult {
        return runBlocking {
            try {
                when (functionName) {
                    "searchCatalogItems" -> {
                        val query = arguments["query"] as? String
                            ?: return@runBlocking ToolResult(false, error = "Missing 'query' parameter")
                        
                        val graphQLQuery = """
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
                        """.trimIndent()
                        
                        val request = GraphQLRequest(
                            query = graphQLQuery,
                            variables = mapOf("query" to query)
                        )
                        
                        val response = graphQLClient.execute(request)
                        ToolResult(true, data = response.data?.get("searchCatalogItems"))
                    }
                    
                    "catalogItem" -> {
                        val id = arguments["id"] as? String
                            ?: return@runBlocking ToolResult(false, error = "Missing 'id' parameter")
                        
                        val graphQLQuery = """
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
                        """.trimIndent()
                        
                        val request = GraphQLRequest(
                            query = graphQLQuery,
                            variables = mapOf("id" to id)
                        )
                        
                        val response = graphQLClient.execute(request)
                        ToolResult(true, data = response.data?.get("catalogItem"))
                    }
                    
                    "saveCatalogItem" -> {
                        val name = arguments["name"] as? String
                            ?: return@runBlocking ToolResult(false, error = "Missing 'name' parameter")
                        val category = arguments["category"] as? String
                            ?: return@runBlocking ToolResult(false, error = "Missing 'category' parameter")
                        val priceCents = (arguments["priceCents"] as? Number)?.toInt()
                            ?: return@runBlocking ToolResult(false, error = "Missing 'priceCents' parameter")
                        val status = arguments["status"] as? String
                            ?: return@runBlocking ToolResult(false, error = "Missing 'status' parameter")
                        val description = arguments["description"] as? String
                        
                        val graphQLMutation = """
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
                        """.trimIndent()
                        
                        val input = mapOf(
                            "name" to name,
                            "category" to category,
                            "priceCents" to priceCents,
                            "status" to status,
                            "description" to description
                        )
                        
                        val request = GraphQLRequest(
                            query = graphQLMutation,
                            variables = mapOf("input" to input)
                        )
                        
                        val response = graphQLClient.execute(request)
                        ToolResult(true, data = response.data?.get("saveCatalogItem"))
                    }
                    
                    else -> ToolResult(false, error = "Unknown function: $functionName")
                }
            } catch (e: Exception) {
                ToolResult(false, error = e.message ?: "Unknown error")
            }
        }
    }
}
```

#### 4.3 Create Checkout Tool

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/llm/tool/CheckoutTool.kt`

```kotlin
package io.github.salomax.procureflow.assistant.llm.tool

import io.github.salomax.procureflow.assistant.graphql.GraphQLClient
import io.github.salomax.procureflow.assistant.graphql.GraphQLRequest
import io.github.salomax.procureflow.assistant.llm.FunctionDefinition
import io.github.salomax.procureflow.assistant.llm.FunctionParameters
import io.github.salomax.procureflow.assistant.llm.PropertyDefinition
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class CheckoutTool(
    private val graphQLClient: GraphQLClient
) : Tool {
    
    fun getCheckoutFunction(): FunctionDefinition {
        return FunctionDefinition(
            name = "checkout",
            description = "Process a checkout for catalog items. Use this when the user wants to buy or checkout items. The items array should contain catalogItemId, name, priceCents, and quantity for each item.",
            parameters = FunctionParameters(
                properties = mapOf(
                    "items" to PropertyDefinition(
                        type = "array",
                        description = "Array of items to checkout. Each item must have catalogItemId, name, priceCents, and quantity."
                    ),
                    "totalPriceCents" to PropertyDefinition(
                        type = "integer",
                        description = "Total price of all items in cents"
                    ),
                    "itemCount" to PropertyDefinition(
                        type = "integer",
                        description = "Total number of items (sum of all quantities)"
                    )
                ),
                required = listOf("items", "totalPriceCents", "itemCount")
            )
        )
    }
    
    override fun execute(functionName: String, arguments: Map<String, Any?>): ToolResult {
        return runBlocking {
            try {
                if (functionName != "checkout") {
                    return@runBlocking ToolResult(false, error = "Unknown function: $functionName")
                }
                
                val items = arguments["items"] as? List<*>
                    ?: return@runBlocking ToolResult(false, error = "Missing 'items' parameter")
                val totalPriceCents = (arguments["totalPriceCents"] as? Number)?.toInt()
                    ?: return@runBlocking ToolResult(false, error = "Missing 'totalPriceCents' parameter")
                val itemCount = (arguments["itemCount"] as? Number)?.toInt()
                    ?: return@runBlocking ToolResult(false, error = "Missing 'itemCount' parameter")
                
                val graphQLMutation = """
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
                """.trimIndent()
                
                val input = mapOf(
                    "items" to items,
                    "totalPriceCents" to totalPriceCents,
                    "itemCount" to itemCount
                )
                
                val request = GraphQLRequest(
                    query = graphQLMutation,
                    variables = mapOf("input" to input)
                )
                
                val response = graphQLClient.execute(request)
                ToolResult(true, data = response.data?.get("checkout"))
            } catch (e: Exception) {
                ToolResult(false, error = e.message ?: "Unknown error")
            }
        }
    }
}
```

### Phase 5: Assistant Agent

#### 5.1 Create Conversation Context

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/agent/ConversationContext.kt`

```kotlin
package io.github.salomax.procureflow.assistant.agent

import io.github.salomax.procureflow.assistant.llm.ChatMessage
import io.github.salomax.procureflow.assistant.llm.MessageRole
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ConversationContext(
    val sessionId: String,
    val messages: MutableList<ChatMessage> = mutableListOf(),
    val lastReferencedItem: String? = null
) {
    fun addMessage(role: MessageRole, content: String) {
        messages.add(ChatMessage(role, content))
    }
    
    fun addSystemMessage(content: String) {
        messages.add(0, ChatMessage(MessageRole.SYSTEM, content))
    }
}
```

#### 5.2 Create Assistant Agent

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/agent/AssistantAgent.kt`

```kotlin
package io.github.salomax.procureflow.assistant.agent

import io.github.salomax.procureflow.assistant.llm.*
import io.github.salomax.procureflow.assistant.llm.tool.ToolRegistry
import io.github.salomax.procureflow.assistant.llm.tool.ToolResult
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class AssistantAgent(
    private val llmProvider: LLMProvider,
    private val toolRegistry: ToolRegistry
) {
    
    private val systemPrompt = """
        You are a helpful procurement assistant for a catalog system. 
        Your role is to help users:
        1. Search for catalog items
        2. Get details about specific items (price, status, description)
        3. Checkout items directly (no cart - direct checkout)
        
        When users want to buy items, you should:
        - First search for the item if the name is not exact
        - Extract the quantity from their message (default to 1 if not specified)
        - Calculate the total price
        - Use the checkout function to process the order
        
        Always respond in natural, friendly language. When you use functions, explain what you're doing.
    """.trimIndent()
    
    suspend fun processMessage(
        userMessage: String,
        context: ConversationContext
    ): String {
        // Add system prompt if not present
        if (context.messages.none { it.role == MessageRole.SYSTEM }) {
            context.addSystemMessage(systemPrompt)
        }
        
        // Add user message
        context.addMessage(MessageRole.USER, userMessage)
        
        // Get function definitions
        val functions = toolRegistry.getAllFunctionDefinitions()
        
        // Call LLM with function calling
        val request = LLMRequest(
            messages = context.messages.toList(),
            functions = functions
        )
        
        var response = llmProvider.chatWithFunctions(request)
        
        // Handle function calls
        while (response.finishReason == FinishReason.FUNCTION_CALL && response.functionCalls.isNotEmpty()) {
            // Add assistant message with function calls
            val functionCallText = response.functionCalls.joinToString("\n") { call ->
                "Calling function: ${call.name} with arguments: ${call.arguments}"
            }
            context.addMessage(MessageRole.ASSISTANT, functionCallText)
            
            // Execute function calls
            val functionResults = response.functionCalls.map { call ->
                val tool = toolRegistry.getToolByName(call.name)
                if (tool != null) {
                    val result = tool.execute(call.name, call.arguments)
                    // Add function result message
                    context.addMessage(
                        MessageRole.FUNCTION,
                        "Function ${call.name} result: ${if (result.success) "Success" else "Error: ${result.error}"}\nData: ${result.data}"
                    )
                    result
                } else {
                    ToolResult(false, error = "Unknown function: ${call.name}")
                }
            }
            
            // Call LLM again with function results
            response = llmProvider.chatWithFunctions(
                LLMRequest(
                    messages = context.messages.toList(),
                    functions = functions
                )
            )
        }
        
        // Add final assistant response
        val finalResponse = response.text ?: "I apologize, but I encountered an error processing your request."
        context.addMessage(MessageRole.ASSISTANT, finalResponse)
        
        return finalResponse
    }
}
```

### Phase 6: GraphQL Schema and Resolver

#### 6.1 Create GraphQL Schema

**Location**: `service/kotlin/assistant/src/main/resources/graphql/schema.graphqls`

```graphql
type Query {
  # Get conversation context for a session
  conversation(sessionId: String!): Conversation
}

type Mutation {
  # Send a chat message and get AI assistant response
  chat(input: ChatInput!): ChatResponse!
  
  # Clear conversation history for a session
  clearConversation(sessionId: String!): Boolean!
}

input ChatInput {
  sessionId: String!
  message: String!
}

type ChatResponse {
  sessionId: String!
  response: String!
}

type Conversation {
  sessionId: String!
  messageCount: Int!
  lastMessage: String
}
```

#### 6.2 Create GraphQL Resolver

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/graphql/resolvers/AssistantResolver.kt`

```kotlin
package io.github.salomax.procureflow.assistant.graphql.resolvers

import io.github.salomax.procureflow.assistant.agent.AssistantAgent
import io.github.salomax.procureflow.assistant.agent.ConversationContext
import io.github.salomax.procureflow.assistant.service.ConversationService
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

@Singleton
class AssistantResolver(
    private val assistantAgent: AssistantAgent,
    private val conversationService: ConversationService
) {
    
    fun chat(input: Map<String, Any?>): ChatResponse {
        val sessionId = extractField<String>(input, "sessionId")
        val message = extractField<String>(input, "message")
        
        return runBlocking {
            val context = conversationService.getOrCreateContext(sessionId)
            val response = assistantAgent.processMessage(message, context)
            
            ChatResponse(
                sessionId = sessionId,
                response = response
            )
        }
    }
    
    fun conversation(sessionId: String): Conversation? {
        val context = conversationService.getContext(sessionId) ?: return null
        
        return Conversation(
            sessionId = sessionId,
            messageCount = context.messages.size,
            lastMessage = context.messages.lastOrNull()?.content
        )
    }
    
    fun clearConversation(sessionId: String): Boolean {
        conversationService.clearContext(sessionId)
        return true
    }
    
    private fun <T> extractField(input: Map<String, Any?>, name: String): T {
        @Suppress("UNCHECKED_CAST")
        return input[name] as? T ?: throw IllegalArgumentException("Field '$name' is required")
    }
}

data class ChatResponse(
    val sessionId: String,
    val response: String
)

data class Conversation(
    val sessionId: String,
    val messageCount: Int,
    val lastMessage: String?
)
```

#### 6.3 Create Conversation Service

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/service/ConversationService.kt`

```kotlin
package io.github.salomax.procureflow.assistant.service

import io.github.salomax.procureflow.assistant.agent.ConversationContext
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
class ConversationService {
    
    // In-memory conversation storage (use Redis in production)
    private val conversations = ConcurrentHashMap<String, ConversationContext>()
    
    fun getOrCreateContext(sessionId: String): ConversationContext {
        return conversations.getOrPut(sessionId) {
            ConversationContext(sessionId = sessionId)
        }
    }
    
    fun getContext(sessionId: String): ConversationContext? {
        return conversations[sessionId]
    }
    
    fun clearContext(sessionId: String) {
        conversations.remove(sessionId)
    }
}
```

#### 6.4 Create GraphQL Wiring Factory

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/graphql/AssistantWiringFactory.kt`

```kotlin
package io.github.salomax.procureflow.assistant.graphql

import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring
import io.github.salomax.procureflow.assistant.graphql.resolvers.AssistantResolver
import io.github.salomax.procureflow.common.graphql.GraphQLWiringFactory
import jakarta.inject.Singleton

@Singleton
class AssistantWiringFactory(
    private val assistantResolver: AssistantResolver
) : GraphQLWiringFactory() {
    
    override fun registerQueryResolvers(type: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder {
        return type
            .dataFetcher("conversation") { env ->
                val sessionId = env.getArgument<String>("sessionId")
                assistantResolver.conversation(sessionId)
            }
    }
    
    override fun registerMutationResolvers(type: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder {
        return type
            .dataFetcher("chat") { env ->
                val input = env.getArgument<Map<String, Any?>>("input")
                assistantResolver.chat(input)
            }
            .dataFetcher("clearConversation") { env ->
                val sessionId = env.getArgument<String>("sessionId")
                assistantResolver.clearConversation(sessionId)
            }
    }
    
    override fun registerSubscriptionResolvers(type: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder {
        // No subscriptions for now
        return type
    }
}
```

#### 6.5 Create GraphQL Factory

**Location**: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/graphql/AssistantGraphQLFactory.kt`

```kotlin
package io.github.salomax.procureflow.assistant.graphql

import graphql.GraphQL
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

@Singleton
class AssistantGraphQLFactory(
    private val wiringFactory: AssistantWiringFactory
) {
    
    private val logger = LoggerFactory.getLogger(AssistantGraphQLFactory::class.java)
    
    @Singleton
    fun graphQL(): GraphQL {
        val registry = loadSchema()
        val runtimeWiring = wiringFactory.build()
        val schema = SchemaGenerator().makeExecutableSchema(registry, runtimeWiring)
        return GraphQL.newGraphQL(schema).build()
    }
    
    private fun loadSchema(): TypeDefinitionRegistry {
        val schemaResource = javaClass.classLoader.getResourceAsStream("graphql/schema.graphqls")
            ?: throw IllegalStateException("GraphQL schema file not found: graphql/schema.graphqls")
        
        return try {
            InputStreamReader(schemaResource).use { reader ->
                SchemaParser().parse(reader)
            }
        } catch (e: Exception) {
            logger.error("Failed to load GraphQL schema", e)
            throw IllegalStateException("Failed to load GraphQL schema", e)
        }
    }
}
```

**Note**: The GraphQL HTTP endpoint is provided by the `GraphQLController` from the `common` module, which is already included as a dependency. The controller automatically handles POST requests to `/graphql` and uses the `GraphQL` bean provided by this factory.

### Phase 9: Configuration and Setup

#### 9.1 Update supergraph.yaml (if needed)

**Location**: `contracts/graphql/supergraph/supergraph.yaml`

The assistant service will call the federated GraphQL endpoint, so no changes needed here unless you want to add the assistant as a subgraph (not required for this implementation).

#### 9.2 Environment Variables

Create a `.env` file or set environment variables:

```bash
# LLM Configuration
LLM_PROVIDER=gemini
GEMINI_API_KEY=your-gemini-api-key-here
GEMINI_MODEL=gemini-1.5-pro
GEMINI_TEMPERATURE=0.7
GEMINI_MAX_TOKENS=2048

# GraphQL Configuration
GRAPHQL_ENDPOINT=http://localhost:4000/graphql
GRAPHQL_TIMEOUT=30
```

### Phase 7: Unit Tests

#### 7.1 Update build.gradle.kts for Testing Dependencies

**Location**: `service/kotlin/assistant/build.gradle.kts`

Add test dependencies:

```kotlin
dependencies {
    // ... existing dependencies ...
    
    // Testing
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
    testImplementation("org.assertj:assertj-core")
    testImplementation("io.micronaut:micronaut-http-server-netty")
    testImplementation("io.micronaut:micronaut-http-client")
}
```

#### 7.2 Create Unit Tests for Assistant Agent

**Location**: `service/kotlin/assistant/src/test/kotlin/io/github/salomax/procureflow/assistant/agent/AssistantAgentTest.kt`

```kotlin
package io.github.salomax.procureflow.assistant.agent

import io.github.salomax.procureflow.assistant.llm.*
import io.github.salomax.procureflow.assistant.llm.tool.Tool
import io.github.salomax.procureflow.assistant.llm.tool.ToolRegistry
import io.github.salomax.procureflow.assistant.llm.tool.ToolResult
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

@MicronautTest
class AssistantAgentTest {
    
    private lateinit var llmProvider: LLMProvider
    private lateinit var toolRegistry: ToolRegistry
    private lateinit var assistantAgent: AssistantAgent
    
    @BeforeEach
    fun setUp() {
        llmProvider = mock()
        toolRegistry = mock()
        assistantAgent = AssistantAgent(llmProvider, toolRegistry)
    }
    
    @Test
    fun `should process message and return LLM response`() = runBlocking {
        // Arrange
        val userMessage = "Search for USB-C cables"
        val context = ConversationContext(sessionId = "test-session")
        
        val functions = listOf(
            FunctionDefinition(
                name = "searchCatalogItems",
                description = "Search catalog",
                parameters = FunctionParameters(
                    properties = emptyMap(),
                    required = emptyList()
                )
            )
        )
        
        val llmResponse = LLMResponse(
            text = "I found 2 USB-C cables in the catalog.",
            functionCalls = emptyList(),
            finishReason = FinishReason.STOP
        )
        
        whenever(toolRegistry.getAllFunctionDefinitions()).thenReturn(functions)
        whenever(llmProvider.chatWithFunctions(any())).thenReturn(llmResponse)
        
        // Act
        val result = assistantAgent.processMessage(userMessage, context)
        
        // Assert
        assertThat(result).isEqualTo("I found 2 USB-C cables in the catalog.")
        verify(llmProvider).chatWithFunctions(any())
        assertThat(context.messages).hasSize(3) // System, User, Assistant
    }
    
    @Test
    fun `should handle function calls and execute tools`() = runBlocking {
        // Arrange
        val userMessage = "I need to buy 10 USB-C cables"
        val context = ConversationContext(sessionId = "test-session")
        
        val functions = listOf(
            FunctionDefinition(
                name = "searchCatalogItems",
                description = "Search catalog",
                parameters = FunctionParameters(
                    properties = emptyMap(),
                    required = emptyList()
                )
            )
        )
        
        val tool = mock<Tool>()
        val toolResult = ToolResult(
            success = true,
            data = mapOf("items" to listOf(mapOf("id" to "1", "name" to "USB-C Cable")))
        )
        
        val firstLLMResponse = LLMResponse(
            text = null,
            functionCalls = listOf(
                FunctionCall(
                    name = "searchCatalogItems",
                    arguments = mapOf("query" to "USB-C")
                )
            ),
            finishReason = FinishReason.FUNCTION_CALL
        )
        
        val secondLLMResponse = LLMResponse(
            text = "I found USB-C Cable. Processing checkout for 10 units.",
            functionCalls = emptyList(),
            finishReason = FinishReason.STOP
        )
        
        whenever(toolRegistry.getAllFunctionDefinitions()).thenReturn(functions)
        whenever(toolRegistry.getToolByName("searchCatalogItems")).thenReturn(tool)
        whenever(tool.execute(eq("searchCatalogItems"), any())).thenReturn(toolResult)
        whenever(llmProvider.chatWithFunctions(any()))
            .thenReturn(firstLLMResponse)
            .thenReturn(secondLLMResponse)
        
        // Act
        val result = assistantAgent.processMessage(userMessage, context)
        
        // Assert
        assertThat(result).contains("Processing checkout")
        verify(tool).execute(eq("searchCatalogItems"), any())
        verify(llmProvider, times(2)).chatWithFunctions(any())
    }
    
    @Test
    fun `should add system prompt on first message`() = runBlocking {
        // Arrange
        val userMessage = "Hello"
        val context = ConversationContext(sessionId = "test-session")
        
        val llmResponse = LLMResponse(
            text = "Hello! How can I help you?",
            functionCalls = emptyList(),
            finishReason = FinishReason.STOP
        )
        
        whenever(toolRegistry.getAllFunctionDefinitions()).thenReturn(emptyList())
        whenever(llmProvider.chatWithFunctions(any())).thenReturn(llmResponse)
        
        // Act
        assistantAgent.processMessage(userMessage, context)
        
        // Assert
        assertThat(context.messages).hasSize(3)
        assertThat(context.messages[0].role).isEqualTo(MessageRole.SYSTEM)
        assertThat(context.messages[0].content).contains("procurement assistant")
    }
}
```

#### 7.3 Create Unit Tests for Tools

**Location**: `service/kotlin/assistant/src/test/kotlin/io/github/salomax/procureflow/assistant/llm/tool/CatalogToolTest.kt`

```kotlin
package io.github.salomax.procureflow.assistant.llm.tool

import io.github.salomax.procureflow.assistant.graphql.GraphQLClient
import io.github.salomax.procureflow.assistant.graphql.GraphQLResponse
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
    fun `should execute searchCatalogItems function`() = runBlocking {
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
    
    @Test
    fun `should return error when search query is missing`() = runBlocking {
        // Arrange
        val arguments = emptyMap<String, Any?>()
        
        // Act
        val result = catalogTool.execute("searchCatalogItems", arguments)
        
        // Assert
        assertThat(result.success).isFalse()
        assertThat(result.error).contains("Missing 'query' parameter")
    }
    
    @Test
    fun `should execute catalogItem function`() = runBlocking {
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
```

#### 7.4 Create Unit Tests for GraphQL Resolver

**Location**: `service/kotlin/assistant/src/test/kotlin/io/github/salomax/procureflow/assistant/graphql/resolvers/AssistantResolverTest.kt`

```kotlin
package io.github.salomax.procureflow.assistant.graphql.resolvers

import io.github.salomax.procureflow.assistant.agent.AssistantAgent
import io.github.salomax.procureflow.assistant.agent.ConversationContext
import io.github.salomax.procureflow.assistant.service.ConversationService
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

@MicronautTest
class AssistantResolverTest {
    
    private lateinit var assistantAgent: AssistantAgent
    private lateinit var conversationService: ConversationService
    private lateinit var resolver: AssistantResolver
    
    @BeforeEach
    fun setUp() {
        assistantAgent = mock()
        conversationService = mock()
        resolver = AssistantResolver(assistantAgent, conversationService)
    }
    
    @Test
    fun `should process chat message`() = runBlocking {
        // Arrange
        val sessionId = "test-session"
        val message = "I need to buy 10 USB-C cables"
        val input = mapOf(
            "sessionId" to sessionId,
            "message" to message
        )
        val context = ConversationContext(sessionId = sessionId)
        val expectedResponse = "I found USB-C Cable. Processing checkout for 10 units."
        
        whenever(conversationService.getOrCreateContext(sessionId)).thenReturn(context)
        whenever(assistantAgent.processMessage(message, context)).thenReturn(expectedResponse)
        
        // Act
        val result = resolver.chat(input)
        
        // Assert
        assertThat(result.sessionId).isEqualTo(sessionId)
        assertThat(result.response).isEqualTo(expectedResponse)
        verify(assistantAgent).processMessage(message, context)
    }
    
    @Test
    fun `should get conversation context`() {
        // Arrange
        val sessionId = "test-session"
        val context = ConversationContext(sessionId = sessionId)
        context.addMessage(io.github.salomax.procureflow.assistant.llm.MessageRole.USER, "Hello")
        context.addMessage(io.github.salomax.procureflow.assistant.llm.MessageRole.ASSISTANT, "Hi!")
        
        whenever(conversationService.getContext(sessionId)).thenReturn(context)
        
        // Act
        val result = resolver.conversation(sessionId)
        
        // Assert
        assertThat(result).isNotNull()
        assertThat(result!!.sessionId).isEqualTo(sessionId)
        assertThat(result.messageCount).isEqualTo(2)
        assertThat(result.lastMessage).isEqualTo("Hi!")
    }
    
    @Test
    fun `should return null for non-existent conversation`() {
        // Arrange
        val sessionId = "non-existent"
        whenever(conversationService.getContext(sessionId)).thenReturn(null)
        
        // Act
        val result = resolver.conversation(sessionId)
        
        // Assert
        assertThat(result).isNull()
    }
    
    @Test
    fun `should clear conversation`() {
        // Arrange
        val sessionId = "test-session"
        doNothing().whenever(conversationService).clearContext(sessionId)
        
        // Act
        val result = resolver.clearConversation(sessionId)
        
        // Assert
        assertThat(result).isTrue()
        verify(conversationService).clearContext(sessionId)
    }
}
```

### Phase 8: Integration Tests

#### 8.1 Create Integration Test Base

**Location**: `service/kotlin/assistant/src/test/kotlin/io/github/salomax/procureflow/assistant/test/AssistantIntegrationTest.kt`

```kotlin
package io.github.salomax.procureflow.assistant.test

import io.github.salomax.procureflow.common.test.assertions.assertNoErrors
import io.github.salomax.procureflow.common.test.assertions.shouldBeJson
import io.github.salomax.procureflow.common.test.assertions.shouldBeSuccessful
import io.github.salomax.procureflow.common.test.http.exchangeAsString
import io.github.salomax.procureflow.common.test.integration.BaseIntegrationTest
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.json.tree.JsonNode
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

@MicronautTest(startApplication = true)
@DisplayName("Assistant Integration Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
@Tag("assistant")
@TestMethodOrder(MethodOrderer.Random::class)
abstract class AssistantIntegrationTest : BaseIntegrationTest() {
    
    protected fun chatMutation(sessionId: String, message: String): String {
        return """
        {
          "query": "mutation Chat(${'$'}input: ChatInput!) { chat(input: ${'$'}input) { sessionId response } }",
          "variables": {
            "input": {
              "sessionId": "$sessionId",
              "message": "$message"
            }
          }
        }
        """.trimIndent()
    }
    
    protected fun conversationQuery(sessionId: String): String {
        return """
        {
          "query": "query Conversation(${'$'}sessionId: String!) { conversation(sessionId: ${'$'}sessionId) { sessionId messageCount lastMessage } }",
          "variables": {
            "sessionId": "$sessionId"
          }
        }
        """.trimIndent()
    }
    
    protected fun clearConversationMutation(sessionId: String): String {
        return """
        {
          "query": "mutation ClearConversation(${'$'}sessionId: String!) { clearConversation(sessionId: ${'$'}sessionId) }",
          "variables": {
            "sessionId": "$sessionId"
          }
        }
        """.trimIndent()
    }
}
```

#### 8.2 Create GraphQL Chat Integration Test

**Location**: `service/kotlin/assistant/src/test/kotlin/io/github/salomax/procureflow/assistant/test/graphql/GraphQLChatIntegrationTest.kt`

```kotlin
package io.github.salomax.procureflow.assistant.test.graphql

import io.github.salomax.procureflow.assistant.test.AssistantIntegrationTest
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.json.tree.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GraphQLChatIntegrationTest : AssistantIntegrationTest() {
    
    private fun uniqueSessionId() = "test-session-${System.currentTimeMillis()}"
    
    @Test
    fun `should process chat message via GraphQL mutation`() {
        // Arrange
        val sessionId = uniqueSessionId()
        val message = "Search for USB-C cables"
        val mutation = chatMutation(sessionId, message)
        
        val request = HttpRequest.POST("/graphql", mutation)
            .contentType(MediaType.APPLICATION_JSON)
        
        // Act
        val response = httpClient.exchangeAsString(request)
        response
            .shouldBeSuccessful()
            .shouldBeJson()
        
        // Assert
        val payload: JsonNode = json.read(response)
        payload["errors"].assertNoErrors()
        
        val data = payload["data"]
        assertThat(data).isNotNull()
        
        val chatResponse = data["chat"]
        assertThat(chatResponse).isNotNull()
        assertThat(chatResponse["sessionId"].stringValue).isEqualTo(sessionId)
        assertThat(chatResponse["response"].stringValue).isNotEmpty()
    }
    
    @Test
    fun `should get conversation context via GraphQL query`() {
        // Arrange - first send a message
        val sessionId = uniqueSessionId()
        val message = "Hello"
        val chatMutation = chatMutation(sessionId, message)
        
        val chatRequest = HttpRequest.POST("/graphql", chatMutation)
            .contentType(MediaType.APPLICATION_JSON)
        
        val chatResponse = httpClient.exchangeAsString(chatRequest)
        chatResponse.shouldBeSuccessful()
        
        // Act - query conversation
        val query = conversationQuery(sessionId)
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
        assertThat(data).isNotNull()
        
        val conversation = data["conversation"]
        assertThat(conversation).isNotNull()
        assertThat(conversation["sessionId"].stringValue).isEqualTo(sessionId)
        assertThat(conversation["messageCount"].intValue).isGreaterThan(0)
    }
    
    @Test
    fun `should clear conversation via GraphQL mutation`() {
        // Arrange - first send a message
        val sessionId = uniqueSessionId()
        val message = "Hello"
        val chatMutation = chatMutation(sessionId, message)
        
        val chatRequest = HttpRequest.POST("/graphql", chatMutation)
            .contentType(MediaType.APPLICATION_JSON)
        
        httpClient.exchangeAsString(chatRequest).shouldBeSuccessful()
        
        // Act - clear conversation
        val mutation = clearConversationMutation(sessionId)
        val request = HttpRequest.POST("/graphql", mutation)
            .contentType(MediaType.APPLICATION_JSON)
        
        val response = httpClient.exchangeAsString(request)
        response
            .shouldBeSuccessful()
            .shouldBeJson()
        
        // Assert
        val payload: JsonNode = json.read(response)
        payload["errors"].assertNoErrors()
        
        val data = payload["data"]
        assertThat(data).isNotNull()
        assertThat(data["clearConversation"].booleanValue).isTrue()
        
        // Verify conversation is cleared
        val query = conversationQuery(sessionId)
        val queryRequest = HttpRequest.POST("/graphql", query)
            .contentType(MediaType.APPLICATION_JSON)
        
        val queryResponse = httpClient.exchangeAsString(queryRequest)
        val queryPayload: JsonNode = json.read(queryResponse)
        val conversation = queryPayload["data"]["conversation"]
        assertThat(conversation.isNull).isTrue()
    }
    
    @Test
    fun `should handle multiple messages in same session`() {
        // Arrange
        val sessionId = uniqueSessionId()
        
        // Act - send multiple messages
        val message1 = "Search for USB-C cables"
        val mutation1 = chatMutation(sessionId, message1)
        val response1 = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", mutation1).contentType(MediaType.APPLICATION_JSON)
        )
        response1.shouldBeSuccessful()
        
        val message2 = "What is the price?"
        val mutation2 = chatMutation(sessionId, message2)
        val response2 = httpClient.exchangeAsString(
            HttpRequest.POST("/graphql", mutation2).contentType(MediaType.APPLICATION_JSON)
        )
        response2.shouldBeSuccessful()
        
        // Assert - check conversation has multiple messages
        val query = conversationQuery(sessionId)
        val queryRequest = HttpRequest.POST("/graphql", query)
            .contentType(MediaType.APPLICATION_JSON)
        
        val queryResponse = httpClient.exchangeAsString(queryRequest)
        val payload: JsonNode = json.read(queryResponse)
        payload["errors"].assertNoErrors()
        
        val conversation = payload["data"]["conversation"]
        assertThat(conversation["messageCount"].intValue).isGreaterThanOrEqualTo(4) // System + 2 user + 2 assistant
    }
}
```

## Testing

### Running Tests

1. **Unit Tests**:
   ```bash
   cd service/kotlin
   ./gradlew :assistant:test
   ```

2. **Integration Tests**:
   ```bash
   cd service/kotlin
   ./gradlew :assistant:testIntegration
   ```

### Manual Testing

1. Start the assistant service:
   ```bash
   cd service/kotlin
   ./gradlew :assistant:run
   ```

2. Test the chat mutation via GraphQL:
   ```bash
   curl -X POST http://localhost:8082/graphql \
     -H "Content-Type: application/json" \
     -d '{
       "query": "mutation Chat($input: ChatInput!) { chat(input: $input) { sessionId response } }",
       "variables": {
         "input": {
           "sessionId": "test-session-1",
           "message": "I need to buy 10 USB-C cables"
         }
       }
     }'
   ```

3. Test conversation query:
   ```bash
   curl -X POST http://localhost:8082/graphql \
     -H "Content-Type: application/json" \
     -d '{
       "query": "query Conversation($sessionId: String!) { conversation(sessionId: $sessionId) { sessionId messageCount lastMessage } }",
       "variables": {
         "sessionId": "test-session-1"
       }
     }'
   ```

## Notes

1. **Gemini SDK**: The Gemini SDK version and API may change. Check the latest Google AI SDK documentation for updates.

2. **Error Handling**: Add comprehensive error handling for:
   - LLM API failures
   - GraphQL client errors
   - Invalid function calls
   - Network timeouts

3. **Conversation Storage**: Currently using in-memory storage. For production, use Redis or a database.

4. **Authentication**: Add authentication/authorization if needed for the chat endpoint.

5. **Rate Limiting**: Consider adding rate limiting for the chat endpoint.

6. **Logging**: Add structured logging for debugging and monitoring.

7. **Metrics**: Add Micrometer metrics for monitoring LLM calls, function executions, and response times.

## Future Enhancements

1. Support for multiple LLM providers (OpenAI, Anthropic, etc.)
2. Conversation history persistence
3. Streaming responses for better UX
4. Multi-turn conversation optimization
5. Intent classification before function calling
6. Fuzzy matching for item names
7. Context-aware item references ("those", "it", etc.)

