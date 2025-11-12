# Chat Assistant Frontend Implementation

## Overview

Implement a Cursor-like AI chat interface for the Catalog Chat Assistant feature, including GraphQL operations, reusable chat components, drawer wrapper, and header integration.

## Implementation Steps

### 1. GraphQL Operations Setup

- Create `web/src/lib/graphql/operations/assistant/` directory
- Add `mutations.ts` with `CHAT_MUTATION` and `CLEAR_CONVERSATION_MUTATION`
- Add `queries.ts` with `CONVERSATION_QUERY`
- Add `index.ts` for exports
- Update `web/src/lib/graphql/operations/index.ts` to export assistant operations

### 2. Apollo Client Configuration

- Update `web/src/lib/graphql/client.ts` to support multiple endpoints
- Use Apollo Link split to route assistant operations to `http://localhost:8082/graphql`
- Route operations by name: `Chat`, `ClearConversation`, `Conversation`
- Keep existing auth link and main endpoint for other operations

### 3. Chat Component Implementation

- Create `web/src/shared/components/ui/feedback/Chat/` directory
- Implement `Chat.tsx`:
- Session management with `useId()` or UUID
- Message state management (user/assistant messages)
- Auto-scroll to show beginning of new messages at top
- Keyboard shortcuts (Enter to submit, Shift+Enter for newline)
- Loading and error states
- Integration with `useChatMutation` hook
- Implement `ChatMessage.tsx`:
- User messages (right-aligned, primary color)
- Assistant messages (left-aligned, paper background)
- Loading indicator (typing dots)
- Error state styling
- Create `index.ts` for exports
- Add TypeScript types for `ChatMessage` and `ChatProps`

### 4. ChatDrawer Component

- Create `web/src/shared/components/ui/feedback/Chat/ChatDrawer.tsx`
- Wrap Chat component in Drawer (similar to CartDrawer pattern)
- Use Drawer from `@/shared/components/ui/layout/Drawer`
- Configure as right-side drawer with width 480px
- No title needed (Chat component handles its own header if needed)

### 5. Header Integration

- Update `web/src/shared/ui/shell/AppHeader.tsx`
- Add `SmartToyIcon` from `@mui/icons-material`
- Add state for chat drawer open/close
- Add chat button in actions section (right side, before profile)
- Add ChatDrawer component at end of component
- Follow same pattern as CartDrawer integration

### 6. Component Exports

- Update `web/src/shared/components/ui/feedback/index.ts` to export Chat components
- Ensure proper barrel exports

### 7. Code Generation

- Run `pnpm codegen` to generate TypeScript types and React hooks
- Verify generated hooks: `useChatMutation`, `useClearConversationMutation`, `useConversationQuery`

### 8. Environment Configuration

- Document `NEXT_PUBLIC_ASSISTANT_GRAPHQL_URL` environment variable
- Default to `http://localhost:8082/graphql` if not set

## Key Files to Create/Modify

**New Files:**

- `web/src/lib/graphql/operations/assistant/mutations.ts`
- `web/src/lib/graphql/operations/assistant/queries.ts`
- `web/src/lib/graphql/operations/assistant/index.ts`
- `web/src/shared/components/ui/feedback/Chat/Chat.tsx`
- `web/src/shared/components/ui/feedback/Chat/ChatMessage.tsx`
- `web/src/shared/components/ui/feedback/Chat/ChatDrawer.tsx`
- `web/src/shared/components/ui/feedback/Chat/index.ts`

**Modified Files:**

- `web/src/lib/graphql/client.ts` - Add split link for assistant endpoint
- `web/src/lib/graphql/operations/index.ts` - Export assistant operations
- `web/src/shared/components/ui/feedback/index.ts` - Export Chat components
- `web/src/shared/ui/shell/AppHeader.tsx` - Add chat button and drawer

## Technical Details

### Apollo Client Split Link

Route assistant operations to port 8082, all others to port 4000. Use operation name matching for routing.

### Session Management

Generate unique session ID on component mount using React's `useId()` hook. Persist in component state.

### Auto-scroll Behavior

Scroll to show beginning of new messages at top of scroll area (not bottom), using `scrollIntoView({ block: 'start' })`.

### Message Types

- User: Right-aligned, primary background, white text
- Assistant: Left-aligned, paper background
- Loading: Typing indicator with animated dots
- Error: Distinct error styling with retry option

### Keyboard Shortcuts

- Enter: Submit message (prevent default, call submit handler)
- Shift+Enter: Allow default (newline in textarea)

## Testing Considerations

- Test message sending and receiving
- Test keyboard shortcuts
- Test auto-scroll behavior
- Test error handling
- Test drawer open/close
- Test session persistence
- Verify GraphQL operations route to correct endpoints