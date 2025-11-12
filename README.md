# ProcedureFlow

A comprehensive solution demonstrating how AI agents make workflows way better.


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