package com.expediagroup.graphql.client.gradle

import com.expediagroup.graphql.client.generator.generateClient
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

@Suppress("UnstableApiUsage")
@CacheableTask
class GraphQLClientGenerationTask(
    `package`: String,
    schema: File,
    src: List<File>,
    outputDirectory: File
) : SourceTask() {

    @Input
    val `package`: Property<String> = this.project.objects.property(String::class.java)

    @Input
    val schema: RegularFileProperty = this.project.objects.fileProperty()

    @Input
    val src: ConfigurableFileCollection = this.project.objects.fileCollection()

    @OutputDirectory
    val outputDirectory: DirectoryProperty = this.project.objects.directoryProperty()

    init {
        this.`package`.set(`package`)
        this.schema.set(schema)
        this.src.from(src.map { it.absolutePath })
        this.outputDirectory.set(outputDirectory)
    }

    @TaskAction
    fun generate() {
        val fileSpecs = generateClient(`package`.get(), schema.get().asFile, *src.files.toTypedArray())

        fileSpecs.forEach {
            it.writeTo(outputDirectory.get().asFile)
        }
    }
}