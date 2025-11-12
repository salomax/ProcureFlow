package io.github.salomax.procureflow.security

import io.micronaut.runtime.Micronaut.build

object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        build(*args)
            .packages("io.github.salomax.procureflow.security")
            .start()
    }
}
