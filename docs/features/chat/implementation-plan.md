# Chat-Based Catalog Assistant Implementation Plan

## Overview

Implement a new Micronaut service module (`assistant`) that provides a chat-based interface for catalog operations. The service uses Google Gemini LLM with function calling to translate natural language into GraphQL operations against the federated GraphQL endpoint.

## Architecture

- **Service**: New `assistant` module (port 8082)
- **LLM Provider**: Google Gemini (with abstraction for future providers)
- **GraphQL Client**: HTTP-based client calling federated endpoint (port 4000)
- **API**: GraphQL endpoint for chat operations
- **Pattern**: Follows existing module structure (common, app, security)

## Implementation Phases

### Phase 1: Module Structure Setup

**1.1 Update settings.gradle.kts**

- Add `:assistant` to `include()` statement
- Location: `service/kotlin/settings.gradle.kts`

**1.2 Create Module Directory Structure**

- Create `service/kotlin/assistant/` with standard Micronaut structure
- Follow existing module patterns (app, security)

**1.3 Create build.gradle.kts**

- Dependencies: common module, Gemini SDK, HTTP client, GraphQL dependencies
- KSP processors for Micronaut
- Test dependencies (JUnit, Mockito, AssertJ)
- Location: `service/kotlin/assistant/build.gradle.kts`

**1.4 Create Application.kt**

- Standard Micronaut application entry point
- Location: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/Application.kt`

**1.5 Create application.yml**

- Port: 8082
- CORS configuration for frontend (localhost:3000)
- LLM configuration (Gemini API key, model, temperature)
- GraphQL endpoint configuration (federated endpoint)
- Location: `service/kotlin/assistant/src/main/resources/application.yml`

### Phase 2: LLM Abstraction Layer

**2.1 Create LLM Provider Interface**

- Abstract interface for LLM providers
- Method: `chatWithFunctions(request: LLMRequest): LLMResponse`
- Location: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/llm/LLMProvider.kt`

**2.2 Create LLM Request/Response Models**

- `LLMRequest`: messages, functions, temperature, maxTokens
- `ChatMessage`: role (USER, ASSISTANT, SYSTEM, FUNCTION), content
- `FunctionDefinition`: name, description, parameters
- `LLMResponse`: text, functionCalls, finishReason
- Use `@Serdeable` for JSON serialization
- Locations: `LLMRequest.kt`, `LLMResponse.kt`

**2.3 Create Gemini Implementation**

- `GeminiConfig`: Configuration properties for Gemini
- `GeminiProvider`: Implements `LLMProvider` interface
- Convert function definitions to Gemini format
- Handle function calling responses
- Location: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/llm/gemini/`

### Phase 3: GraphQL Client

**3.1 Create GraphQL Client**

- HTTP client for calling federated GraphQL endpoint
- Use Micronaut `@Client` annotation with configurable endpoint
- Suspend functions for coroutine support
- Error handling for GraphQL errors
- Location: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/graphql/GraphQLClient.kt`

**3.2 Create GraphQL Request/Response Models**

- `GraphQLRequest`: query, variables, operationName
- `GraphQLResponse`: data, errors
- Use `@Serdeable` for JSON serialization
- Location: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/graphql/GraphQLRequest.kt`

### Phase 4: Tool Definitions

**4.1 Create Tool Registry**

- Registry for managing all available tools
- Methods: `getAllFunctionDefinitions()`, `getToolByName()`
- Location: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/llm/tool/ToolRegistry.kt`

**4.2 Create Catalog Tool**

- Implements `Tool` interface
- Functions: `searchCatalogItems`, `catalogItem`, `saveCatalogItem`
- Each function returns `FunctionDefinition` for LLM
- `execute()` method calls GraphQL client with appropriate queries
- Location: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/llm/tool/CatalogTool.kt`

**4.3 Create Checkout Tool**

- Implements `Tool` interface
- Function: `checkout`
- Executes checkout mutation via GraphQL client
- Location: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/llm/tool/CheckoutTool.kt`

### Phase 5: Assistant Agent

**5.1 Create Conversation Context**

- Manages conversation state per session
- Stores message history
- Tracks last referenced items
- Location: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/agent/ConversationContext.kt`

**5.2 Create Assistant Agent**

- Orchestrates LLM calls and function execution
- Handles multi-turn conversations
- Processes function calls and results
- System prompt for procurement assistant role
- Location: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/agent/AssistantAgent.kt`

**5.3 Create Conversation Service**

- Manages conversation contexts (in-memory for now)
- Methods: `getOrCreateContext()`, `getContext()`, `clearContext()`
- Future: Use Redis for production
- Location: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/service/ConversationService.kt`

### Phase 6: GraphQL API

**6.1 Create GraphQL Schema**

- Query: `conversation(sessionId: String!): Conversation`
- Mutation: `chat(input: ChatInput!): ChatResponse!`, `clearConversation(sessionId: String!): Boolean!`
- Types: `ChatInput`, `ChatResponse`, `Conversation`
- Location: `service/kotlin/assistant/src/main/resources/graphql/schema.graphqls`

**6.2 Create GraphQL Resolver**

- Implements chat mutation, conversation query, clearConversation mutation
- Uses `AssistantAgent` and `ConversationService`
- Location: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/graphql/resolvers/AssistantResolver.kt`

**6.3 Create GraphQL Wiring Factory**

- Extends `GraphQLWiringFactory` from common module
- Registers query and mutation resolvers
- Location: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/graphql/AssistantWiringFactory.kt`

**6.4 Create GraphQL Factory**

- Creates GraphQL instance with schema and wiring
- Uses `BaseSchemaRegistryFactory` pattern (if available) or loads schema directly
- Location: `service/kotlin/assistant/src/main/kotlin/io/github/salomax/procureflow/assistant/graphql/AssistantGraphQLFactory.kt`

**6.5 Register GraphQL Controller**

- Use `GraphQLControllerBase` from common module
- Automatically handles `/graphql` endpoint

### Phase 7: Testing

**7.1 Unit Tests - Assistant Agent**

- Test message processing
- Test function call handling
- Test system prompt injection
- Use Mockito for mocking LLM provider and tools
- Location: `service/kotlin/assistant/src/test/kotlin/io/github/salomax/procureflow/assistant/agent/AssistantAgentTest.kt`

**7.2 Unit Tests - Tools**

- Test function definitions
- Test GraphQL query generation
- Test error handling
- Mock GraphQL client
- Location: `service/kotlin/assistant/src/test/kotlin/io/github/salomax/procureflow/assistant/llm/tool/`

**7.3 Unit Tests - GraphQL Resolver**

- Test chat mutation
- Test conversation query
- Test clearConversation mutation
- Mock assistant agent and conversation service
- Location: `service/kotlin/assistant/src/test/kotlin/io/github/salomax/procureflow/assistant/graphql/resolvers/AssistantResolverTest.kt`

**7.4 Integration Tests**

- Extend `BaseIntegrationTest` from common module
- Test GraphQL chat endpoint end-to-end
- Test conversation context persistence
- Test multiple messages in same session
- Location: `service/kotlin/assistant/src/test/kotlin/io/github/salomax/procureflow/assistant/test/graphql/GraphQLChatIntegrationTest.kt`

**7.5 Update build.gradle.kts for Testing**

- Add test dependencies (already in common module)
- Add `testIntegration` task following existing pattern
- Configure test tags

### Phase 8: Configuration & Documentation

**8.1 Environment Variables**

- Document required environment variables
- `GEMINI_API_KEY`: Google Gemini API key
- `GRAPHQL_ENDPOINT`: Federated GraphQL endpoint (default: http://localhost:4000/graphql)
- `LLM_PROVIDER`: LLM provider (default: gemini)

**8.2 Dockerfile**

- Create Dockerfile for assistant service
- Follow existing Dockerfile patterns
- Location: `service/kotlin/assistant/Dockerfile`

**8.3 Logging Configuration**

- Create logback.xml
- Follow existing logging patterns
- Location: `service/kotlin/assistant/src/main/resources/logback.xml`

## Key Implementation Details

### GraphQL Client Configuration

- Endpoint: Configurable via `graphql.endpoint` property
- Default: `http://localhost:4000/graphql` (Apollo Router)
- Timeout: Configurable (default: 30 seconds)

### LLM Function Definitions

- `searchCatalogItems(query: String!)`: Search catalog by query string
- `catalogItem(id: ID!)`: Get item details by ID
- `saveCatalogItem(input: CatalogItemInput!)`: Save/enroll new item
- `checkout(input: CheckoutInput!)`: Process checkout

### Conversation Management

- In-memory storage for MVP (ConcurrentHashMap)
- Session-based conversation tracking
- Future: Redis for production scalability

### Error Handling

- LLM API failures: Return error response
- GraphQL client errors: Propagate to LLM for user-friendly message
- Invalid function calls: Return error in function result
- Network timeouts: Handle gracefully

## Dependencies

### New Dependencies

- `com.google.ai.client.generativeai:generativeai:0.2.2` - Gemini SDK
- `io.micronaut:micronaut-http-client` - HTTP client (already in common)
- `io.micronaut.graphql:micronaut-graphql` - GraphQL support (verify if needed)
- `com.graphql-java:graphql-java` - GraphQL Java (already in common)

### Existing Dependencies (from common module)

- Micronaut core, HTTP, Serde
- GraphQL Java, Apollo Federation
- Kotlin coroutines
- Testing frameworks

## Testing Strategy

1. **Unit Tests**: Mock all external dependencies (LLM, GraphQL client)
2. **Integration Tests**: Test with real GraphQL endpoint (may need mock federated endpoint)
3. **Manual Testing**: Use curl/GraphQL playground to test chat mutations

## Future Enhancements (Out of Scope)

- Multiple LLM provider support (OpenAI, Anthropic)
- Conversation history persistence (Redis)
- Streaming responses
- Intent classification optimization
- Fuzzy matching for item names
- Context-aware item references

## Files to Create

### Source Files (15 files)

1. `Application.kt`
2. `llm/LLMProvider.kt`
3. `llm/LLMRequest.kt`
4. `llm/LLMResponse.kt`
5. `llm/gemini/GeminiConfig.kt`
6. `llm/gemini/GeminiProvider.kt`
7. `llm/tool/ToolRegistry.kt`
8. `llm/tool/CatalogTool.kt`
9. `llm/tool/CheckoutTool.kt`
10. `graphql/GraphQLClient.kt`
11. `graphql/GraphQLRequest.kt`
12. `agent/ConversationContext.kt`
13. `agent/AssistantAgent.kt`
14. `service/ConversationService.kt`
15. `graphql/resolvers/AssistantResolver.kt`
16. `graphql/AssistantWiringFactory.kt`
17. `graphql/AssistantGraphQLFactory.kt`

### Configuration Files (4 files)

1. `build.gradle.kts`
2. `application.yml`
3. `logback.xml`
4. `Dockerfile`

### Schema Files (1 file)

1. `graphql/schema.graphqls`

### Test Files (4+ files)

1. `agent/AssistantAgentTest.kt`
2. `llm/tool/CatalogToolTest.kt`
3. `llm/tool/CheckoutToolTest.kt`
4. `graphql/resolvers/AssistantResolverTest.kt`
5. `test/graphql/GraphQLChatIntegrationTest.kt`

## Notes

- Follow existing code patterns from `app` and `security` modules
- Use `GraphQLControllerBase` from common module for HTTP endpoint
- Use `GraphQLWiringFactory` pattern from common module
- Follow testing patterns from existing integration tests
- Ensure CORS is configured for frontend integration
- Consider rate limiting for production (future enhancement)