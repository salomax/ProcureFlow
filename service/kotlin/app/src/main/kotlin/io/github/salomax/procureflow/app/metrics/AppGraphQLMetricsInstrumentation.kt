package io.github.salomax.procureflow.app.metrics

import io.github.salomax.procureflow.common.metrics.GraphQLMetricsInstrumentation
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.Singleton

/**
 * App module implementation of GraphQL metrics instrumentation.
 * 
 * Provides metrics tracking for app-specific operations (catalog and checkout).
 */
@Singleton
class AppGraphQLMetricsInstrumentation(
    meterRegistry: MeterRegistry
) : GraphQLMetricsInstrumentation(meterRegistry) {

    override fun getModuleName(): String {
        return "app"
    }
}
