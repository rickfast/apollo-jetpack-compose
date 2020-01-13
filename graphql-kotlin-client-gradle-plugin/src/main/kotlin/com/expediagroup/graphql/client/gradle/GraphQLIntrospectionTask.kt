package com.expediagroup.graphql.client.gradle

import com.expediagroup.graphql.client.generator.introspectSchema
import graphql.schema.idl.SchemaPrinter
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileWriter
import java.net.URL

@Suppress("UnstableApiUsage")
class GraphQLIntrospectionTask(url: String, outputDirectory: File) : DefaultTask() {

    @Input
    val url: Property<String> = project.objects.property(String::class.java)

    @OutputDirectory
    val outputDirectory: DirectoryProperty = project.objects.directoryProperty()

    init {
        this.url.set(url)
        this.outputDirectory.set(outputDirectory)
    }

    @TaskAction
    fun introspect() {
        val schema = introspectSchema(URL(url.get()))

        val writer = FileWriter("${outputDirectory.get().asFile.absolutePath}/schema.graphql")

        writer.write(SchemaPrinter().print(schema))
        writer.flush()
        writer.close()
    }
}