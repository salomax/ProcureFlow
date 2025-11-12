# Implementation Prompt for Cart and Checkout Feature

## Context

Implement a full-stack solution for the **Cart and Checkout** feature as specified in `docs/features/cart/cart-and-checkout.feature`. This feature allows procurement users to select catalog items, add them to a cart, manage cart contents, and complete checkout (simulation only - no payment processing).

## Feature Requirements

The feature must support:
1. **Cart icon in header** - Cart icon must be displayed in AppHeader's right-side actions area
2. **Right-side drawer** - Cart and checkout must be implemented as a right-side drawer (not a full page)
3. **Add items to cart** - Single or multiple catalog items with specified quantities
4. **Set quantity when adding** - Users can specify quantity before adding items to cart
5. **View cart contents** - Display items with name, price, and quantity in the drawer
6. **Manage cart** - Remove items, update quantities, clear entire cart
7. **Set quantity during checkout** - Users can modify item quantities in the checkout view (within drawer)
8. **Cart persistence** - Cart should persist across page navigation
9. **Cart icon with badge** - Display item count badge on cart icon in header
10. **Checkout flow** - Navigate to checkout view within drawer, review items, confirm
11. **Checkout simulation** - Log transaction and show confirmation (no payment)
12. **Error handling** - Prevent checkout with empty cart, handle duplicate items

## Architecture Reference

**CRITICAL**: Follow all patterns and conventions from the specification:

### Key Specification Documents
- **Architecture**: `spec/ARCHITECTURE_OVERVIEW.md`
- **Project Rules**: `.cursorrules`
- **Backend Patterns**: 
  - `spec/service/graphql-federation-architecture.md` - GraphQL Federation patterns
  - `spec/service/database-schema-organization.md` - Database schema rules (⚠️ ALL tables MUST be in a schema, NEVER in public)
  - `spec/service/kotlin/jpa-entity.md` - JPA entity patterns
- **Frontend Patterns**:
  - `spec/web/web-src-structure.md` - Frontend directory structure
  - `spec/web/web-components.md` - Component system patterns
  - `spec/web/web-graphql-operations.md` - GraphQL operations patterns
  - `spec/web/web-themes.md` - Theme system
  - `spec/web/web-i18n-architecture.md` - i18n patterns
  - `spec/web/web-custom-hooks.md` - Custom hooks patterns

## Implementation Plan

### Phase 1: Frontend Cart State Management

#### 1.1 Create Cart Types
- **Location**: `web/src/lib/hooks/cart/types.ts`
- Define TypeScript types:
  - `CartItem` - Represents an item in the cart (id, catalogItemId, name, priceCents, quantity)
  - `Cart` - Represents the entire cart (items, totalPriceCents, itemCount)
  - `CartItemInput` - Input type for adding items to cart

**Requirements**:
- `CartItem` should reference `CatalogItem` by ID
- Include quantity field (default: 1)
- Include calculated subtotal (priceCents * quantity)
- `Cart` should have computed properties for totals

#### 1.2 Create Cart Context Provider
- **Location**: `web/src/shared/providers/CartProvider.tsx`
- Create React Context for cart state management
- Follow pattern from `AuthProvider.tsx`

**Requirements**:
- Use `localStorage` for cart persistence (key: `procureflow_cart`)
- Implement cart operations:
  - `addItem(item: CatalogItem, quantity?: number)` - Add item or increase quantity if exists
  - `removeItem(itemId: string)` - Remove item from cart
  - `updateQuantity(itemId: string, quantity: number)` - Update item quantity
  - `clearCart()` - Remove all items
  - `getItemCount()` - Get total number of items
  - `getTotalPrice()` - Calculate total price in cents
- Handle localStorage errors gracefully
- Sync state with localStorage on mount and updates
- Provide cart state: `items`, `itemCount`, `totalPriceCents`, `isEmpty`

#### 1.3 Create Cart Hook
- **Location**: `web/src/lib/hooks/cart/useCart.ts`
- Create custom hook that uses CartContext
- Follow pattern from `useCatalog.ts`

**Requirements**:
- Export `useCart()` hook that returns:
  - Cart state (items, itemCount, totalPriceCents, isEmpty)
  - Cart operations (addItem, removeItem, updateQuantity, clearCart)
  - Loading states if needed
- Handle edge cases:
  - Duplicate items (increase quantity instead of adding duplicate)
  - Quantity validation (minimum 1, maximum reasonable limit)
  - Item removal when quantity reaches 0

### Phase 2: Backend Checkout Logging (Optional but Recommended)

#### 2.1 Create Database Schema (Optional)
- **Location**: `service/kotlin/app/src/main/resources/db/migration/`
- **Schema**: Use `app` schema (NOT `public` - this is MANDATORY)
- **Migration File**: Create `V2_X__create_checkout_logs.sql` (if implementing server-side logging)

**Requirements** (if implementing):
- Create `app.checkout_logs` table with:
  - `id` (UUID, primary key, default uuidv7())
  - `user_id` (UUID, nullable - for future user association)
  - `items` (JSONB, NOT NULL) - Store cart items as JSON
  - `total_price_cents` (BIGINT, NOT NULL)
  - `item_count` (INTEGER, NOT NULL)
  - `status` (TEXT, NOT NULL) - enum: 'COMPLETED', 'FAILED'
  - `created_at` (TIMESTAMP, NOT NULL, default now())
- Add indexes:
  - Index on `created_at` for querying recent checkouts
  - Index on `user_id` (if implemented)
- Follow patterns from `spec/service/database-schema-organization.md`

#### 2.2 Create GraphQL Schema (Optional)
- **Location**: `service/kotlin/app/src/main/resources/graphql/schema.graphqls`
- Add checkout mutation (if implementing server-side logging):

```graphql
type CheckoutLog @key(fields: "id") {
  id: ID!
  userId: ID
  items: [CheckoutItem!]!
  totalPriceCents: Int!
  itemCount: Int!
  status: CheckoutStatus!
  createdAt: String!
}

type CheckoutItem {
  catalogItemId: ID!
  name: String!
  priceCents: Int!
  quantity: Int!
}

enum CheckoutStatus {
  COMPLETED
  FAILED
}

input CheckoutInput {
  items: [CheckoutItemInput!]!
  totalPriceCents: Int!
  itemCount: Int!
}

input CheckoutItemInput {
  catalogItemId: ID!
  name: String!
  priceCents: Int!
  quantity: Int!
}

type Mutation {
  checkout(input: CheckoutInput!): CheckoutLog!
}
```

**Requirements**:
- Use `@key(fields: "id")` for Federation support (if implementing)
- Follow naming conventions (camelCase for fields, PascalCase for types)
- Include all required fields for logging

#### 2.3 Create GraphQL Resolver
- **Location**: `service/kotlin/app/src/main/kotlin/.../resolver/`
- Create `CheckoutResolver` class (if implementing server-side logging):
  - Use `@GraphQLApi` annotation
  - Implement mutation resolver: `checkout(@Argument input: CheckoutInput): CheckoutLog`
  - Log checkout transaction
  - Return checkout log with status


### Phase 3: Frontend GraphQL Operations (If Implementing Server-Side)

#### 3.1 Create GraphQL Operations
- **Location**: `web/src/lib/graphql/operations/cart/`
- Create domain folder structure:
  - `mutations.ts` - Checkout mutation
  - `index.ts` - Exports

**Mutations** (`mutations.ts` - if implementing):
```typescript
import { gql } from '@apollo/client';

export const CHECKOUT = gql`
  mutation Checkout($input: CheckoutInput!) {
    checkout(input: $input) {
      id
      totalPriceCents
      itemCount
      status
      createdAt
    }
  }
`;
```

#### 3.2 Generate Types (If Implementing)
- Run code generation:
  ```bash
  cd web
  pnpm codegen
  ```

### Phase 4: Frontend Components

#### 4.1 Create Cart Icon Component
- **Location**: `web/src/shared/components/ui/navigation/CartIcon.tsx`
- Create cart icon with badge showing item count
- Follow Material-UI patterns from design system

**Requirements**:
- Display shopping cart icon (Material-UI `ShoppingCartIcon`)
- Show badge with item count when cart is not empty
- Badge should be hidden when cart is empty
- Click handler to open cart drawer/view
- Use theme tokens from `spec/web/web-themes.md`
- Add i18n support for accessibility labels

#### 4.2 Create Cart/Checkout Drawer Component
- **Location**: `web/src/app/(procureflow)/cart/components/CartDrawer.tsx`
- Create a right-side drawer component for cart and checkout

**Requirements**:
- Use Material-UI `Drawer` component with `anchor="right"`
- Drawer should slide in from the right side of the screen
- Drawer width should be appropriate (e.g., 400px on desktop, full width on mobile)
- Drawer should be controlled by open/close state
- Display all cart items with:
  - Item name
  - Price per unit
  - Quantity (with increment/decrement controls)
  - Subtotal (price * quantity)
  - Remove button
- Display cart summary:
  - Total item count
  - Total price
  - "Proceed to Checkout" button (disabled if cart is empty)
- Handle empty cart state with message
- Use Material-UI components from design system
- Follow component patterns from `spec/web/web-components.md`
- Use theme tokens from `spec/web/web-themes.md`
- Add i18n support
- Ensure drawer is accessible (keyboard navigation, focus management, ARIA labels)

#### 4.3 Update App Header with Cart Icon
- **Location**: `web/src/shared/ui/shell/AppHeader.tsx`
- Add CartIcon to the header's right-side actions section
- Cart icon must be visible across all pages in the header

**Requirements**:
- Integrate CartIcon into the AppHeader component
- Position in the right-side actions area (alongside theme toggle and user avatar)
- Cart icon should open the cart/checkout drawer when clicked
- Ensure it's accessible (keyboard navigation, ARIA labels)
- Ensure it's responsive (works on mobile and desktop)
- Badge should display item count when cart is not empty
- Icon should be visually consistent with other header actions

#### 4.4 Update Catalog Search Component
- **Location**: `web/src/app/(procureflow)/catalog/components/CatalogSearch.tsx`
- Add "Add to Cart" functionality with quantity selection to each catalog item

**Requirements**:
- Add quantity input/selector (number input or stepper) to each search result item
- Default quantity should be 1
- Quantity input should have minimum value of 1
- Add "Add to Cart" button/icon to each search result item
- On click, add item to cart with specified quantity using `useCart()` hook
- Show feedback (toast notification) when item is added
- Handle duplicate items (show message that quantity was increased)
- Quantity selector should be accessible and user-friendly
- Use Material-UI components (TextField with type="number" or InputAdornment with increment/decrement buttons)
- Add i18n support

#### 4.5 Integrate Checkout into Cart Drawer
- **Location**: `web/src/app/(procureflow)/cart/components/CartDrawer.tsx`
- Integrate checkout functionality into the cart drawer (same drawer, different view state)

**Requirements**:
- The cart drawer should support two view states:
  - **Cart View**: Display cart items with management options
  - **Checkout View**: Display cart items for review and confirmation
- Add state management to switch between cart and checkout views
- In Checkout View, display all cart items with editable quantities:
  - Item name and details
  - Quantity input/selector (number input or stepper) that allows modification
  - Price per unit
  - Subtotal (calculated: price * quantity)
  - Remove button
- Quantity changes should update:
  - Item subtotal in real-time
  - Cart total in real-time
  - Cart state (persist to localStorage)
- Display order summary in checkout view:
  - Item count (total units)
  - Subtotal
  - Total
- "Confirm Checkout" button in checkout view
- Navigation between views:
  - "Proceed to Checkout" button in cart view → switches to checkout view
  - "Back to Cart" button in checkout view → switches back to cart view
- Handle empty cart (show message, disable checkout)
- Quantity validation (minimum 1, reasonable maximum)
- Use Material-UI components from design system
- Add i18n support
- Ensure smooth transitions between views

#### 4.6 Create Checkout Confirmation (Modal or Drawer View)
- **Location**: `web/src/app/(procureflow)/cart/components/CheckoutConfirmation.tsx`
- Create confirmation component shown after successful checkout

**Requirements**:
- Can be implemented as:
  - A third view state in the cart drawer (Confirmation View)
  - A modal dialog overlay
  - A success message within the drawer
- Display success message
- Show checkout summary (items, total)
- Show checkout ID/timestamp (if server-side logging implemented)
- "Continue Shopping" button (closes drawer and optionally redirects to catalog)
- Clear cart after showing confirmation
- Auto-close drawer after a delay (optional, e.g., 3-5 seconds) or on button click
- Use Material-UI components
- Add i18n support

#### 4.7 Add Cart Provider to App
- **Location**: `web/src/app/providers.tsx`
- Wrap application with CartProvider

**Requirements**:
- Add `<CartProvider>` to providers tree
- Ensure it's available throughout the app
- Follow pattern from existing providers

### Phase 5: i18n Translations

#### 5.1 Create Cart Translations
- **Location**: `web/src/app/(procureflow)/cart/i18n/locales/`
- Create translation files:
  - `en.json` - English translations
  - `pt.json` - Portuguese translations (if needed)
- Create `index.ts` for translation exports

**Required translations**:
- Cart icon accessibility label
- Cart view title
- Empty cart message
- Item labels (name, price, quantity, subtotal)
- Action buttons (remove, update quantity, clear cart, checkout)
- Checkout page title and labels
- Confirmation page messages
- Error messages (empty cart, etc.)
- Success messages (item added, checkout complete)

#### 5.2 Create Checkout Translations
- **Location**: `web/src/app/(procureflow)/checkout/i18n/locales/`
- Create translation files similar to cart translations

### Phase 6: Testing

#### 6.1 Frontend Component Tests
- **Location**: `web/src/app/(procureflow)/cart/__tests__/`
- Create component tests:
  - Test cart operations (add, remove, update quantity, clear)
  - Test cart persistence (localStorage)
  - Test cart icon badge display
  - Test checkout flow
  - Test empty cart handling
  - Test duplicate item handling
  - Use MSW for GraphQL mocking (if server-side implemented)

#### 6.2 Integration Tests
- Test full checkout flow from catalog to confirmation
- Test cart persistence across navigation
- Test error scenarios

## Implementation Checklist

### Frontend State Management
- [ ] Create cart types (`types.ts`)
- [ ] Create CartProvider with localStorage persistence
- [ ] Create useCart hook
- [ ] Add CartProvider to app providers

### Frontend Components
- [ ] Create CartIcon component with badge
- [ ] Create Cart/Checkout Drawer component (right-side drawer)
- [ ] Update AppHeader to include CartIcon in right-side actions
- [ ] Update CatalogSearch to add "Add to Cart" buttons
- [ ] Integrate checkout view into Cart Drawer
- [ ] Create Checkout Confirmation component (modal or drawer view)

### Backend (Optional - for logging)
- [ ] Create Flyway migration with `app` schema (if implementing)
- [ ] Create `checkout_logs` table (if implementing)
- [ ] Create domain model classes (if implementing)
- [ ] Create JPA entity (if implementing)
- [ ] Create repository (if implementing)
- [ ] Create service class (if implementing)
- [ ] Create GraphQL schema (if implementing)
- [ ] Create GraphQL resolver (if implementing)
- [ ] Sync schema to contracts directory (if implementing)
- [ ] Generate supergraph schema (if implementing)

### Frontend GraphQL (If Implementing Server-Side)
- [ ] Create GraphQL operations (mutations)
- [ ] Run code generation
- [ ] Verify generated types

### i18n
- [ ] Create cart translations (en.json, pt.json)
- [ ] Create checkout translations (en.json, pt.json)
- [ ] Add translation exports

### Testing
- [ ] Frontend component tests
- [ ] Integration tests
- [ ] E2E tests (optional but recommended)

## Critical Requirements

1. **Cart Icon in Header**: Cart icon MUST be surfaced in the AppHeader component's right-side actions area
2. **Checkout as Drawer**: Checkout MUST be implemented as a right-side drawer (not a full page)
3. **Cart Persistence**: Cart must persist across page navigation using localStorage
4. **Type Safety**: Maintain end-to-end type safety
5. **Quantity Selection**: Users must be able to set quantity when adding items to cart
6. **Quantity Editing**: Users must be able to modify quantities during checkout
7. **Real-time Updates**: Quantity changes should update subtotals and totals in real-time
8. **Duplicate Handling**: Adding duplicate items should increase quantity, not create duplicates
9. **Empty Cart Handling**: Prevent checkout with empty cart, show appropriate messages
10. **Component System**: Use components from design system, not custom implementations
11. **i18n**: All UI strings must be internationalized
12. **Error Handling**: Proper error handling at all layers
13. **Validation**: Quantity validation (minimum 1, reasonable maximum)
14. **User Feedback**: Toast notifications for cart operations
15. **Accessibility**: Cart icon, drawer, quantity inputs, and all components must be accessible
16. **Drawer Behavior**: Drawer should slide in from right, be dismissible (click outside or ESC key), and handle focus management

## Feature Scenarios to Implement

### Scenario 1: Add a single item to cart
- Given: User is viewing catalog items
- When: User selects "USB-C Cable - 1m" and adds to cart
- Then: Cart contains 1 item, cart icon shows badge with count "1"

### Scenario 1a: Add item to cart with specific quantity
- Given: User is viewing catalog items
- When: User selects "USB-C Cable - 1m", sets quantity to 5, and adds to cart
- Then: Cart contains 1 item with quantity 5, cart icon shows badge with count "5"

### Scenario 2: Add multiple items to cart
- Given: User is viewing catalog items
- When: User adds 3 different items to cart
- Then: Cart contains 3 items, cart icon shows badge with count "3"

### Scenario 3: View cart contents
- Given: Cart contains 2 items
- When: User opens the cart
- Then: Cart displays all items with name, price, and quantity, shows total price

### Scenario 4: Remove item from cart
- Given: Cart contains 3 items
- When: User removes "Wireless Mouse" from cart
- Then: Cart contains 2 items, cart icon shows badge with count "2"

### Scenario 5: Update item quantity in cart
- Given: Cart contains "USB-C Cable - 1m" with quantity 1
- When: User increases quantity to 3
- Then: Cart displays item with quantity 3, subtotal reflects updated quantity

### Scenario 6: Clear entire cart
- Given: Cart contains multiple items
- When: User clears the cart
- Then: Cart is empty, cart icon shows no badge, empty cart message displayed

### Scenario 7: Set item quantity during checkout
- Given: User is on checkout page with "USB-C Cable - 1m" (quantity 1)
- When: User sets quantity for "USB-C Cable - 1m" to 3
- Then: Checkout page displays item with quantity 3, subtotal and total reflect updated quantity

### Scenario 8: Complete checkout successfully
- Given: User is on checkout page with items in cart
- When: User confirms checkout
- Then: System simulates successful checkout, logs transaction (if server-side), shows confirmation, cart is cleared, user redirected to confirmation page

### Scenario 9: Attempt checkout with empty cart
- Given: Cart is empty
- When: User attempts to proceed to checkout
- Then: System prevents checkout, shows "Your cart is empty" message

### Scenario 10: Attempt to add duplicate item to cart
- Given: Cart already contains "USB-C Cable - 1m"
- When: User attempts to add "USB-C Cable - 1m" again
- Then: System increases quantity of existing item, cart does not contain duplicate entries

### Scenario 11: Cart persists across page navigation
- Given: User has added items to cart
- When: User navigates to different page and returns
- Then: Cart still contains previously added items, cart icon shows correct count

## Additional Notes

- **Client-Side First**: For MVP, cart can be entirely client-side with localStorage. Server-side logging is optional but recommended for production.
- **Checkout Simulation**: Since there's no payment processing, checkout should:
  - Log the transaction (client-side console.log or server-side if implemented)
  - Show confirmation message
  - Clear the cart
  - Show confirmation in drawer or modal
- **Cart Icon Placement**: Cart icon MUST be in the AppHeader component's right-side actions area (alongside theme toggle and user avatar)
- **Drawer Implementation**: 
  - Use Material-UI `Drawer` component with `anchor="right"`
  - Drawer should be controlled by state (open/close)
  - Drawer should be dismissible by clicking outside (backdrop) or pressing ESC key
  - Drawer should handle focus management (trap focus when open, return focus when closed)
  - Drawer width: ~400px on desktop, full width on mobile (use responsive breakpoints)
- **Performance**: Consider debouncing localStorage writes if needed
- **Error Handling**: Handle localStorage quota exceeded errors gracefully
- **Accessibility**: 
  - Ensure cart icon and drawer are keyboard accessible and screen reader friendly
  - Use proper ARIA labels for drawer and cart icon
  - Ensure focus management when drawer opens/closes
- **Responsive Design**: Drawer should work well on mobile devices (full width on small screens)

## Success Criteria

The implementation is complete when:
1. All eleven feature scenarios pass
2. Cart icon is displayed in AppHeader's right-side actions area
3. Cart/checkout drawer opens from the right side when cart icon is clicked
4. Users can set quantity when adding items to cart from catalog
5. Users can modify quantities during checkout (in drawer)
6. Quantity changes update subtotals and totals in real-time
7. Cart persists across page navigation
8. Cart icon displays correct item count badge
9. Users can add, remove, update quantities, and clear cart
10. Checkout flow works end-to-end within the drawer (cart view → checkout view → confirmation)
11. Empty cart is handled gracefully
12. Duplicate items increase quantity instead of creating duplicates
13. Drawer is accessible (keyboard navigation, focus management, ARIA labels)
14. All UI strings are internationalized
15. Components use design system and themes
16. Tests pass for all layers
17. Code follows all patterns from specification

