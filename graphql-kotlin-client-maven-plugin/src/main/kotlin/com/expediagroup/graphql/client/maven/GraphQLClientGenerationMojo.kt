package com.expediagroup.graphql.client.maven

import com.expediagroup.graphql.client.generator.generateClient
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File

@Mojo(name = "generate")
class GraphQLClientGenerationMojo : AbstractMojo() {
    @Parameter(name = "package", required = true)
    private lateinit var `package`: String

    @Parameter(name = "src", required = true)
    private lateinit var src: List<File>

    @Parameter(name = "schema", defaultValue = "\${project.build.directory}/schema.graphql")
    private lateinit var schema: File

    @Parameter(name = "outputDirectory", defaultValue = "\${project.build.directory}")
    private lateinit var outputDirectory: File

    override fun execute() = try {
        val fileSpecs = generateClient(`package`, schema, *src.toTypedArray())

        fileSpecs.forEach {
            it.writeTo(outputDirectory)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}