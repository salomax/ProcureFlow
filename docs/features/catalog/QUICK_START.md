# Quick Start: Search and Enroll Feature Implementation

## For Cursor Planner

Use this prompt to implement the full-stack Search and Enroll feature:

```
@docs/features/catalog/implementation-prompt.md
```

## Key Files to Reference

### Feature Specification
- `docs/features/catalog/search-and-enroll.feature` - Gherkin feature file

### Specification Documents
- `.cursorrules` - Project rules and conventions
- `spec/ARCHITECTURE_OVERVIEW.md` - System architecture
- `spec/service/database-schema-organization.md` - Database rules (⚠️ CRITICAL: Use `app` schema, NOT `public`)
- `spec/service/kotlin/jpa-entity.md` - JPA entity patterns
- `spec/service/graphql-federation-architecture.md` - GraphQL patterns
- `spec/web/web-src-structure.md` - Frontend structure
- `spec/web/web-components.md` - Component patterns
- `spec/web/web-graphql-operations.md` - GraphQL operations patterns

## Quick Implementation Checklist

### 1. Database (Backend)
- [ ] Create migration: `V1_X__create_catalog_items.sql` in `app` schema
- [ ] Create `CatalogItemEntity` with `@Table(schema = "app")`
- [ ] Create `CatalogItemRepository` with search methods

### 2. GraphQL (Backend)
- [ ] Add schema to `service/kotlin/app/src/main/resources/graphql/schema.graphqls`
- [ ] Create `CatalogItemResolver` with search and enroll operations
- [ ] Run `./neotool graphql sync` and `./neotool graphql generate`

### 3. Frontend GraphQL
- [ ] Create operations in `web/src/lib/graphql/operations/catalog/`
- [ ] Run `pnpm codegen` in `web/` directory

### 4. Frontend Components
- [ ] Create `useCatalog` hook in `web/src/lib/hooks/catalog/`
- [ ] Create search component
- [ ] Create enrollment form
- [ ] Create catalog page
- [ ] Add i18n translations

## Critical Rules

1. ⚠️ **Database Schema**: ALL tables MUST be in `app` schema, NEVER in `public`
2. ✅ **Type Safety**: End-to-end type safety from DB → GraphQL → Frontend
3. ✅ **GraphQL Federation**: Use `@key(fields: "id")` for entities
4. ✅ **Design System**: Use components from `shared/components/ui/`
5. ✅ **i18n**: All UI strings must be internationalized

## Feature Scenarios

1. **Happy Path**: Search finds existing item
2. **Sad Path**: Search returns empty, shows enroll option
3. **Enrollment**: Create new item with Name, Category, Price

## Commands Reference

```bash
# Backend: Sync GraphQL schema
./neotool graphql sync
./neotool graphql generate

# Frontend: Generate types
cd web && pnpm codegen
```

