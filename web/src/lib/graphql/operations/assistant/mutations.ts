import { gql } from '@apollo/client';

// Chat mutation - send a message to the assistant
export const CHAT_MUTATION = gql`
  mutation Chat($input: ChatInput!) {
    chat(input: $input) {
      sessionId
      response
    }
  }
`;

// Clear conversation mutation - reset the conversation history
export const CLEAR_CONVERSATION_MUTATION = gql`
  mutation ClearConversation($sessionId: String!) {
    clearConversation(sessionId: $sessionId)
  }
`;

