# Search and Enroll Catalog Feature - Implementation Plan

## Overview

Implement a complete full-stack solution for searching and enrolling catalog items (materials and services) as specified in `docs/features/catalog/search-and-enroll.feature`. The implementation will follow all patterns from the specification documents.

## Phase 1: Database Layer

### 1.1 Create Database Migration

**File**: `service/kotlin/app/src/main/resources/db/migration/V1_2__create_catalog_items.sql`

- Create `app` schema if not exists (following spec requirement - existing migration V1_1 doesn't use schema)
- Create `app.catalog_items` table with:
  - `id` (UUID, primary key, default uuidv7())
  - `name` (TEXT, NOT NULL)
  - `category` (TEXT, NOT NULL) - values: 'MATERIAL', 'SERVICE'
  - `price_cents` (BIGINT, NOT NULL)
  - `status` (TEXT, NOT NULL) - values: 'ACTIVE', 'PENDING_APPROVAL', 'INACTIVE'
  - `created_at`, `updated_at` (TIMESTAMP, NOT NULL, default now())
  - `version` (BIGINT, NOT NULL, default 0) - for optimistic locking
- Add indexes:
  - Index on `name` for search (consider GIN for full-text search)
  - Index on `category`
  - Index on `status`
- Follow `spec/service/database-schema-organization.md`: use schema-qualified names, set search path

### 1.2 Create Domain Model

**File**: `service/kotlin/app/src/main/kotlin/io/github/salomax/procureflow/catalog/domain/CatalogItem.kt`

- Create `CatalogItem` data class with all fields
- Create enum `CatalogItemCategory` (MATERIAL, SERVICE)
- Create enum `CatalogItemStatus` (ACTIVE, PENDING_APPROVAL, INACTIVE)
- Implement `toEntity()` method

### 1.3 Create JPA Entity

**File**: `service/kotlin/app/src/main/kotlin/io/github/salomax/procureflow/catalog/entity/CatalogItemEntity.kt`

- Extend `BaseEntity<UUID?>`
- Use `@Table(name = "catalog_items", schema = "app")` - schema is REQUIRED
- Follow patterns from `spec/service/kotlin/jpa-entity.md`:
  - Use `open` class
  - Use `@Version` for optimistic locking
  - Use `Instant` for timestamps
  - Use `@Enumerated(EnumType.STRING)` for enums
  - Implement `toDomain()` method

### 1.4 Create Repository

**File**: `service/kotlin/app/src/main/kotlin/io/github/salomax/procureflow/catalog/repository/CatalogItemRepository.kt`

- Extend `JpaRepository<CatalogItemEntity, UUID>`
- Add custom query methods:
  - `findByNameContainingIgnoreCase(name: String): List<CatalogItemEntity>`
  - `findByCategory(category: CatalogItemCategory): List<CatalogItemEntity>`
  - `findByStatus(status: CatalogItemStatus): List<CatalogItemEntity>`

## Phase 2: Backend Service Layer

### 2.1 Create Service

**File**: `service/kotlin/app/src/main/kotlin/io/github/salomax/procureflow/catalog/service/CatalogItemService.kt`

- Implement business logic:
  - `search(query: String): List<CatalogItem>` - case-insensitive partial match
  - `create(item: CatalogItem): CatalogItem` - with validation
  - `get(id: UUID): CatalogItem?`
- Use dependency injection (`@Singleton`)
- Handle optimistic locking exceptions
- Convert between Entity and Domain models

### 2.2 Create GraphQL Schema

**File**: `service/kotlin/app/src/main/resources/graphql/schema.graphqls`

Add to existing schema:

```graphql
enum CatalogItemCategory {
  MATERIAL
  SERVICE
}

enum CatalogItemStatus {
  ACTIVE
  PENDING_APPROVAL
  INACTIVE
}

type CatalogItem @key(fields: "id") {
  id: ID!
  name: String!
  category: CatalogItemCategory!
  priceCents: Int!
  status: CatalogItemStatus!
  createdAt: String!
  updatedAt: String!
}

input CatalogItemInput {
  name: String!
  category: CatalogItemCategory!
  priceCents: Int!
}

extend type Query {
  searchCatalogItems(query: String!): [CatalogItem!]!
  catalogItem(id: ID!): CatalogItem
}

extend type Mutation {
  enrollCatalogItem(input: CatalogItemInput!): CatalogItem!
}
```

### 2.3 Create GraphQL DTOs

**File**: `service/kotlin/app/src/main/kotlin/io/github/salomax/procureflow/catalog/graphql/dto/CatalogItemInputDTO.kt`

- Create DTOs for GraphQL input/output mapping
- Follow existing pattern from ProductInputDTO

### 2.4 Create GraphQL Resolver

**File**: `service/kotlin/app/src/main/kotlin/io/github/salomax/procureflow/catalog/graphql/resolvers/CatalogItemResolver.kt`

- Use `@GraphQLApi` annotation
- Implement query resolvers:
  - `searchCatalogItems(@Argument query: String): List<CatalogItem>`
  - `catalogItem(@Argument id: ID): CatalogItem?`
- Implement mutation resolver:
  - `enrollCatalogItem(@Argument input: CatalogItemInput): CatalogItem`
- Use service layer for business logic
- Consider using GenericCrudResolver pattern (like ProductResolver) or implement directly

### 2.5 Update GraphQL Factory

**File**: `service/kotlin/app/src/main/kotlin/io/github/salomax/neotool/example/graphql/GraphQLFactory.kt`

- Add `CatalogItem` to `fetchEntities` callback
- Add `CatalogItem` to `resolveEntityType` callback
- Follow Federation patterns from `spec/service/graphql-federation-architecture.md`

### 2.6 Update AppWiringFactory

**File**: `service/kotlin/app/src/main/kotlin/io/github/salomax/neotool/example/graphql/AppWiringFactory.kt`

- Register CatalogItemResolver
- Add data fetchers for `searchCatalogItems`, `catalogItem`, `enrollCatalogItem`
- Follow existing pattern from ProductResolver and CustomerResolver

### 2.7 Sync GraphQL Schema

- Run `./neotool graphql sync` (select source: kotlin/app, target: app)
- Run `./neotool graphql generate` to generate supergraph

## Phase 3: Frontend GraphQL Operations

### 3.1 Create Fragments

**File**: `web/src/lib/graphql/fragments/catalog.ts`

- Create `CATALOG_ITEM_FIELDS` fragment with all catalog item fields
- Follow pattern from existing fragments

### 3.2 Create GraphQL Operations

**Files**:

- `web/src/lib/graphql/operations/catalog/queries.ts`
- `web/src/lib/graphql/operations/catalog/mutations.ts`
- `web/src/lib/graphql/operations/catalog/index.ts`

**Queries**:

- `SEARCH_CATALOG_ITEMS` - search query with query parameter
- `GET_CATALOG_ITEM` - get single item by ID

**Mutations**:

- `ENROLL_CATALOG_ITEM` - enroll new item

### 3.3 Generate Types

- Run `cd web && pnpm codegen` to generate TypeScript types and hooks
- Verify generated files in `web/src/lib/graphql/operations/catalog/*.generated.ts`

## Phase 4: Frontend Components

### 4.1 Create Domain Hook

**File**: `web/src/lib/hooks/catalog/useCatalog.ts`

- Follow pattern from `useCustomers.ts`
- Implement:
  - Search functionality with debouncing
  - Enrollment functionality
  - Loading and error states
  - Return type with all necessary state and functions

### 4.2 Create Search Component

**File**: `web/src/app/(neotool)/catalog/components/CatalogSearch.tsx`

- Use Material-UI components from design system
- Implement search input with debouncing
- Display search results list
- Show "Enroll New Item" button when no results
- Use theme tokens from `spec/web/web-themes.md`
- Add i18n support

### 4.3 Create Enrollment Form Component

**File**: `web/src/app/(neotool)/catalog/components/EnrollItemForm.tsx`

- Form fields: Name (text), Category (select: Material/Service), Price (currency)
- Use form components from design system
- Validation (required fields, price > 0)
- Submit handler calling enrollment mutation
- Success/error feedback (toast notifications)
- Dialog/modal wrapper for form

### 4.4 Create Catalog Page

**File**: `web/src/app/(neotool)/catalog/page.tsx`

- Integrate CatalogSearch and EnrollItemForm components
- Handle state management (dialog open/close)
- Follow Next.js App Router patterns
- Use layout from `(neotool)` route group

### 4.5 Add i18n Translations

**Files**:

- `web/src/app/(neotool)/catalog/i18n/locales/en.json`
- `web/src/app/(neotool)/catalog/i18n/locales/pt.json` (if needed)
- `web/src/app/(neotool)/catalog/i18n/index.ts`

- Include all UI strings:
  - Search placeholder, no results message
  - Form labels, validation messages
  - Success/error messages
  - Button labels
- Follow i18n patterns from `spec/web/web-i18n-architecture.md`

## Phase 5: Testing

### 5.1 Backend Integration Tests

**File**: `service/kotlin/app/src/test/kotlin/.../catalog/CatalogItemServiceTest.kt`

- Test repository queries (search functionality)
- Test service layer logic
- Test GraphQL resolvers
- Use Testcontainers for database tests
- Follow testing patterns from `spec/service/testing-guidelines.md`

### 5.2 Frontend Component Tests

**Files**:

- `web/src/app/(neotool)/catalog/__tests__/CatalogSearch.test.tsx`
- `web/src/app/(neotool)/catalog/__tests__/EnrollItemForm.test.tsx`

- Test search functionality
- Test enrollment form validation and submission
- Test error handling
- Use MSW for GraphQL mocking

## Critical Requirements Checklist

- [ ] Database: ALL tables in `app` schema, NEVER in `public`
- [ ] Type Safety: End-to-end from database to UI
- [ ] GraphQL Federation: Use `@key(fields: "id")` directive
- [ ] Component System: Use components from `shared/components/ui/`
- [ ] i18n: All UI strings internationalized
- [ ] Error Handling: Proper error handling at all layers
- [ ] Validation: Input validation at API boundaries
- [ ] Testing: Comprehensive test coverage

## Feature Scenarios Coverage

1. **Happy Path**: Search finds existing item - displays results with status
2. **Sad Path**: Search returns empty - shows "Enroll New Item" option
3. **Enrollment**: Create new item - form submission creates item with appropriate status

## Implementation Order

1. Database migration and domain/entity/repository (Phase 1)
2. Backend service and GraphQL (Phase 2)
3. Sync GraphQL schema and generate supergraph
4. Frontend GraphQL operations and codegen (Phase 3)
5. Frontend components and i18n (Phase 4)
6. Testing (Phase 5)

## Notes

- Existing migration V1_1 doesn't follow spec (no schema), but new migration will follow spec
- Follow existing patterns from Product/Customer implementations
- Ensure GraphQL factory Federation resolvers are updated
- All components must use design system, not custom implementations