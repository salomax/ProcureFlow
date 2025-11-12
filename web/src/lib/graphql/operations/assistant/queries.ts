import { gql } from '@apollo/client';

// Conversation query - get conversation details
export const CONVERSATION_QUERY = gql`
  query Conversation($sessionId: String!) {
    conversation(sessionId: $sessionId) {
      sessionId
      messageCount
      lastMessage
    }
  }
`;

