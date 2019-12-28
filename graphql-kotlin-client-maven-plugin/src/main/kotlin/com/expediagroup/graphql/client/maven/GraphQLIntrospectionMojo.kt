package com.expediagroup.graphql.client.maven

import com.expediagroup.graphql.client.generator.introspectSchema
import graphql.schema.idl.SchemaPrinter
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File
import java.io.FileWriter
import java.net.URL

@Mojo(name = "introspect")
class GraphQLIntrospectionMojo : AbstractMojo() {
    @Parameter(name = "outputDirectory", defaultValue = "\${project.build.directory}")
    private lateinit var outputDirectory: File

    @Parameter(name = "url", required = true)
    private lateinit var url: URL

    override fun execute() {
        val schema = introspectSchema(url)

        if (outputDirectory.exists().not()) {
            outputDirectory.mkdir()
        }

        val writer = FileWriter("${outputDirectory.absolutePath}/schema.graphql")

        writer.write(SchemaPrinter().print(schema))
        writer.flush()
        writer.close()
    }
}