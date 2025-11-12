package io.github.salomax.procureflow.assistant.graphql.resolvers

import io.github.salomax.procureflow.assistant.agent.AssistantAgent
import io.github.salomax.procureflow.assistant.service.ConversationService
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking

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

