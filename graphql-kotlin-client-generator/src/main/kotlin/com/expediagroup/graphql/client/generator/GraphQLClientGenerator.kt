package com.expediagroup.graphql.client.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import graphql.language.*
import graphql.parser.Parser
import graphql.schema.idl.TypeDefinitionRegistry
import java.io.File
import java.io.FileReader

class GraphQLClientGenerator(private val `package`: String,
                             private val graphQLSchema: TypeDefinitionRegistry
) {
    private val parser: Parser = Parser()
    private val topLevel = mutableMapOf<String, TypeSpec.Builder>()

    fun generate(queryFile: File): FileSpec {
        val root = graphQLSchema.getType("Query").get() as ObjectTypeDefinition
        val query = parser.parseDocument(FileReader(queryFile))
        query.getDefinitionsOfType(OperationDefinition::class.java).map { it.selectionSet }

        val fileSpec = FileSpec.builder(`package`, "${queryFile.nameWithoutExtension}.kt")

        query.getDefinitionsOfType(OperationDefinition::class.java).forEach {
            val className = it.selectionSet.updateType(root, overrideName = "${it.name}Result")
            val funSpec = FunSpec.builder(it.name)
                .returns(ClassName("com.expediagroup.graphql.client.core", "GraphQLResult").parameterizedBy(className))
                .addModifiers(KModifier.SUSPEND)
                .addCode("null")
                .build()
            fileSpec.addFunction(funSpec)
        }

        fileSpec.addTypeAlias(TypeAliasSpec.builder("ID", STRING).build())

        topLevel.values.forEach {
            fileSpec.addType(it.build())
        }

        return fileSpec.build()
    }

    fun SelectionSet?.updateQueryInterface(enclosingTypeDef: ObjectTypeDefinition, name: String, variableDefinitions: List<VariableDefinition>): TypeName {
        val i = topLevel.computeIfAbsent(name) {
            TypeSpec.interfaceBuilder(name)
        }

        this?.getSelectionsOfType(Field::class.java)?.forEach { fieldSelection ->
            val (correspondingFieldDef, nullable, kotlinFieldType) = getFieldData(enclosingTypeDef, fieldSelection)

            i.addFunction(
                FunSpec.builder(correspondingFieldDef.name)
                    .addModifiers(KModifier.ABSTRACT)
                    .returns(kotlinFieldType.copy(nullable = nullable)).build())

            if (fieldSelection.alias != null) {
                i.addFunction(
                    FunSpec.builder(fieldSelection.alias)
                        .returns(kotlinFieldType.copy(nullable = nullable))
                        .addCode("this.${correspondingFieldDef.name}")
                        .build())
            }
        }
        return ClassName(`package`, enclosingTypeDef.name)
    }

    private fun updateEnum(enclosingEnumDef: EnumTypeDefinition): TypeName {
        val o = topLevel.computeIfAbsent(enclosingEnumDef.name) {
            TypeSpec.enumBuilder(it)
        }

        o.addEnumConstant("_UNKNOWN_VALUE")

        enclosingEnumDef.enumValueDefinitions.forEach {
            o.addEnumConstant(it.name)
        }

        return ClassName(`package`, enclosingEnumDef.name)
    }

    private fun SelectionSet?.updateType(enclosingTypeDef: ObjectTypeDefinition, overrideName: String? = null): ClassName {
        val name = overrideName ?: enclosingTypeDef.name
        println("Processing enclosing type: $name")
        val o = topLevel.computeIfAbsent(name) {
            TypeSpec.classBuilder(name)
        }

        this?.getSelectionsOfType(Field::class.java)?.forEach { fieldSelection ->
            val (correspondingFieldDef, nullable, kotlinFieldType) = getFieldData(enclosingTypeDef, fieldSelection)

            o.modifiers.add(KModifier.DATA)
            o.primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(correspondingFieldDef.name, kotlinFieldType.copy(nullable = nullable))
                    .build()
            )

            if (fieldSelection.alias != null) {
                o.addProperty(
                    PropertySpec.builder(fieldSelection.alias, kotlinFieldType)
                        .initializer("this.${correspondingFieldDef.name}").build())
            }
        }

        return ClassName(`package`, name)
    }

    private fun getFieldData(
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
        val kotlinFieldType = correspondingFieldType.toTypeName(fieldSelection.selectionSet)

        return Triple(correspondingFieldDef, nullable, kotlinFieldType)
    }

    private fun NamedNode<*>.toCustom(selectionSet: SelectionSet?): TypeName =
        when (val type = graphQLSchema.getType(this.name).get()) {
            is ObjectTypeDefinition -> selectionSet.updateType(type)
            is EnumTypeDefinition -> updateEnum(type)
            else -> throw RuntimeException()
        }

    private fun Type<*>.toTypeName(selectionSet: SelectionSet?): TypeName {
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
                "ID" -> ClassName(`package`, "ID")
                else -> nullAgnostic.toCustom(selectionSet)
            }
            is ListType -> LIST.parameterizedBy(nullAgnostic.type.toTypeName(selectionSet))
            else -> throw java.lang.RuntimeException(this.toString())
        }
    }
}