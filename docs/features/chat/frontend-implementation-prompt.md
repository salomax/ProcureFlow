# Chat Assistant Frontend - Implementation Prompt

## Overview

Implement a frontend chat component for the Catalog Chat Assistant feature. The component should provide a Cursor-like AI chat interface that allows users to interact with the catalog through natural language conversations.

## Reference Documentation

- **Feature Specification**: `docs/features/chat/catalog-chat-assistant.feature`
- **Backend Implementation**: `docs/features/chat/implementation-prompt.md`
- **Testing Guide**: `docs/features/chat/curl-testing-guide.md`
- **Frontend Structure**: `spec/web/web-src-structure.md`
- **Component System**: `spec/web/web-components.md`
- **Theme System**: `spec/web/web-themes.md`
- **GraphQL Operations**: `spec/web/web-graphql-operations.md`

## Architecture Requirements

### Backend API

The assistant service exposes a GraphQL API at `http://localhost:8082/graphql` with the following operations:

**Chat Mutation**:
```graphql
mutation Chat($input: ChatInput!) {
  chat(input: $input) {
    sessionId
    response
  }
}
```

**Conversation Query**:
```graphql
query Conversation($sessionId: String!) {
  conversation(sessionId: $sessionId) {
    sessionId
    messageCount
    lastMessage
  }
}
```

**Clear Conversation Mutation**:
```graphql
mutation ClearConversation($sessionId: String!) {
  clearConversation(sessionId: $sessionId)
}
```

### GraphQL Types

```typescript
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

## Implementation Requirements

### 1. Create Shared Chat Component

**Location**: `web/src/shared/components/ui/feedback/Chat/`

Create a reusable chat component similar to Cursor's AI chat interface with the following structure:

```
Chat/
├── Chat.tsx              # Main chat component
├── ChatMessage.tsx       # Individual message component
├── ChatInput.tsx         # Message input area
├── Chat.stories.tsx      # Storybook stories
├── Chat.test.tsx         # Unit tests
└── index.ts              # Exports
```

#### Component Features

1. **Message Display Area**
   - Scrollable container that fills available space
   - Messages displayed in chronological order
   - User messages on the right, assistant messages on the left
   - Auto-scroll to bottom when new messages arrive
   - Keep the beginning of new messages visible at the top of the scroll area

2. **Input Area**
   - Textarea at the bottom of the component
   - Submit button next to the textarea
   - Keyboard shortcuts:
     - **Enter**: Submit message (if not Shift+Enter)
     - **Shift+Enter**: Insert new line (prevent submission)
   - Disable input and button while message is being sent
   - Clear input after successful submission
   - Show loading state during message sending

3. **Message Types**
   - **User Messages**: Right-aligned, distinct styling
   - **Assistant Messages**: Left-aligned, distinct styling
   - **Loading State**: Show typing indicator while waiting for response
   - **Error State**: Display error messages if request fails

4. **Session Management**
   - Generate unique session ID on component mount (use `useId()` or `uuid`)
   - Persist session ID in component state
   - Support clearing conversation (reset session)

#### Component Props

```typescript
interface ChatProps {
  sessionId?: string;              // Optional: use provided session or generate new
  onSessionChange?: (sessionId: string) => void;  // Callback when session changes
  placeholder?: string;            // Input placeholder text
  disabled?: boolean;              // Disable chat interaction
  onError?: (error: Error) => void; // Error callback
  className?: string;
  sx?: SxProps<Theme>;
}
```

#### Component Structure

```tsx
export function Chat({
  sessionId: providedSessionId,
  onSessionChange,
  placeholder = "Type your message...",
  disabled = false,
  onError,
  className,
  sx,
}: ChatProps) {
  // Session management
  const [sessionId, setSessionId] = useState<string>(() => 
    providedSessionId || generateSessionId()
  );
  
  // Messages state
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  
  // Input state
  const [inputValue, setInputValue] = useState("");
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  
  // GraphQL hooks
  const [sendMessage] = useChatMutation();
  
  // Auto-scroll to new messages
  useEffect(() => {
    if (messages.length > 0 && messagesContainerRef.current) {
      const container = messagesContainerRef.current;
      const lastMessage = container.lastElementChild;
      if (lastMessage) {
        // Scroll to show the beginning of the new message at the top
        lastMessage.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }
  }, [messages]);
  
  // Handle message submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputValue.trim() || isLoading || disabled) return;
    
    const userMessage = inputValue.trim();
    setInputValue("");
    setIsLoading(true);
    
    // Add user message immediately
    setMessages(prev => [...prev, {
      id: generateMessageId(),
      role: 'user',
      content: userMessage,
      timestamp: new Date(),
    }]);
    
    try {
      const result = await sendMessage({
        variables: {
          input: {
            sessionId,
            message: userMessage,
          },
        },
      });
      
      // Add assistant response
      if (result.data?.chat?.response) {
        setMessages(prev => [...prev, {
          id: generateMessageId(),
          role: 'assistant',
          content: result.data.chat.response,
          timestamp: new Date(),
        }]);
      }
    } catch (error) {
      onError?.(error as Error);
      setMessages(prev => [...prev, {
        id: generateMessageId(),
        role: 'assistant',
        content: 'Sorry, I encountered an error. Please try again.',
        timestamp: new Date(),
        error: true,
      }]);
    } finally {
      setIsLoading(false);
    }
  };
  
  // Handle keyboard shortcuts
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
    // Shift+Enter allows new line (default behavior)
  };
  
  return (
    <Box className={className} sx={{ ...sx, display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Messages container */}
      <Box
        ref={messagesContainerRef}
        sx={{
          flex: 1,
          overflowY: 'auto',
          p: 2,
          display: 'flex',
          flexDirection: 'column',
          gap: 2,
        }}
      >
        {messages.map((message) => (
          <ChatMessage key={message.id} message={message} />
        ))}
        {isLoading && <ChatMessage message={{ role: 'assistant', content: '', loading: true }} />}
        <div ref={messagesEndRef} />
      </Box>
      
      {/* Input area */}
      <Box
        component="form"
        onSubmit={handleSubmit}
        sx={{
          p: 2,
          borderTop: '1px solid',
          borderColor: 'divider',
          display: 'flex',
          gap: 1,
          alignItems: 'flex-end',
        }}
      >
        <TextField
          multiline
          maxRows={4}
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={disabled || isLoading}
          sx={{ flex: 1 }}
          inputProps={{
            'aria-label': 'Chat message input',
          }}
        />
        <IconButton
          type="submit"
          disabled={!inputValue.trim() || isLoading || disabled}
          aria-label="Send message"
        >
          <SendIcon />
        </IconButton>
      </Box>
    </Box>
  );
}
```

### 2. Create GraphQL Operations

**Location**: `web/src/lib/graphql/operations/assistant/`

#### Create Directory Structure

```
lib/graphql/operations/assistant/
├── mutations.ts          # Chat mutations
├── queries.ts            # Conversation queries
└── index.ts              # Exports
```

#### Mutations

```typescript
// lib/graphql/operations/assistant/mutations.ts
import { gql } from '@apollo/client';

export const CHAT_MUTATION = gql`
  mutation Chat($input: ChatInput!) {
    chat(input: $input) {
      sessionId
      response
    }
  }
`;

export const CLEAR_CONVERSATION_MUTATION = gql`
  mutation ClearConversation($sessionId: String!) {
    clearConversation(sessionId: $sessionId)
  }
`;
```

#### Queries

```typescript
// lib/graphql/operations/assistant/queries.ts
import { gql } from '@apollo/client';

export const CONVERSATION_QUERY = gql`
  query Conversation($sessionId: String!) {
    conversation(sessionId: $sessionId) {
      sessionId
      messageCount
      lastMessage
    }
  }
`;
```

#### Index

```typescript
// lib/graphql/operations/assistant/index.ts
export * from './mutations';
export * from './queries';
```

#### Update Main Operations Index

```typescript
// lib/graphql/operations/index.ts
export * from './assistant';
// ... other domain exports
```

### 3. Configure Apollo Client for Assistant Service

**Location**: `web/src/lib/graphql/client.ts`

The assistant service runs on a different port (8082) than the main GraphQL endpoint (4000). You need to configure Apollo Client to support multiple endpoints.

**Option A: Create Separate Client for Assistant**

```typescript
// lib/graphql/assistant-client.ts
import { ApolloClient, InMemoryCache, createHttpLink } from '@apollo/client';

const assistantHttpLink = createHttpLink({
  uri: process.env.NEXT_PUBLIC_ASSISTANT_GRAPHQL_URL || 'http://localhost:8082/graphql',
});

export const assistantClient = new ApolloClient({
  link: assistantHttpLink,
  cache: new InMemoryCache(),
});
```

**Option B: Use Apollo Link to Route Based on Operation**

```typescript
// lib/graphql/client.ts
import { ApolloClient, InMemoryCache, createHttpLink, from, split } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import { getMainDefinition } from '@apollo/client/utilities';

const mainHttpLink = createHttpLink({
  uri: process.env.NEXT_PUBLIC_GRAPHQL_URL || 'http://localhost:4000/graphql',
});

const assistantHttpLink = createHttpLink({
  uri: process.env.NEXT_PUBLIC_ASSISTANT_GRAPHQL_URL || 'http://localhost:8082/graphql',
});

// Split link to route operations to different endpoints
const splitLink = split(
  ({ query }) => {
    const definition = getMainDefinition(query);
    // Route assistant operations to assistant service
    if (definition.kind === 'OperationDefinition') {
      const operationName = definition.name?.value;
      return operationName === 'Chat' || 
             operationName === 'ClearConversation' || 
             operationName === 'Conversation';
    }
    return false;
  },
  assistantHttpLink,
  mainHttpLink
);

export const client = new ApolloClient({
  link: splitLink,
  cache: new InMemoryCache(),
});
```

**Recommended**: Use Option B for a unified client experience.

### 4. Wrap Chat Component in Drawer

**Location**: `web/src/shared/components/ui/feedback/Chat/ChatDrawer.tsx`

Create a drawer wrapper component that surfaces the chat component:

```tsx
import { Drawer } from '@/shared/components/ui/layout/Drawer';
import { Chat } from './Chat';

interface ChatDrawerProps {
  open: boolean;
  onClose: () => void;
  anchor?: 'left' | 'right' | 'top' | 'bottom';
  width?: number | string;
}

export function ChatDrawer({
  open,
  onClose,
  anchor = 'right',
  width = 480,
}: ChatDrawerProps) {
  return (
    <Drawer
      open={open}
      onClose={onClose}
      anchor={anchor}
      sx={{
        '& .MuiDrawer-paper': {
          width,
          maxWidth: '100vw',
        },
      }}
    >
      <Chat />
    </Drawer>
  );
}
```

### 5. Add Chat Button to Header

**Location**: `web/src/shared/ui/shell/AppHeader.tsx`

Add a chat button to the header that opens the chat drawer:

```tsx
// Add imports
import { ChatDrawer } from '@/shared/components/ui/feedback/Chat/ChatDrawer';
import SmartToyIcon from '@mui/icons-material/SmartToy'; // AI-related icon

// In AppHeader component
const [chatDrawerOpen, setChatDrawerOpen] = React.useState(false);

const handleChatIconClick = () => {
  setChatDrawerOpen(true);
};

const handleChatDrawerClose = () => {
  setChatDrawerOpen(false);
};

// In the actions section (right side of header)
<Tooltip title="AI Assistant">
  <IconButton 
    aria-label="Open AI assistant" 
    onClick={handleChatIconClick}
  >
    <SmartToyIcon />
  </IconButton>
</Tooltip>

// Add ChatDrawer at the end of the component
<ChatDrawer 
  open={chatDrawerOpen} 
  onClose={handleChatDrawerClose} 
/>
```

### 6. Update Component Exports

**Location**: `web/src/shared/components/ui/feedback/index.ts`

```typescript
export * from './Chat';
export * from './ToastProvider';
// ... other feedback components
```

**Location**: `web/src/shared/components/ui/index.ts`

Ensure Chat is exported from the main UI components index.

## Design Requirements

### Visual Design

1. **Message Styling**
   - User messages: Right-aligned, primary color background, white text
   - Assistant messages: Left-aligned, paper background, default text color
   - Message bubbles with rounded corners
   - Timestamp (optional, can be hidden by default)
   - Proper spacing between messages

2. **Input Area**
   - Textarea with border
   - Send button with icon
   - Disabled state styling
   - Loading state indicator

3. **Loading State**
   - Typing indicator (animated dots) for assistant responses
   - Disable input during loading

4. **Error State**
   - Error messages styled differently (e.g., red border or background)
   - Retry option for failed messages

### Theme Integration

- Use theme tokens for colors, spacing, and typography
- Support both light and dark modes
- Follow Material-UI design patterns
- Use `sx` prop for styling (not inline styles)

### Accessibility

- Proper ARIA labels for all interactive elements
- Keyboard navigation support
- Screen reader friendly
- Focus management
- Error announcements

## Implementation Steps

### Step 1: Setup GraphQL Operations

1. Create `lib/graphql/operations/assistant/` directory
2. Create `mutations.ts` and `queries.ts` files
3. Define GraphQL operations
4. Update operations index files
5. Run code generation: `pnpm codegen`

### Step 2: Configure Apollo Client

1. Update `lib/graphql/client.ts` to support multiple endpoints
2. Test that operations route to correct endpoints
3. Verify environment variables are set

### Step 3: Create Chat Components

1. Create `Chat/` directory in `shared/components/ui/feedback/`
2. Implement `Chat.tsx` with all features
3. Implement `ChatMessage.tsx` for individual messages
4. Implement `ChatInput.tsx` (or integrate into Chat.tsx)
5. Add proper TypeScript types
6. Add Storybook stories
7. Add unit tests

### Step 4: Create Drawer Wrapper

1. Create `ChatDrawer.tsx`
2. Integrate with existing Drawer component
3. Test drawer open/close functionality

### Step 5: Integrate with Header

1. Update `AppHeader.tsx`
2. Add chat button with AI icon
3. Add state management for drawer
4. Test button click and drawer opening

### Step 6: Testing

1. Test message sending and receiving
2. Test keyboard shortcuts (Enter, Shift+Enter)
3. Test auto-scrolling behavior
4. Test error handling
5. Test loading states
6. Test session management
7. Test drawer open/close
8. Test in both light and dark modes

## Code Generation

After creating GraphQL operations, run:

```bash
cd web
pnpm codegen
```

This will generate:
- TypeScript types for operations
- React hooks (`useChatMutation`, `useConversationQuery`, etc.)
- Type-safe operation documents

## Environment Variables

Add to `.env.local`:

```bash
NEXT_PUBLIC_ASSISTANT_GRAPHQL_URL=http://localhost:8082/graphql
```

## Testing Checklist

- [ ] Chat component renders correctly
- [ ] Messages display in correct order
- [ ] User messages appear on right, assistant on left
- [ ] Enter key submits message
- [ ] Shift+Enter creates new line
- [ ] Auto-scroll works correctly (shows beginning of new message at top)
- [ ] Loading state displays during message sending
- [ ] Error handling works correctly
- [ ] Drawer opens/closes correctly
- [ ] Header button opens drawer
- [ ] Session ID is generated and persisted
- [ ] GraphQL operations work correctly
- [ ] Theme switching works (light/dark mode)
- [ ] Accessibility features work (keyboard navigation, screen readers)
- [ ] Component is responsive

## Additional Features (Optional)

1. **Message Timestamps**: Show timestamps on hover or in a toggle
2. **Message Actions**: Copy, retry, delete actions for messages
3. **Conversation History**: Load previous conversations
4. **Markdown Support**: Render markdown in assistant messages
5. **Code Syntax Highlighting**: Highlight code blocks in messages
6. **File Attachments**: Support file uploads (future enhancement)
7. **Voice Input**: Voice-to-text input (future enhancement)
8. **Streaming Responses**: Show streaming text as it arrives (future enhancement)

## File Structure Summary

```
web/src/
├── lib/graphql/
│   ├── operations/
│   │   └── assistant/
│   │       ├── mutations.ts
│   │       ├── queries.ts
│   │       └── index.ts
│   └── client.ts (updated)
├── shared/components/ui/feedback/
│   └── Chat/
│       ├── Chat.tsx
│       ├── ChatMessage.tsx
│       ├── ChatDrawer.tsx
│       ├── Chat.stories.tsx
│       ├── Chat.test.tsx
│       └── index.ts
└── shared/ui/shell/
    └── AppHeader.tsx (updated)
```

## References

- [Material-UI Drawer](https://mui.com/material-ui/react-drawer/)
- [Material-UI TextField](https://mui.com/material-ui/react-text-field/)
- [Apollo Client React Hooks](https://www.apollographql.com/docs/react/api/react/hooks/)
- [Cursor AI Chat UI Reference](https://cursor.sh/) - Visual reference for chat interface design

## Notes

- The assistant service runs on port 8082, separate from the main GraphQL endpoint
- Session IDs should be unique per user session
- Messages are stored in component state (not persisted to backend beyond conversation context)
- The backend maintains conversation context per session ID
- Consider rate limiting for production use
- Consider adding analytics for chat usage

