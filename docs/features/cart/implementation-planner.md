# Cart and Checkout Feature Implementation Plan

## Overview

Implement a complete cart and checkout system following the feature specification in `docs/features/cart/cart-and-checkout.feature`. The implementation will be client-side first with localStorage persistence, featuring a right-side drawer UI, quantity management, and optional server-side checkout logging.

## Architecture Decisions

- **Client-Side First**: Cart state managed in React Context with localStorage persistence
- **Drawer UI**: Right-side Material-UI Drawer component (already exists in design system)
- **Server-Side Logging**: Optional - can be implemented later for checkout transaction logging
- **Type Safety**: End-to-end TypeScript types from cart state to UI

## Implementation Phases

### Phase 1: Frontend Cart State Management

#### 1.1 Create Cart Types

**File**: `web/src/lib/hooks/cart/types.ts`

Define TypeScript types:

- `CartItem`: `{ id: string, catalogItemId: string, name: string, priceCents: number, quantity: number }`
- `Cart`: `{ items: CartItem[], itemCount: number, totalPriceCents: number, isEmpty: boolean }`
- `CartItemInput`: Input type for adding items

#### 1.2 Create CartProvider

**File**: `web/src/shared/providers/CartProvider.tsx`

Follow `AuthProvider.tsx` pattern:

- React Context for cart state
- localStorage persistence (key: `procureflow_cart`)
- Operations: `addItem`, `removeItem`, `updateQuantity`, `clearCart`
- Computed properties: `itemCount`, `totalPriceCents`, `isEmpty`
- Handle duplicate items (increase quantity)
- Graceful localStorage error handling

#### 1.3 Create useCart Hook

**File**: `web/src/lib/hooks/cart/useCart.ts`

Follow `useCatalog.ts` pattern:

- Export `useCart()` hook using CartContext
- Return cart state and operations
- Quantity validation (min: 1, max: reasonable limit)
- Auto-remove items when quantity reaches 0

#### 1.4 Add CartProvider to App

**File**: `web/src/app/providers.tsx`

Add `<CartProvider>` to provider tree (after AuthProvider, before ToastProvider)

### Phase 2: Frontend Components

#### 2.1 Create CartIcon Component

**File**: `web/src/shared/components/ui/navigation/CartIcon.tsx`

- Material-UI `ShoppingCartIcon` with Badge
- Badge shows item count when cart not empty
- Click handler to open cart drawer
- Use theme tokens from design system
- i18n for accessibility labels
- Follow Badge component pattern from design system

#### 2.2 Update AppHeader

**File**: `web/src/shared/ui/shell/AppHeader.tsx`

Add CartIcon to right-side actions area (between theme toggle and profile avatar):

- Import CartIcon and useCart hook
- Add state for drawer open/close
- Position CartIcon in the actions Box
- Ensure responsive and accessible

#### 2.3 Create CartDrawer Component

**File**: `web/src/app/(procureflow)/cart/components/CartDrawer.tsx`

Use existing Drawer component from design system:

- Right-side drawer (`anchor="right"`)
- Width: 400px desktop, full width mobile
- Two view states: `cart` and `checkout`
- Cart view: display items, quantity controls, remove buttons, summary, "Proceed to Checkout" button
- Checkout view: editable quantities, order summary, "Confirm Checkout" button, "Back to Cart" button
- Empty cart state handling
- Real-time subtotal/total updates
- Use Material-UI components from design system

#### 2.4 Create CheckoutConfirmation Component

**File**: `web/src/app/(procureflow)/cart/components/CheckoutConfirmation.tsx`

- Third view state in drawer or modal overlay
- Success message and checkout summary
- "Continue Shopping" button (closes drawer)
- Clear cart after confirmation
- Optional auto-close after delay

#### 2.5 Update CatalogSearch Component

**File**: `web/src/app/(procureflow)/catalog/components/CatalogSearch.tsx`

Add "Add to Cart" functionality:

- Quantity input/stepper for each catalog item (default: 1, min: 1)
- "Add to Cart" button/icon per item
- Use `useCart().addItem()` hook
- Show toast notification on add
- Handle duplicate items (show message about quantity increase)
- Use Material-UI TextField with type="number" or stepper controls

### Phase 3: i18n Translations

#### 3.1 Create Cart Translations

**Files**:

- `web/src/app/(procureflow)/cart/i18n/locales/en.json`
- `web/src/app/(procureflow)/cart/i18n/locales/pt.json`
- `web/src/app/(procureflow)/cart/i18n/index.ts`

Follow catalog i18n pattern:

- Cart icon accessibility labels
- Cart/checkout view titles
- Empty cart messages
- Item labels (name, price, quantity, subtotal)
- Action buttons (remove, update, clear, checkout, confirm)
- Confirmation messages
- Error messages
- Success messages

### Phase 4: Optional Backend Checkout Logging

#### 4.1 Database Migration (Optional)

**File**: `service/kotlin/app/src/main/resources/db/migration/V2_X__create_checkout_logs.sql`

**CRITICAL**: Use `app` schema (NOT `public`):

- Create `app.checkout_logs` table
- Fields: `id` (UUID, uuidv7), `user_id` (UUID, nullable), `items` (JSONB), `total_price_cents` (BIGINT), `item_count` (INTEGER), `status` (TEXT enum), `created_at` (TIMESTAMP)
- Indexes on `created_at` and `user_id`

#### 4.2 GraphQL Schema (Optional)

**File**: `service/kotlin/app/src/main/resources/graphql/schema.graphqls`

Add checkout mutation types:

- `CheckoutLog` type with `@key(fields: "id")` for Federation
- `CheckoutItem` type
- `CheckoutStatus` enum
- `CheckoutInput` and `CheckoutItemInput` input types
- `checkout` mutation

#### 4.3 GraphQL Resolver (Optional)

**File**: `service/kotlin/app/src/main/kotlin/.../resolver/CheckoutResolver.kt`

- `@GraphQLApi` annotation
- `checkout(@Argument input: CheckoutInput): CheckoutLog` mutation
- Log transaction to database
- Return checkout log with status

#### 4.4 Frontend GraphQL Operations (Optional)

**Files**:

- `web/src/lib/graphql/operations/cart/mutations.ts`
- `web/src/lib/graphql/operations/cart/index.ts`

- `CHECKOUT` mutation
- Run `pnpm codegen` to generate types
- Update CartDrawer to call mutation on checkout confirmation

### Phase 5: Testing

#### 5.1 Component Tests

**File**: `web/src/app/(procureflow)/cart/__tests__/`

Test:

- Cart operations (add, remove, update, clear)
- localStorage persistence
- Cart icon badge display
- Drawer open/close
- Checkout flow
- Empty cart handling
- Duplicate item handling

## File Structure

```
web/src/
├── lib/hooks/cart/
│   ├── types.ts
│   ├── useCart.ts
│   └── index.ts
├── shared/providers/
│   ├── CartProvider.tsx
│   └── index.ts (update exports)
├── shared/components/ui/navigation/
│   └── CartIcon.tsx
├── shared/ui/shell/
│   └── AppHeader.tsx (update)
├── app/(procureflow)/
│   ├── cart/
│   │   ├── components/
│   │   │   ├── CartDrawer.tsx
│   │   │   ├── CheckoutConfirmation.tsx
│   │   │   └── index.ts
│   │   ├── i18n/
│   │   │   ├── locales/
│   │   │   │   ├── en.json
│   │   │   │   └── pt.json
│   │   │   └── index.ts
│   │   └── __tests__/
│   └── catalog/
│       └── components/
│           └── CatalogSearch.tsx (update)
└── app/
    └── providers.tsx (update)

service/kotlin/app/ (optional)
├── src/main/resources/
│   ├── db/migration/
│   │   └── V2_X__create_checkout_logs.sql
│   └── graphql/
│       └── schema.graphqls (update)
└── src/main/kotlin/.../resolver/
    └── CheckoutResolver.kt
```

## Critical Requirements

1. **Cart Icon in Header**: Must be in AppHeader's right-side actions area
2. **Right-Side Drawer**: Use existing Drawer component with `anchor="right"`
3. **localStorage Persistence**: Key `procureflow_cart`, handle errors gracefully
4. **Quantity Selection**: Users can set quantity when adding items
5. **Quantity Editing**: Users can modify quantities during checkout
6. **Real-time Updates**: Quantity changes update subtotals/totals immediately
7. **Duplicate Handling**: Adding duplicate items increases quantity
8. **Empty Cart Handling**: Prevent checkout, show appropriate messages
9. **Accessibility**: Keyboard navigation, focus management, ARIA labels
10. **i18n**: All UI strings internationalized
11. **Design System**: Use components from design system, not custom implementations

## Success Criteria

- All 11 feature scenarios from `cart-and-checkout.feature` pass
- Cart icon visible in AppHeader with correct badge count
- Right-side drawer opens/closes correctly
- Cart persists across page navigation
- Quantity selection and editing work correctly
- Checkout flow completes end-to-end
- Empty cart handled gracefully
- All components accessible and responsive
- Tests pass for all layers