package com.expediagroup.graphql.client.generator

import com.google.gson.Gson
import graphql.introspection.IntrospectionResultToSchema
import graphql.language.Document
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL

private val gson = Gson()
private val introspectionResultToSchema = IntrospectionResultToSchema()

const val payload = """{"operationName":"IntrospectionQuery","variables":{},"query":"query IntrospectionQuery {\n  __schema {\n    queryType {\n      name\n    }\n    mutationType {\n      name\n    }\n    subscriptionType {\n      name\n    }\n    types {\n      ...FullType\n    }\n    directives {\n      name\n      description\n      locations\n      args {\n        ...InputValue\n      }\n    }\n  }\n}\n\nfragment FullType on __Type {\n  kind\n  name\n  description\n  fields(includeDeprecated: true) {\n    name\n    description\n    args {\n      ...InputValue\n    }\n    type {\n      ...TypeRef\n    }\n    isDeprecated\n    deprecationReason\n  }\n  inputFields {\n    ...InputValue\n  }\n  interfaces {\n    ...TypeRef\n  }\n  enumValues(includeDeprecated: true) {\n    name\n    description\n    isDeprecated\n    deprecationReason\n  }\n  possibleTypes {\n    ...TypeRef\n  }\n}\n\nfragment InputValue on __InputValue {\n  name\n  description\n  type {\n    ...TypeRef\n  }\n  defaultValue\n}\n\nfragment TypeRef on __Type {\n  kind\n  name\n  ofType {\n    kind\n    name\n    ofType {\n      kind\n      name\n      ofType {\n        kind\n        name\n        ofType {\n          kind\n          name\n          ofType {\n            kind\n            name\n            ofType {\n              kind\n              name\n              ofType {\n                kind\n                name\n              }\n            }\n          }\n        }\n      }\n    }\n  }\n}\n"}"""

fun introspectSchema(url: URL): Document {
    val connection = url.openConnection() as HttpURLConnection

    connection.doInput = true
    connection.doOutput = true
    connection.setRequestProperty("Accept", "application/json")
    connection.setRequestProperty("Content-Type", "application/json")

    val out = connection.outputStream.bufferedWriter()

    out.write(payload)
    out.flush()
    out.close()

    val resultJson = connection.inputStream.reader().readText()
    val result = gson.fromJson<Map<String, Any>>(resultJson, object : ParameterizedType {
        override fun getRawType(): Type = Map::class.java
        override fun getOwnerType(): Type? = null
        override fun getActualTypeArguments(): Array<Type> = arrayOf(String::class.java, Any::class.java)
    })

    @Suppress("UNCHECKED_CAST")
    return introspectionResultToSchema.createSchemaDefinition(result["data"] as Map<String, Any>)
}