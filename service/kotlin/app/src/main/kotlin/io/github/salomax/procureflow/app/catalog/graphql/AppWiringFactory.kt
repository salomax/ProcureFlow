package io.github.salomax.procureflow.app.catalog.graphql

import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring
import io.github.salomax.procureflow.app.catalog.domain.CatalogItem
import io.github.salomax.procureflow.app.catalog.graphql.resolvers.CatalogItemResolver
import io.github.salomax.procureflow.app.checkout.domain.CheckoutLog
import io.github.salomax.procureflow.app.checkout.graphql.resolvers.CheckoutResolver
import io.github.salomax.procureflow.common.graphql.GraphQLArgumentUtils.createValidatedDataFetcher
import io.github.salomax.procureflow.common.graphql.GraphQLWiringFactory
import io.github.salomax.procureflow.common.graphql.GraphQLResolverRegistry
import jakarta.inject.Singleton

/**
 * App module wiring factory for GraphQL resolvers
 */
@Singleton
class AppWiringFactory(
    private val catalogItemResolver: CatalogItemResolver,
    private val checkoutResolver: CheckoutResolver,
    resolverRegistry: GraphQLResolverRegistry
) : GraphQLWiringFactory() {
    
    init {
        // Register resolvers in the registry for cross-module access
        resolverRegistry.register("catalog", catalogItemResolver)
    }
    
    public override fun registerQueryResolvers(type: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder {
        return type
            .dataFetcher("searchCatalogItems", createValidatedDataFetcher(emptyList()) { env ->
                // Query is required by GraphQL schema (String!), but empty strings are valid (returns all items)
                val query = env.getArgument<String>("query") ?: throw IllegalArgumentException("query is required")
                catalogItemResolver.searchCatalogItems(query)
            })
            .dataFetcher("catalogItem", createValidatedDataFetcher(listOf("id")) { env ->
                val id = env.getArgument<String>("id") ?: throw IllegalArgumentException("id is required")
                catalogItemResolver.catalogItem(id)
            })
    }
    
    public override fun registerMutationResolvers(type: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder {
        return type
            .dataFetcher("saveCatalogItem", createValidatedDataFetcher(listOf("input")) { env ->
                val input = env.getArgument<Map<String, Any?>>("input") ?: throw IllegalArgumentException("input is required")
                catalogItemResolver.saveCatalogItem(input)
            })
            .dataFetcher("checkout", createValidatedDataFetcher(listOf("input")) { env ->
                val input = env.getArgument<Map<String, Any?>>("input") ?: throw IllegalArgumentException("input is required")
                checkoutResolver.checkout(input)
            })
    }
    
    public override fun registerSubscriptionResolvers(type: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder {
        return type
    }

    public override fun registerCustomTypeResolvers(builder: RuntimeWiring.Builder): RuntimeWiring.Builder {
        return builder
            .type("CatalogItem") { type ->
                type.dataFetcher("id", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val item = env.getSource<CatalogItem>()
                    item?.id?.toString()
                })
                type.dataFetcher("name", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val item = env.getSource<CatalogItem>()
                    item?.name
                })
                type.dataFetcher("category", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val item = env.getSource<CatalogItem>()
                    item?.category?.name
                })
                type.dataFetcher("priceCents", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val item = env.getSource<CatalogItem>()
                    item?.priceCents
                })
                type.dataFetcher("status", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val item = env.getSource<CatalogItem>()
                    item?.status?.name
                })
                type.dataFetcher("createdAt", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val item = env.getSource<CatalogItem>()
                    item?.createdAt?.toString()
                })
                type.dataFetcher("updatedAt", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val item = env.getSource<CatalogItem>()
                    item?.updatedAt?.toString()
                })
                type.dataFetcher("description", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val item = env.getSource<CatalogItem>()
                    item?.description
                })
            }
            .type("CheckoutLog") { type ->
                type.dataFetcher("id", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val log = env.getSource<CheckoutLog>()
                    log?.id?.toString()
                })
                type.dataFetcher("userId", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val log = env.getSource<CheckoutLog>()
                    log?.userId?.toString()
                })
                type.dataFetcher("items", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val log = env.getSource<CheckoutLog>()
                    log?.items
                })
                type.dataFetcher("totalPriceCents", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val log = env.getSource<CheckoutLog>()
                    log?.totalPriceCents
                })
                type.dataFetcher("itemCount", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val log = env.getSource<CheckoutLog>()
                    log?.itemCount
                })
                type.dataFetcher("status", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val log = env.getSource<CheckoutLog>()
                    log?.status?.name
                })
                type.dataFetcher("createdAt", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val log = env.getSource<CheckoutLog>()
                    log?.createdAt?.toString()
                })
            }
            .type("CheckoutItem") { type ->
                type.dataFetcher("catalogItemId", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val item = env.getSource<io.github.salomax.procureflow.app.checkout.domain.CheckoutItem>()
                    item?.catalogItemId
                })
                type.dataFetcher("name", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val item = env.getSource<io.github.salomax.procureflow.app.checkout.domain.CheckoutItem>()
                    item?.name
                })
                type.dataFetcher("priceCents", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val item = env.getSource<io.github.salomax.procureflow.app.checkout.domain.CheckoutItem>()
                    item?.priceCents
                })
                type.dataFetcher("quantity", createValidatedDataFetcher { env: DataFetchingEnvironment ->
                    val item = env.getSource<io.github.salomax.procureflow.app.checkout.domain.CheckoutItem>()
                    item?.quantity
                })
            }
    }
}

