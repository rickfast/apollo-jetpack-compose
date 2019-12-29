package com.expediagroup.graphql.client.ktor

import com.expediagroup.graphql.client.core.GraphQLClient
import com.expediagroup.graphql.client.core.GraphQLResult
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.call.TypeInfo
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.JsonSerializer
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import kotlinx.io.core.Input
import kotlinx.io.core.readText
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.URL

class KtorGraphQLClient @KtorExperimentalAPI constructor(private val url: URL,
                                                         engine: HttpClientEngineFactory<*> = Apache
) : GraphQLClient {
    private val gson: Gson = Gson()
    private val typeCache = mutableMapOf<Class<*>, ParameterizedType>()

    private val client = HttpClient(engine) {
        install(JsonFeature) {
            serializer = object : JsonSerializer {
                override fun read(type: TypeInfo, body: Input): Any {
                    return body.readText()
                }

                override fun write(data: Any, contentType: ContentType): OutgoingContent {
                    return TextContent(gson.toJson(data), contentType)
                }

            }
        }
    }

    override suspend fun <T> executeOperation(query: String, variables: Any, context: Any, resultType: Class<T>): GraphQLResult<T> {
        val payload = mapOf<String, Any?>(
            "query" to query,
            "variables" to variables
        )
        val result = client.post<String>(url) {
            body = payload
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }

        return gson.fromJson(result, parameterizedType(resultType))
    }

    private fun <T> parameterizedType(resultType: Class<T>): ParameterizedType {
        return typeCache.computeIfAbsent(resultType) {
            object : ParameterizedType {
                override fun getRawType(): Type = GraphQLResult::class.java

                override fun getOwnerType(): Type? = null

                override fun getActualTypeArguments(): Array<Type> = arrayOf(resultType)
            }
        }
    }
}