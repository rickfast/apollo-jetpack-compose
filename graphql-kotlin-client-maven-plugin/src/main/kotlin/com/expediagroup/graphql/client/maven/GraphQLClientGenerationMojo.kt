package com.expediagroup.graphql.client.maven

import com.expediagroup.graphql.client.generator.generateClient
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

@Mojo(name = "generate")
class GraphQLClientGenerationMojo : AbstractMojo() {
    @Parameter(name = "package", required = true)
    private lateinit var `package`: String

    @Parameter(name = "src", required = true)
    private lateinit var src: List<File>

    @Parameter(name = "schema", required = true)
    private lateinit var schema: File

    @Parameter(name = "project", defaultValue = "\${project}")
    private lateinit var project: MavenProject

    @Parameter(name = "outDir")
    private var outDir: File? = null

    override fun execute() {
        val outputDirectory = outDir ?: File(project.build.outputDirectory)
        val fileSpecs = generateClient(`package`, schema, *src.toTypedArray())

        fileSpecs.forEach {
            it.writeTo(outputDirectory)
        }
    }
}