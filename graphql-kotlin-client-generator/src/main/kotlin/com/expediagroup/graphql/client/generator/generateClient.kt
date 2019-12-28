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

//fun main(args: Array<String>) {
//    val doc = Parser().parseDocument("query AllFilms {\n  allFilms {\n    buttnugget: id\n  }\n}\n")
//    val doc2 = Parser().parseDocument("query AllPersons {\n  allPersons {\n gender\n }\n}\n")
//
//    val document = Document(arrayOf(doc, doc2).flatMap { it.getDefinitionsOfType(OperationDefinition::class.java) })
//    val graphQLSchema = SchemaParser().parse(File(thread { }.contextClassLoader.getResource("schema.graphql").file))
//    val root = graphQLSchema.getType("Query").get() as ObjectTypeDefinition
//    doc.getDefinitionsOfType(OperationDefinition::class.java).map { it.selectionSet }
//    println(AstPrinter.printAst(document))
//
//    val fileSpec = FileSpec.builder(`package`, "blah.kt")
//
//    document.getDefinitionsOfType(OperationDefinition::class.java).forEach {
//        val className = it.selectionSet.updateType(root, overrideName = "${it.name}Result")
//        val funSpec = FunSpec.builder(it.name)
//            .returns(ClassName("com.expediagroup.graphql.client.core", "GraphQLResult").parameterizedBy(className))
//            .addModifiers(KModifier.SUSPEND)
//            .addCode("null")
//            .build()
//        fileSpec.addFunction(funSpec)
//    }
//
//
//    fileSpec.addTypeAlias(TypeAliasSpec.builder("ID", STRING).build())
//
//    topLevel.values.forEach {
//        fileSpec.addType(it.build())
//    }
//
//    fileSpec.build().writeTo(System.out)
//}


