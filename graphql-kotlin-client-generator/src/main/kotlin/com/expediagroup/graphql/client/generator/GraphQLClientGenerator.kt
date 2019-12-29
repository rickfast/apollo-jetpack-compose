package com.expediagroup.graphql.client.generator

import com.google.common.base.CaseFormat
import com.google.common.base.Converter
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import graphql.language.*
import graphql.parser.Parser
import graphql.schema.idl.TypeDefinitionRegistry
import java.io.File

data class Context(
    val graphQLSchema: TypeDefinitionRegistry,
    val `package`: String,
    val enclosingTypeName: String,
    val dataClasses: MutableMap<String, TypeSpec.Builder> = mutableMapOf()
)

val parser: Parser = Parser()
val upperCamelToLowerCamel: Converter<String, String> = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL)
val upperCamelToUpperUnderscore: Converter<String, String> = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.UPPER_UNDERSCORE)

fun String.toLowerCamel() = upperCamelToLowerCamel.convert(this)!!
fun String.toUpperUnderscore() = upperCamelToUpperUnderscore.convert(this)!!
fun TypeSpec.Builder.setCommentFromGraphQL(node: AbstractNode<*>): TypeSpec.Builder {
    val graphQLComments = node.comments.joinToString("\n") { it.content }

    if (graphQLComments.isNotEmpty()) {
        addKdoc(graphQLComments)
    }

    return this
}


const val LIBRARY_PACKAGE = "com.expediagroup.graphql.client.core"

fun generate(`package`: String, graphQLSchema: TypeDefinitionRegistry, queryFile: File): FileSpec {
    val functionSpecs = mutableListOf<FunSpec>()
    val rootType = graphQLSchema.getType("Query").get() as ObjectTypeDefinition
    val queryString = queryFile.readText()
    val queryDocument = parser.parseDocument(queryString)
    val fileSpec = FileSpec.builder(`package`, queryFile.nameWithoutExtension)
    val name = queryDocument.getDefinitionsOfType(OperationDefinition::class.java).first().name
    val enclosingTypeSpec = TypeSpec.classBuilder(name)
    val context = Context(graphQLSchema, `package`, name)
    val queryConstName = "${context.enclosingTypeName.toUpperUnderscore()}_QUERY"

    queryDocument.getDefinitionsOfType(OperationDefinition::class.java).forEach {
        val className = it.selectionSet.updateType(context, rootType, overrideName = "${it.name}Result")
        val variableType = it.variableDefinitions.createVariableType(context)
        val funSpec = FunSpec.builder(it.name.toLowerCamel())
            .returns(ClassName(LIBRARY_PACKAGE, "GraphQLResult").parameterizedBy(className))
            .addModifiers(KModifier.SUSPEND)
        val variableCode = if (variableType != null) {
            funSpec.addParameter("variables", ClassName(context.`package`, "${context.enclosingTypeName}.Variables"))
            enclosingTypeSpec.addType(variableType)
            "variables"
        } else {
            "mapOf<String, Any>()"
        }

        funSpec.addStatement("return graphQLClient.executeOperation($queryConstName, $variableCode, mapOf<String, Any>(), %T::class.java)", className)

        functionSpecs += funSpec.build()
    }

    fileSpec.addTypeAlias(TypeAliasSpec.builder("ID", STRING).build())

    context.dataClasses.values.forEach {
        val constructorBuilder = FunSpec.constructorBuilder()

        it.propertySpecs.forEach { propertySpec ->
            constructorBuilder.addParameter(propertySpec.name, propertySpec.type)
        }

        it.primaryConstructor(constructorBuilder.build())
        enclosingTypeSpec.addType(it.build())
    }

    enclosingTypeSpec.primaryConstructor(FunSpec.constructorBuilder()
        .addParameter("graphQLClient", ClassName(LIBRARY_PACKAGE, "GraphQLClient"))
        .build())
    enclosingTypeSpec.addProperty(PropertySpec.builder("graphQLClient", ClassName(LIBRARY_PACKAGE, "GraphQLClient"), KModifier.PRIVATE)
        .initializer("graphQLClient").build())
    enclosingTypeSpec.addFunctions(functionSpecs)

    fileSpec.addProperty(PropertySpec.builder(queryConstName, STRING)
        .addModifiers(KModifier.CONST)
        .initializer("%S", queryString).build())
    fileSpec.addType(enclosingTypeSpec.build())

    return fileSpec.build()
}

private fun List<VariableDefinition>?.createVariableType(context: Context): TypeSpec? {
    val variableTypeSpec = TypeSpec.classBuilder("Variables")
        .addModifiers(KModifier.DATA)
    val constructorSpec = FunSpec.constructorBuilder()

    this?.forEach { variableDef ->
        val kotlinType = variableDef.type.toTypeName(context)

        constructorSpec.addParameter(variableDef.name, kotlinType)
        variableTypeSpec.addProperty(PropertySpec.builder(variableDef.name, kotlinType)
            .initializer(variableDef.name).build())
    }

    variableTypeSpec.primaryConstructor(constructorSpec.build())

    return if (variableTypeSpec.propertySpecs.isEmpty()) null else variableTypeSpec.build()
}

private fun ObjectTypeDefinition.updateInputType(context: Context): TypeName {
    val o = context.dataClasses.computeIfAbsent(this.name) {
        TypeSpec.classBuilder(this.name)
    }

    o.setCommentFromGraphQL(this)

    this.fieldDefinitions.forEach { fieldDefinition ->
        val kotlinFieldType = fieldDefinition.type.toTypeName(context)
        val fieldName = fieldDefinition.name

        o.modifiers.add(KModifier.DATA)
        o.addProperty(
            PropertySpec.builder(fieldName, kotlinFieldType)
                .initializer(fieldName)
                .build()
        )
    }

    return ClassName(context.`package`, "${context.enclosingTypeName}.$name")
}

private fun NamedNode<*>.toCustom(context: Context): TypeName =
    when (val type = context.graphQLSchema.getType(this.name).get()) {
        is ObjectTypeDefinition -> type.updateInputType(context)
        is EnumTypeDefinition -> updateEnum(context, type)
        else -> throw RuntimeException()
    }


private fun Type<*>.toTypeName(context: Context): TypeName {
    val nullAgnostic = if (this is NonNullType) {
        this.type
    } else {
        this
    }
    val nullable = this !is NonNullType

    return when (nullAgnostic) {
        is NamedNode<*> -> when (nullAgnostic.name) {
            "String" -> STRING
            "Int" -> INT
            "Float" -> FLOAT
            "Boolean" -> BOOLEAN
            "ID" -> ClassName(context.`package`, "ID")
            else -> nullAgnostic.toCustom(context)
        }
            is ListType -> LIST.parameterizedBy(nullAgnostic.type.toTypeName(context))
        else -> throw java.lang.RuntimeException(this.toString())
    }.copy(nullable = nullable)
}

private fun updateEnum(context: Context, enclosingEnumDef: EnumTypeDefinition): TypeName {
    val o = context.dataClasses.computeIfAbsent(enclosingEnumDef.name) {
        TypeSpec.enumBuilder(it)
    }

    o.addEnumConstant("_UNKNOWN_VALUE")
    o.setCommentFromGraphQL(enclosingEnumDef)

    enclosingEnumDef.enumValueDefinitions.forEach {
        o.addEnumConstant(it.name)
    }

    return ClassName(context.`package`, "${context.enclosingTypeName}.${enclosingEnumDef.name}")
}

private fun SelectionSet?.updateType(
    context: Context,
    enclosingTypeDef: ObjectTypeDefinition,
    overrideName: String? = null
): ClassName {
    val name = overrideName ?: enclosingTypeDef.name
    println("Processing enclosing type: $name")
    val o = context.dataClasses.computeIfAbsent(name) {
        TypeSpec.classBuilder(name)
    }

    this?.getSelectionsOfType(Field::class.java)?.forEach { fieldSelection ->
        val (correspondingFieldDef, nullable, kotlinFieldType) = getFieldData(context, enclosingTypeDef, fieldSelection)
        val fieldName = fieldSelection.alias ?: correspondingFieldDef.name

        o.modifiers.add(KModifier.DATA)
        o.addProperty(
            PropertySpec.builder(fieldName, kotlinFieldType.copy(nullable = nullable))
                .initializer(fieldName)
                .build()
        )
    }

    return ClassName(context.`package`, "${context.enclosingTypeName}.$name")
}

private fun getFieldData(
    context: Context,
    enclosingTypeDef: ObjectTypeDefinition,
    fieldSelection: Field
): Triple<FieldDefinition, Boolean, TypeName> {
    val correspondingFieldDef = enclosingTypeDef.fieldDefinitions.find { it.name == fieldSelection.name }!!
    val nullable = correspondingFieldDef.type !is NonNullType
    val correspondingFieldType = if (nullable) {
        correspondingFieldDef.type
    } else {
        (correspondingFieldDef.type as NonNullType).type
    }
    val kotlinFieldType = correspondingFieldType.toTypeName(context, fieldSelection.selectionSet)

    return Triple(correspondingFieldDef, nullable, kotlinFieldType)
}

private fun NamedNode<*>.toCustom(context: Context, selectionSet: SelectionSet?): TypeName =
    when (val type = context.graphQLSchema.getType(this.name).get()) {
        is ObjectTypeDefinition -> selectionSet.updateType(context, type)
        is EnumTypeDefinition -> updateEnum(context, type)
        else -> throw RuntimeException()
    }

private fun Type<*>.toTypeName(context: Context, selectionSet: SelectionSet?): TypeName {
    val nullAgnostic = if (this is NonNullType) {
        this.type
    } else {
        this
    }

    return when (nullAgnostic) {
        is NamedNode<*> -> when (nullAgnostic.name) {
            "String" -> STRING
            "Int" -> INT
            "Float" -> FLOAT
            "Boolean" -> BOOLEAN
            "ID" -> ClassName(context.`package`, "ID")
            else -> nullAgnostic.toCustom(context, selectionSet)
        }
        is ListType -> LIST.parameterizedBy(nullAgnostic.type.toTypeName(context, selectionSet))
        else -> throw java.lang.RuntimeException(this.toString())
    }
}