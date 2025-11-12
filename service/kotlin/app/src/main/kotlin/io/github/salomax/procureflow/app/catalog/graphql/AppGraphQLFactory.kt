package io.github.salomax.procureflow.app.catalog.graphql

import graphql.GraphQL
import org.slf4j.LoggerFactory
import com.apollographql.federation.graphqljava.Federation
import graphql.analysis.MaxQueryComplexityInstrumentation
import graphql.analysis.MaxQueryDepthInstrumentation
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeDefinitionRegistry
import io.github.salomax.procureflow.app.catalog.domain.CatalogItem
import io.github.salomax.procureflow.app.catalog.repository.CatalogItemRepository
import io.github.salomax.procureflow.common.exception.GraphQLOptimisticLockExceptionHandler
import io.github.salomax.procureflow.common.metrics.GraphQLMetricsInstrumentation
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import java.util.UUID

@Factory
class AppGraphQLFactory(
    private val appWiringFactory: AppWiringFactory,
    private val catalogItemRepository: CatalogItemRepository,
    private val registry: TypeDefinitionRegistry,
    private val metricsInstrumentation: GraphQLMetricsInstrumentation
) {
    
    @Singleton
    fun graphQL(): GraphQL {
        val runtimeWiring = appWiringFactory.build()
        val federatedSchema = buildFederatedSchema(runtimeWiring)
        return buildGraphQLInstance(federatedSchema)
    }
    
    private fun buildFederatedSchema(runtimeWiring: RuntimeWiring): graphql.schema.GraphQLSchema {
        return Federation.transform(registry, runtimeWiring)
            .fetchEntities { env ->
                val reps = env.getArgument<List<Map<String, Any>>>("representations")
                reps?.mapNotNull { rep -> fetchEntity(rep) }
            }
            .resolveEntityType { env ->
                val entity = env.getObject<Any?>()
                val schema = env.schema ?: throw IllegalStateException("GraphQL schema is null in resolveEntityType")
                resolveEntityType(entity, schema)
            }
            .build()
    }
    
    private fun fetchEntity(rep: Map<String, Any>): CatalogItem? {
        val id = rep["id"] ?: return null
        
        return try {
            when (rep["__typename"]) {
                "CatalogItem" -> {
                    val entity = catalogItemRepository.findById(UUID.fromString(id.toString())).orElse(null)
                    entity?.toDomain()
                }
                else -> null
            }
        } catch (e: Exception) {
            val logger = LoggerFactory.getLogger(AppGraphQLFactory::class.java)
            logger.debug("Failed to fetch entity for federation: {} with id: {}", rep["__typename"], id, e)
            null
        }
    }
    
    private fun resolveEntityType(entity: Any?, schema: graphql.schema.GraphQLSchema): graphql.schema.GraphQLObjectType? {
        return when (entity) {
            is CatalogItem -> schema.getObjectType("CatalogItem")
                ?: throw IllegalStateException("CatalogItem type not found in schema")
            is io.github.salomax.procureflow.app.checkout.domain.CheckoutLog -> schema.getObjectType("CheckoutLog")
                ?: throw IllegalStateException("CheckoutLog type not found in schema")
            else -> null // Let other modules handle their entities
        }
    }
    
    private fun buildGraphQLInstance(schema: graphql.schema.GraphQLSchema): GraphQL {
        return GraphQL.newGraphQL(schema)
            .instrumentation(MaxQueryComplexityInstrumentation(100))
            .instrumentation(MaxQueryDepthInstrumentation(10))
            .instrumentation(metricsInstrumentation)
            .defaultDataFetcherExceptionHandler(GraphQLOptimisticLockExceptionHandler())
            .build()
    }
}
