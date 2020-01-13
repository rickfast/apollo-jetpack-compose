package com.expediagroup.graphql.client.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceTask
import java.io.File

class GraphQLKotlinPlugin : Plugin<Project> {
    override fun apply(project: Project?) {
        val extension = project!!.extensions.create("graphql", GraphQLExtension::class.java)
        val buildDir = project.buildDir

        val generation = extension.generation
        val introspection = extension.introspection

        if (generation.outputDirectory == null) {
            generation.outputDirectory = project.mkdir("${buildDir.absolutePath}/graphql")
        }

        introspection?.let {
            if (it.outputDirectory == null) {
                introspection.outputDirectory = project.mkdir(buildDir.absolutePath)
            }

            if (generation.schema == null) {
                generation.schema = File("${it.outputDirectory}/schema.graphql")
            }
        }

        val generateTask = project.tasks.create("generate", GraphQLClientGenerationTask::class.java,
            generation.`package`,
            generation.schema,
            generation.src,
            generation.outputDirectory)

        project.plugins.withId("java-base") {
            val convention = project.convention.getPlugin(JavaPluginConvention::class.java)

            convention.sourceSets.map {
                val name = if (it.name == "main") {
                    ""
                } else {
                    it.name
                }

                val kotlinTask = project.tasks.findByName("compile${name.capitalize()}Kotlin") as? SourceTask

                kotlinTask?.source(generation.outputDirectory)
                kotlinTask?.dependsOn("generate")
            }
        }

        introspection?.let {
            val introspectTask = project.tasks.create("introspect", GraphQLIntrospectionTask::class.java,
                it.url, it.outputDirectory)

            generateTask.dependsOn(introspectTask.path)
        }
    }
}