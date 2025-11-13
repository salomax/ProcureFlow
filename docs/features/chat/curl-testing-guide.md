# Chat Assistant - cURL Testing Guide

This guide provides comprehensive examples for testing the Chat Assistant feature using `curl` commands.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Service Setup](#service-setup)
- [Basic Operations](#basic-operations)
- [Example Scenarios](#example-scenarios)
- [Troubleshooting](#troubleshooting)

## Prerequisites

1. **Assistant Service Running**: The assistant service must be running on port `8082`
   ```bash
   # Start the assistant service
   cd service/kotlin
   ./gradlew :assistant:run
   ```

2. **Environment Variables**: Ensure the following environment variables are set:
   ```bash
   export GEMINI_API_KEY=your-gemini-api-key-here
   export GRAPHQL_ENDPOINT=http://localhost:4000/graphql  # Federated GraphQL endpoint
   ```

3. **Dependencies**: The assistant service requires:
   - The federated GraphQL endpoint (typically running on port 4000)
   - A valid Gemini API key for LLM functionality

## Service Setup

### Base URL

The assistant service exposes a GraphQL endpoint at:
```
http://localhost:8082/graphql
```

### Request Format

All requests use `POST` method with `Content-Type: application/json` header.

### Common cURL Options

For better readability, you can use these options:
- `-X POST` - HTTP method
- `-H "Content-Type: application/json"` - Content type header
- `-d @-` - Read JSON from stdin (useful for multi-line JSON)
- `--silent --show-error` - Cleaner output
- `| jq` - Pretty-print JSON responses (requires `jq` tool)

## Basic Operations

### 1. Send a Chat Message

**Mutation**: `chat`

Send a message to the AI assistant and receive a natural language response.

```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation Chat($input: ChatInput!) { chat(input: $input) { sessionId response } }",
    "variables": {
      "input": {
        "sessionId": "test-session-1",
        "message": "Hello, can you help me search for USB-C cables?"
      }
    }
  }'
```

**Response Example**:
```json
{
  "data": {
    "chat": {
      "sessionId": "test-session-1",
      "response": "Hello! I'd be happy to help you search for USB-C cables. Let me search the catalog for you."
    }
  }
}
```

**Pretty-printed with jq**:
```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation Chat($input: ChatInput!) { chat(input: $input) { sessionId response } }",
    "variables": {
      "input": {
        "sessionId": "test-session-1",
        "message": "Hello, can you help me search for USB-C cables?"
      }
    }
  }' | jq
```

### 2. Get Conversation Context

**Query**: `conversation`

Retrieve the conversation history and metadata for a session.

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

**Response Example**:
```json
{
  "data": {
    "conversation": {
      "sessionId": "test-session-1",
      "messageCount": 3,
      "lastMessage": "Hello! I'd be happy to help you search for USB-C cables."
    }
  }
}
```

**Note**: Returns `null` if the session doesn't exist:
```json
{
  "data": {
    "conversation": null
  }
}
```

### 3. Clear Conversation History

**Mutation**: `clearConversation`

Clear all conversation history for a session.

```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation ClearConversation($sessionId: String!) { clearConversation(sessionId: $sessionId) }",
    "variables": {
      "sessionId": "test-session-1"
    }
  }'
```

**Response Example**:
```json
{
  "data": {
    "clearConversation": true
  }
}
```

## Example Scenarios

### Scenario 1: Search for Catalog Items

Search for items in the catalog using natural language.

```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation Chat($input: ChatInput!) { chat(input: $input) { sessionId response } }",
    "variables": {
      "input": {
        "sessionId": "search-session-1",
        "message": "Search for USB-C cables"
      }
    }
  }' | jq
```

**Expected Behavior**: The assistant will:
1. Recognize the search intent
2. Call the `searchCatalogItems` function
3. Return a natural language response with matching items

### Scenario 2: Get Item Details

Ask about specific item details (price, status, description).

```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation Chat($input: ChatInput!) { chat(input: $input) { sessionId response } }",
    "variables": {
      "input": {
        "sessionId": "details-session-1",
        "message": "Tell me about USB-C Cable - 1m"
      }
    }
  }' | jq
```

**Alternative queries**:
```bash
# Get price
"message": "What is the price of USB-C Cable - 1m?"

# Get status
"message": "What is the status of USB-C Cable - 1m?"

# Get description
"message": "Describe USB-C Cable - 1m"
```

### Scenario 3: Checkout Items

Process a checkout directly through chat.

```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation Chat($input: ChatInput!) { chat(input: $input) { sessionId response } }",
    "variables": {
      "input": {
        "sessionId": "checkout-session-1",
        "message": "I need to buy 10 USB-C cables"
      }
    }
  }' | jq
```

**Expected Behavior**: The assistant will:
1. Search for matching items
2. Extract the quantity (10)
3. Calculate the total price
4. Process the checkout via the `checkout` function
5. Return a confirmation message

### Scenario 4: Multi-turn Conversation

Maintain context across multiple messages in the same session.

```bash
# First message
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation Chat($input: ChatInput!) { chat(input: $input) { sessionId response } }",
    "variables": {
      "input": {
        "sessionId": "conversation-session-1",
        "message": "Search for USB-C cables"
      }
    }
  }' | jq

# Follow-up message (uses same sessionId)
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation Chat($input: ChatInput!) { chat(input: $input) { sessionId response } }",
    "variables": {
      "input": {
        "sessionId": "conversation-session-1",
        "message": "What is the price of the first one?"
      }
    }
  }' | jq

# Check conversation history
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query Conversation($sessionId: String!) { conversation(sessionId: $sessionId) { sessionId messageCount lastMessage } }",
    "variables": {
      "sessionId": "conversation-session-1"
    }
  }' | jq
```

### Scenario 5: Handle Item Not Found

Test error handling when items don't exist.

```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation Chat($input: ChatInput!) { chat(input: $input) { sessionId response } }",
    "variables": {
      "input": {
        "sessionId": "error-session-1",
        "message": "Search for Non-existent Item"
      }
    }
  }' | jq
```

**Expected Response**: The assistant should respond with a helpful message indicating the item wasn't found and suggest alternatives.

### Scenario 6: Ambiguous Item Names

Test handling of multiple matching items.

```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation Chat($input: ChatInput!) { chat(input: $input) { sessionId response } }",
    "variables": {
      "input": {
        "sessionId": "ambiguous-session-1",
        "message": "I need to buy USB-C cable"
      }
    }
  }' | jq
```

**Expected Behavior**: The assistant should:
1. Find multiple matching items
2. List all matches
3. Ask for clarification

## Advanced Usage

### Using Variables in Shell Scripts

Create reusable shell functions for easier testing:

```bash
#!/bin/bash

ASSISTANT_URL="http://localhost:8082/graphql"
SESSION_ID="test-session-$(date +%s)"

# Function to send a chat message
chat() {
  local message="$1"
  curl -X POST "$ASSISTANT_URL" \
    -H "Content-Type: application/json" \
    -d "{
      \"query\": \"mutation Chat(\$input: ChatInput!) { chat(input: \$input) { sessionId response } }\",
      \"variables\": {
        \"input\": {
          \"sessionId\": \"$SESSION_ID\",
          \"message\": \"$message\"
        }
      }
    }" | jq
}

# Function to get conversation
get_conversation() {
  curl -X POST "$ASSISTANT_URL" \
    -H "Content-Type: application/json" \
    -d "{
      \"query\": \"query Conversation(\$sessionId: String!) { conversation(sessionId: \$sessionId) { sessionId messageCount lastMessage } }\",
      \"variables\": {
        \"sessionId\": \"$SESSION_ID\"
      }
    }" | jq
}

# Usage
chat "Search for USB-C cables"
chat "What is the price?"
get_conversation
```

### Using JSON Files

For complex queries, store the request in a JSON file:

**request.json**:
```json
{
  "query": "mutation Chat($input: ChatInput!) { chat(input: $input) { sessionId response } }",
  "variables": {
    "input": {
      "sessionId": "test-session-1",
      "message": "I need to buy 10 USB-C cables"
    }
  }
}
```

**Execute**:
```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d @request.json | jq
```

### Testing with Authentication

If authentication is required, add the Authorization header:

```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token-here" \
  -d '{
    "query": "mutation Chat($input: ChatInput!) { chat(input: $input) { sessionId response } }",
    "variables": {
      "input": {
        "sessionId": "test-session-1",
        "message": "Hello"
      }
    }
  }' | jq
```

## Troubleshooting

### Service Not Responding

**Error**: `Connection refused` or `Connection timeout`

**Solutions**:
1. Verify the assistant service is running:
   ```bash
   # Check if port 8082 is listening
   lsof -i :8082
   # or
   netstat -an | grep 8082
   ```

2. Check service logs for errors:
   ```bash
   # If running via Gradle
   tail -f service/kotlin/assistant/build/logs/*.log
   ```

3. Verify environment variables are set:
   ```bash
   echo $GEMINI_API_KEY
   echo $GRAPHQL_ENDPOINT
   ```

### GraphQL Errors

**Error**: `"errors": [{"message": "..."}]`

**Common Causes**:
1. **Invalid sessionId**: Ensure sessionId is a non-empty string
2. **Missing required fields**: Check that all required input fields are provided
3. **Schema mismatch**: Verify the GraphQL schema matches the request

**Debug**: Add error details to see full error messages:
```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '...' | jq '.errors'
```

### LLM API Errors

**Error**: Assistant returns error messages or fails to process

**Solutions**:
1. Verify `GEMINI_API_KEY` is set and valid
2. Check API quota/rate limits
3. Review assistant service logs for LLM-related errors
4. Verify the federated GraphQL endpoint is accessible:
   ```bash
   curl -X POST http://localhost:4000/graphql \
     -H "Content-Type: application/json" \
     -d '{"query": "{ __schema { queryType { name } } }"}'
   ```

### Empty or Null Responses

**Issue**: `"response": null` or empty response

**Possible Causes**:
1. LLM provider timeout
2. GraphQL endpoint unreachable
3. Invalid function call results

**Debug**: Check conversation context to see what happened:
```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query Conversation($sessionId: String!) { conversation(sessionId: $sessionId) { sessionId messageCount lastMessage } }",
    "variables": {
      "sessionId": "your-session-id"
    }
  }' | jq
```

### Session Not Persisting

**Issue**: Conversation history is lost between requests

**Note**: Currently, conversations are stored in-memory. If the service restarts, all sessions are lost. This is expected behavior for the current implementation.

**Workaround**: Use a consistent `sessionId` across requests within the same service instance.

## Quick Reference

### Endpoint
```
POST http://localhost:8082/graphql
```

### Headers
```
Content-Type: application/json
Authorization: Bearer <token>  (optional)
```

### Chat Mutation
```graphql
mutation Chat($input: ChatInput!) {
  chat(input: $input) {
    sessionId
    response
  }
}
```

### Conversation Query
```graphql
query Conversation($sessionId: String!) {
  conversation(sessionId: $sessionId) {
    sessionId
    messageCount
    lastMessage
  }
}
```

### Clear Conversation Mutation
```graphql
mutation ClearConversation($sessionId: String!) {
  clearConversation(sessionId: $sessionId)
}
```

## Additional Resources

- [Feature Specification](./catalog-chat-assistant.feature) - Complete feature scenarios
- [Implementation Guide](./implementation-prompt.md) - Technical implementation details
- [GraphQL Schema](../../../service/kotlin/assistant/src/main/resources/graphql/schema.graphqls) - Full GraphQL schema definition



