# Implementation Prompt for Search and Enroll Feature

## Context

Implement a full-stack solution for the **Search and Enroll Materials and Services** feature as specified in `docs/features/catalog/search-and-enroll.feature`. This feature allows procurement users to search a central catalog for items and enroll new ones when they don't exist.

## Feature Requirements

The feature must support:
1. **Search existing items** by name (partial match)
2. **Display search results** with item availability status
3. **Show enrollment option** when no results are found
4. **Enroll new items** with Name, Category, Price, and Status
5. **Support both Materials and Services** as item types

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

## Implementation Plan

### Phase 1: Database Layer

#### 1.1 Create Database Schema
- **Location**: `service/kotlin/app/src/main/resources/db/migration/`
- **Schema**: Use `app` schema (NOT `public` - this is MANDATORY)
- **Migration File**: Create `V1_X__create_catalog_items.sql`

**Requirements**:
- Create `app.catalog_items` table with:
  - `id` (UUID, primary key, default uuidv7())
  - `name` (TEXT, NOT NULL)
  - `category` (TEXT, NOT NULL) - enum-like: 'Material' or 'Service'
  - `price_cents` (BIGINT, NOT NULL) - store price in cents
  - `status` (TEXT, NOT NULL) - enum: 'Active', 'Pending Approval', 'Inactive'
  - `created_at` (TIMESTAMP, NOT NULL, default now())
  - `updated_at` (TIMESTAMP, NOT NULL, default now())
  - `version` (BIGINT, NOT NULL, default 0) - for optimistic locking
- Add indexes:
  - Index on `name` for search performance (consider GIN index for full-text search)
  - Index on `category`
  - Index on `status`
- Follow patterns from `spec/service/database-schema-organization.md`:
  - ✅ Create schema if not exists: `CREATE SCHEMA IF NOT EXISTS app;`
  - ✅ Set search path: `SET search_path TO app, public;`
  - ✅ Use schema-qualified table names: `app.catalog_items`
  - ❌ NEVER use `public` schema

#### 1.2 Create Domain Model
- **Location**: `service/kotlin/app/src/main/kotlin/.../domain/`
- Create `CatalogItem` data class with:
  - All fields matching database columns
  - `toEntity()` method for conversion
  - Enum classes: `CatalogItemCategory` (Material, Service), `CatalogItemStatus` (Active, PendingApproval, Inactive)

#### 1.3 Create JPA Entity
- **Location**: `service/kotlin/app/src/main/kotlin/.../entity/`
- Create `CatalogItemEntity` class:
  - Extend `BaseEntity<UUID?>`
  - Use `@Table(name = "catalog_items", schema = "app")` - schema is REQUIRED
  - Follow patterns from `spec/service/kotlin/jpa-entity.md`:
    - Use `open` class for JPA proxy generation
    - Use `@Version` for optimistic locking
    - Use `Instant` for timestamps
    - Implement `toDomain()` method
    - Use `@Enumerated(EnumType.STRING)` for enums
    - Use `@Column(columnDefinition = "uuid")` for UUID fields

#### 1.4 Create Repository
- **Location**: `service/kotlin/app/src/main/kotlin/.../repository/`
- Create `CatalogItemRepository` interface:
  - Extend `JpaRepository<CatalogItemEntity, UUID>`
  - Add custom query methods:
    - `findByNameContainingIgnoreCase(name: String): List<CatalogItemEntity>`
    - `findByCategory(category: CatalogItemCategory): List<CatalogItemEntity>`
    - `findByStatus(status: CatalogItemStatus): List<CatalogItemEntity>`

### Phase 2: Backend Service Layer

#### 2.1 Create Service
- **Location**: `service/kotlin/app/src/main/kotlin/.../service/`
- Create `CatalogItemService` class:
  - Implement business logic for:
    - Search items by name (case-insensitive, partial match)
    - Create new catalog item
    - Validate item data
  - Follow clean architecture: Service → Repository → Entity
  - Use dependency injection (Micronaut `@Singleton`)
  - Handle optimistic locking exceptions
  - Convert between Entity and Domain models

#### 2.2 Create GraphQL Schema
- **Location**: `service/kotlin/app/src/main/resources/graphql/schema.graphqls`
- Add to existing schema or create new schema file:

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

type Query {
  searchCatalogItems(query: String!): [CatalogItem!]!
  catalogItem(id: ID!): CatalogItem
}

type Mutation {
  enrollCatalogItem(input: CatalogItemInput!): CatalogItem!
}
```

**Requirements**:
- Use `@key(fields: "id")` for Federation support
- Follow naming conventions (camelCase for fields, PascalCase for types)
- Include all required fields from feature specification

#### 2.3 Create GraphQL Resolvers
- **Location**: `service/kotlin/app/src/main/kotlin/.../resolver/`
- Create `CatalogItemResolver` class:
  - Use `@GraphQLApi` annotation
  - Implement query resolvers:
    - `searchCatalogItems(@Argument query: String): List<CatalogItem>`
    - `catalogItem(@Argument id: ID): CatalogItem?`
  - Implement mutation resolver:
    - `enrollCatalogItem(@Argument input: CatalogItemInput): CatalogItem`
  - Use service layer for business logic
  - Handle errors appropriately

#### 2.4 Update GraphQL Factory (if needed)
- **Location**: `service/kotlin/app/src/main/kotlin/.../graphql/`
- Ensure Federation resolvers are configured:
  - `fetchEntities` callback for `CatalogItem` entity
  - `resolveEntityType` callback for `CatalogItem` type
  - Follow patterns from `spec/service/graphql-federation-architecture.md`

#### 2.5 Sync Schema to Contracts
- After creating/updating schema, run:
  ```bash
  ./neotool graphql sync
  # Select source: kotlin/app
  # Select target: app
  ```
- Then generate supergraph:
  ```bash
  ./neotool graphql generate
  ```

### Phase 3: Frontend GraphQL Operations

#### 3.1 Create GraphQL Operations
- **Location**: `web/src/lib/graphql/operations/catalog/`
- Create domain folder structure:
  - `queries.ts` - Search queries
  - `mutations.ts` - Enrollment mutations
  - `index.ts` - Exports

**Queries** (`queries.ts`):
```typescript
import { gql } from '@apollo/client';
import { CATALOG_ITEM_FIELDS } from '../../fragments/catalog';

export const SEARCH_CATALOG_ITEMS = gql`
  ${CATALOG_ITEM_FIELDS}
  query SearchCatalogItems($query: String!) {
    searchCatalogItems(query: $query) {
      ...CatalogItemFields
    }
  }
`;

export const GET_CATALOG_ITEM = gql`
  ${CATALOG_ITEM_FIELDS}
  query GetCatalogItem($id: ID!) {
    catalogItem(id: $id) {
      ...CatalogItemFields
    }
  }
`;
```

**Mutations** (`mutations.ts`):
```typescript
import { gql } from '@apollo/client';
import { CATALOG_ITEM_FIELDS } from '../../fragments/catalog';

export const ENROLL_CATALOG_ITEM = gql`
  ${CATALOG_ITEM_FIELDS}
  mutation EnrollCatalogItem($input: CatalogItemInput!) {
    enrollCatalogItem(input: $input) {
      ...CatalogItemFields
    }
  }
`;
```

#### 3.2 Create Fragments
- **Location**: `web/src/lib/graphql/fragments/catalog.ts`
- Create `CATALOG_ITEM_FIELDS` fragment with all catalog item fields

#### 3.3 Generate Types
- Run code generation:
  ```bash
  cd web
  pnpm codegen
  ```
- Follow patterns from `spec/web/web-graphql-operations.md`

### Phase 4: Frontend Components

#### 4.1 Create Domain Hook
- **Location**: `web/src/lib/hooks/catalog/`
- Create `useCatalog.ts` hook:
  - Use generated hooks from GraphQL operations
  - Implement search functionality
  - Implement enrollment functionality
  - Handle loading and error states
  - Follow patterns from `spec/web/web-custom-hooks.md`

#### 4.2 Create Search Component
- **Location**: `web/src/app/(neotool)/catalog/components/`
- Create `CatalogSearch.tsx`:
  - Use Material-UI components from design system
  - Implement search input with debouncing
  - Display search results
  - Show "Enroll New Item" option when no results
  - Follow component patterns from `spec/web/web-components.md`
  - Use theme tokens from `spec/web/web-themes.md`
  - Add i18n support following `spec/web/web-i18n-architecture.md`

#### 4.3 Create Enrollment Form Component
- **Location**: `web/src/app/(neotool)/catalog/components/`
- Create `EnrollItemForm.tsx`:
  - Form fields: Name, Category (Material/Service), Price
  - Use form components from design system
  - Validation using Zod (if available) or form validation
  - Submit handler that calls enrollment mutation
  - Success/error feedback
  - Follow form patterns from `spec/web/web-components.md`

#### 4.4 Create Catalog Page
- **Location**: `web/src/app/(neotool)/catalog/page.tsx`
- Integrate search and enrollment components
- Handle routing and state management
- Follow Next.js App Router patterns from `spec/web/web-src-structure.md`

#### 4.5 Add i18n Translations
- **Location**: `web/src/app/(neotool)/catalog/i18n/locales/`
- Create translation files:
  - `en.json` - English translations
  - `pt.json` - Portuguese translations (if needed)
- Include all UI strings from components
- Follow i18n patterns from `spec/web/web-i18n-architecture.md`

### Phase 5: Testing

#### 5.1 Backend Tests
- **Location**: `service/kotlin/app/src/test/kotlin/.../`
- Create integration tests:
  - Test repository queries
  - Test service layer logic
  - Test GraphQL resolvers
  - Use Testcontainers for database tests
  - Follow testing patterns from `spec/service/testing-guidelines.md`

#### 5.2 Frontend Tests
- **Location**: `web/src/app/(neotool)/catalog/__tests__/`
- Create component tests:
  - Test search functionality
  - Test enrollment form
  - Test error handling
  - Use MSW for GraphQL mocking

## Implementation Checklist

### Database Layer
- [ ] Create Flyway migration with `app` schema
- [ ] Create `catalog_items` table with all required fields
- [ ] Add indexes for search performance
- [ ] Create domain model classes
- [ ] Create JPA entity with proper annotations
- [ ] Create repository interface with custom queries

### Backend Service Layer
- [ ] Create service class with business logic
- [ ] Create GraphQL schema with types and operations
- [ ] Create GraphQL resolvers (queries and mutations)
- [ ] Update GraphQL factory for Federation (if needed)
- [ ] Sync schema to contracts directory
- [ ] Generate supergraph schema

### Frontend GraphQL Layer
- [ ] Create GraphQL operations (queries and mutations)
- [ ] Create reusable fragments
- [ ] Run code generation
- [ ] Verify generated types

### Frontend Components
- [ ] Create domain hook for catalog operations
- [ ] Create search component
- [ ] Create enrollment form component
- [ ] Create catalog page
- [ ] Add i18n translations
- [ ] Apply theme tokens and design system

### Testing
- [ ] Backend integration tests
- [ ] Frontend component tests
- [ ] E2E tests (optional but recommended)

## Critical Requirements

1. **Database Schema**: ⚠️ ALL tables MUST be in `app` schema, NEVER in `public`
2. **Type Safety**: Maintain end-to-end type safety from database to UI
3. **GraphQL Federation**: Use `@key` directive for entities
4. **Component System**: Use components from design system, not custom implementations
5. **i18n**: All UI strings must be internationalized
6. **Error Handling**: Proper error handling at all layers
7. **Validation**: Input validation at API boundaries
8. **Testing**: Comprehensive test coverage

## Feature Scenarios to Implement

### Scenario 1: Successfully search and find an existing item
- Given: Catalog contains "USB-C Cable - 1m"
- When: User searches for "USB-C Cable"
- Then: Results display "USB-C Cable - 1m" with availability status

### Scenario 2: Search for an item that is not yet enrolled
- Given: Catalog does not contain "Customized Team T-Shirt"
- When: User searches for "Team T-Shirt"
- Then: Results are empty and "Enroll New Item" option is displayed

### Scenario 3: Successfully enroll a new service or material
- Given: User is on "Enroll New Item" form
- When: User provides Name, Category, and Price, then submits
- Then: System confirms enrollment and item has appropriate status

## Additional Notes

- Follow all patterns from the specification documents
- Maintain consistency with existing codebase
- Use established naming conventions
- Ensure proper error handling and user feedback
- Consider accessibility requirements
- Optimize for performance (search indexing, query optimization)

## Success Criteria

The implementation is complete when:
1. All three feature scenarios pass
2. Database schema follows all rules (app schema, not public)
3. GraphQL schema is synced and supergraph is generated
4. Frontend components use design system and themes
5. All UI strings are internationalized
6. Tests pass for all layers
7. Code follows all patterns from specification

