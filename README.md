# ProcedureFlow

A comprehensive solution demonstrating how AI agents make workflows way better.

# How to 



# Architecture

The project is broken down into clean layers: the client layer (Next.js web app) talks to the API gateway layer (Apollo Router doing GraphQL Federation), which routes requests to the service layer (Kotlin/Micronaut microservices with resolvers, business logic, and data access), and everything eventually hits the data layer (PostgreSQL). Each layer has a clear job and they're all containerized so you can scale and deploy them independently.

![Architecture Diagram](docs/etc/images/image.png)

## Neotool

[Neotool](https://github.com/salomax/neotool) is a modular full-stack boilerplate framework that accelerates development by providing a pre-configured, production-ready foundation with clean architecture patterns—including GraphQL Federation, type-safe end-to-end code generation, a complete design system, and containerized infrastructure—all wired together so teams can focus on building features instead of setting up the stack.

Neotool UI layer includes a comprehensive design system with Material-UI components, a design token-based theme system with light/dark modes, domain-based internationalization (i18n), type-safe GraphQL operations with code generation, custom React hooks for business logic, comprehensive testing setup (Vitest, React Testing Library, Playwright), and Storybook for component documentation.

As IDE with AI-code editor we've been testing with Cursor. But it might be applied to similar ones.


### Neotool Spec-Driven Development

[Neotool](https://github.com/salomax/neotool) uses Spec-Driven Development, meaning the spec in the spec/ folder is the single source of truth for all architecture and patterns. Unlike typical development where knowledge is scattered, Neotool's spec is highly structured and optimized for AI. It uses metadata and tags to make sure AI assistants like Cursor can easily understand the project's context and instantly follow established architectural rules.

The main benefit is speed and consistency. Developers just describe a feature naturally, and the AI generates production-ready code that already complies with complex patterns like GraphQL Federation and Clean Architecture. This cuts down implementation time significantly, eliminates manual guesswork, and keeps the entire codebase consistently following the same high-quality standards.

#### Step 1 - Generate Feature File

The first step in this workflow is creating a **Gherkin feature file** (`.feature`) that describes the feature in a structured, human-readable format using the Given-When-Then syntax.

This file serves as an specification that defines user stories, acceptance criteria, and test scenarios—covering happy paths, error cases, and edge cases—all in plain language that both humans and AI can understand.

Here, we can review the feature flow by the user perspective and adjust it if needed.

**Example prompt to Cursor:**

```
Create a Gherkin feature file for a new feature: "User Profile Management"

The feature should allow users to:
- View their profile information (name, email, role, department)
- Update their profile information
- Change their password
- Upload a profile picture
```

#### Step 2 - Generate Implementation Prompt

Once the feature file is created, the next step is requesting Cursor to generate a comprehensive **implementation prompt** that breaks down the entire feature into a detailed implementation plan. 

This prompt serves as a blueprint that lists all artifacts that will be generated across every layer of the stack, migration, domain models, JPA entities, repositories, services, GraphQL schemas and resolvers, frontend operations, hooks, components, pages, and tests—all organized by implementation phases and cross-referenced with the Neotool specification.

This is crutial because we review if everything is placed as expected.

**Example prompt to Cursor:**

```
Based on feature file at docs/features/user/profile-management.feature and generate a comprehensive implementation prompt that details all artifacts that need to be created to implement this feature according to the Neotool specification.
```

#### Step 3 - Plan and Build

With the implementation prompt ready, this step uses Cursor's **Plan** mode (not Agent mode) to execute the implementation. 

Plan mode works by breaking down the implementation prompt into discrete, sequential steps and executing them one by one, creating all the artifacts systematically across each phase.

**Example prompt to Cursor:**

```
Using Plan mode, implement the feature according to docs/features/user/implementation-prompt.md
```

#### Step 4 - Manual Review & Test

After Cursor generates all the artifacts using Plan mode, this step involves manually reviewing the generated code and running tests to validate the implementation. 

This is a critical quality assurance step that ensures the generated code meets requirements, follows best practices, and integrates correctly with the existing codebase.


# Use Cases

Use Case: Search and Enroll Materials and Services

Goal: Provide a sleek, AI-driven experience that lets purchasing teams work way faster and smarter.

## Features

Feature 1: Search and Enroll

Feature 2: Cart and Checkout

Feature 3: Search and Enroll by natural language


# Implementation

## Step 1 - Create feature files

Initially, it's been created the [search-and-enroll.feature](docs/features/catalog/search-and-enroll.feature) file, transcribing the text below into the Gherkin format:

```plaintext
Search & Register
Users can search for a material/service in a catalog by name or keyword.
If not found, they can register a new item (name, category, description, price, status
```
## Step 2 - Create the prompt to cursor

- Request to Cursor to provide the prompts to implement [search-and-enroll.feature](docs/features/catalog/search-and-enroll.feature) according to [Neotool specs](spec/)

This step had as output 2 documents:
- [implementation-prompt.md](docs/features/catalog/implementation-prompt.md) (rules for implementation)
- [QUICK_START.md](docs/features/catalog/QUICK_START.md) (sort of guideline for Cursor)

## Step 3 - Plan and build the feature

Request via prompt to Cursor to build the feature according to the files generated above (rules and guideline).

Cursor had generated the following plan to build the solution.

## Step 4 - Manual review and tests (backend)

According to the Neotool specs, Cursor was able to generate the artifacts to implement the feature.

In order to validate it, the first step was to spin up Postgres and its connection pool (Pgbouncer):

```shell
docker-compose -f infra/docker/docker-compose.local.yml up -d postgres pgbouncer
```

As expected, the database was available via port 6432, username and password as procureflow, and database procureflow_db.

During the review, some artifacts had been adjusted:
- Service integration tests
- GraphQL instrumentation components
- Supergraph with wrong address for local

After the fixes have been applied to code, we could started the router:

```shell
docker-compose -f infra/docker/docker-compose.local.yml up -d router
```

Search catalog test:

```shell
curl --request POST \
  --url http://localhost:4000/graphql \
  --header 'content-type: application/json' \
  --data '{"query":"query SearchCatalogItems($query: String!) { searchCatalogItems(query: $query) { id name category priceCents status createdAt updatedAt } }","variables":"{\n  \"query\": \"USB-C Cable\"\n}"}'
```
 
 As result:

 ```
 {
  "data": {
    "searchCatalogItems": []
  }
}
```

GraphQL API is okay!

## Step 5 - Manual review and tests (frontend)

Build the modules (using Node >=20)

```shell
pnpm i
```

Starting the Nextjs on `web/` folder:

```shell
pnpm run dev
```

Access http://localhost:3000/

During the review I ran into some tweaks:
- Alignment
- Missing CatalogItem hooks (useCatalog)

I also realized the description field was missing and I added it later.

After some backs and forths to adjust the UI we were able to run the catalog page at http://localhost:3000/catalog




- Missed integration tests, input dto and some errors on front end





DOCKER_BUILDKIT=1 COMPOSE_DOCKER_CLI_BUILD=1 docker-compose -f infra/docker/docker-compose.yml up -d




# CI/CD

## Test → Build → CR → Preview → Staging → Production 

This model validates code quality before merge (Preview Apps), validates system integration after merge (Staging), and promotes the same artifact digest to Production with gates, rollouts, and fast rollback.

### Goals & Principles

- Shift-left quality: catch issues in PR with automated checks + preview environments.

- Staging as rehearsal: validate real infra, configs, and secrets after merge.

- Promote, don’t rebuild: production uses the exact image digest tested in staging.

- Safe releases: environments, approvals, canaries/blue-green, and automatic rollback.


## Stages & Purpose

### Pull Request CI + Preview App (Ephemeral)

Test, build, lint, typecheck, security scan; deploy a temporary environment per PR

PR is approved only after preview validation (automated + manual)

### Staging (Shared, Persistent)

Upon merge to main, deploy to staging using the built images (by tag/digest).

Automated smoke/E2E tests, validate end-to-end integration (infra/secrets/DB migrations) close to prod.

### Promotion to Production (Tag/Release)

Deterministic deployments, create a semantic tag vX.Y.Z; promote the same digest from staging to prod.

Gate via GitHub Environments (approvals), roll out (canary/blue-green), monitor, and auto-rollback if metrics degrade.

### Post-Release Verification & Rollback

Health checks, SLO/SLA monitors, error-rate guards.

Stable prod with auditable release trail.

## Flowchart

```mermaid
graph TD
  A[Developer opens PR] --> B[PR CI: build/lint/test]
  B --> C{Build OK?}
  C -- no --> C1[Fix & push]
  C1 --> B
  C -- yes --> D[Deploy Preview App]
  D --> E[Validation on Preview App]
  E --> F{Approve PR?}
  F -- no --> C1
  F -- yes --> G[Merge to main]
  G --> H[Deploy to Staging using same images]
  H --> I[Run DB migrations along first execution]
  I --> J[Smoke + E2E + Contract tests + Observability checks]
  J --> K{All checks green?}
  K -- no --> H1[Fix → new PR cycle]
  K -- yes --> L[Create Tag with GitHub Release]
  L --> M[Deploy to Production with Canary/Blue-Green]
  M --> N[Post-release health checks & SLO guardrails]
  N --> O{Healthy?}
  O -- no --> P[Auto-rollback to previous digest]
  O -- yes --> Q[Complete Release & Notify]
```