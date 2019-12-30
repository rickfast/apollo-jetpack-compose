package com.expediagroup.graphql.client.generator

import com.squareup.kotlinpoet.FileSpec
import graphql.schema.idl.SchemaParser
import java.io.File

fun generateClient(`package`: String, schema: File, vararg files: File): List<FileSpec> {
    val graphQLSchema = SchemaParser().parse(schema)

    return files.map {
        generate(`package`, graphQLSchema, it)
    }
}