package com.expediagroup.graphql.client.gradle

import java.io.File

class GraphQLExtension {
    var introspection: Introspection? = null
    lateinit var generation: Generation
}

class Introspection {
    lateinit var url: String
    var outputDirectory: File? = null
}

class Generation {
    lateinit var `package`: String
    lateinit var src: List<File>
    var schema: File? = null
    var outputDirectory: File? = null
}