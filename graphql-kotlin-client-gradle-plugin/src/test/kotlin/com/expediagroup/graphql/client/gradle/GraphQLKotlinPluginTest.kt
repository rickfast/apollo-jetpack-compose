package com.expediagroup.graphql.client.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

const val BUILD_FILE_CONTENT = """
plugins {
    id 'com.expediagroup.graphql-kotlin'
}

graphql {
    introspect {
        url = "https://swapi.graph.cool/"
        
    }
    generate {
        package = "blah"
        src = file('query.graphql')
    }
}
"""

const val QUERY_FILE_CONTENT = """
{
  allFilms{
    id
  }
  allPersons{
    name
  }
}
"""

class GraphQLKotlinPluginTest {
    @Rule
    val testProjectDir = TemporaryFolder()
    private var buildFile: File? = null
    private var queryFile: File? = null

    @Before
    @Throws(IOException::class)
    fun setup() {
        buildFile = testProjectDir.newFile("build.gradle")
        buildFile!!.writeText(BUILD_FILE_CONTENT)
        queryFile = testProjectDir.newFile("query.graphql")
        queryFile!!.writeText(QUERY_FILE_CONTENT)
    }

    @Test
    fun `shouldIntrospectAndGenerate`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("compileKotlin")
            .withPluginClasspath()
            .build()

        result
    }
}