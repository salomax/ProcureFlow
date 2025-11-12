import org.gradle.process.CommandLineArgumentProvider

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.20" apply false
    id("io.micronaut.application") version "4.5.4" apply false
    id("io.micronaut.aot") version "4.5.4" apply false
    id("io.micronaut.library") version "4.5.4" apply false
    id("org.jetbrains.kotlin.plugin.jpa") version "2.2.20" apply false
    id("com.google.devtools.ksp") version "2.2.20-2.0.3" apply false
    id("com.gradleup.shadow") version "8.3.7" apply false
}

allprojects {
    group = "io.github.salomax.procureflow"

    repositories { 
        mavenCentral()
        google()
    }

    plugins.withId("io.micronaut.application") {
      the<io.micronaut.gradle.MicronautExtension>().processing {
        incremental(true)
        annotations("io.github.salomax.procureflow.*")
      }
    }

    plugins.withId("io.micronaut.library") {
      the<io.micronaut.gradle.MicronautExtension>().processing {
        incremental(true)
        annotations("io.github.salomax.procureflow.*")
      }
    }

  // Configure Kotlin compilation
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    
    // Configure Java compilation
    tasks.withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    
    // Configure test tasks
    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
        
        // Configure Mockito agent to avoid self-attachment warning
        // This is required for future JDK versions where self-attachment will be disallowed
        // Using jvmArgumentProviders ensures the configuration is resolved lazily at execution time
        // Access configuration lazily at execution time to avoid configuration phase issues
        jvmArgumentProviders.add(object : CommandLineArgumentProvider {
            override fun asArguments(): Iterable<String> {
                val testRuntimeClasspathConfig = project.configurations.findByName("testRuntimeClasspath")
                if (testRuntimeClasspathConfig == null) {
                    return emptyList()
                }
                
                val mockitoInline = try {
                    testRuntimeClasspathConfig
                        .resolvedConfiguration
                        .resolvedArtifacts
                        .find { it.moduleVersion.id.name == "mockito-inline" }
                        ?.file
                } catch (e: Exception) {
                    null
                }
                
                return if (mockitoInline != null && mockitoInline.exists()) {
                    listOf("-javaagent:${mockitoInline.absolutePath}")
                } else {
                    emptyList()
                }
            }
        })
    }
}
