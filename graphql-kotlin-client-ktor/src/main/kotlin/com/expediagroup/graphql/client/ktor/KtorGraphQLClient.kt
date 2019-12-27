package com.expediagroup.graphql.client.ktor

import com.expediagroup.graphql.client.core.GraphQLClient
import com.expediagroup.graphql.client.core.GraphQLResult
import com.expediagroup.graphql.client.core.execute
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.URL

class KtorGraphQLClient @KtorExperimentalAPI constructor(private val url: URL,
                                                         engine: HttpClientEngineFactory<*> = CIO) : GraphQLClient {
    private val gson: Gson = Gson()
    private val typeCache = mutableMapOf<Class<*>, ParameterizedType>()

    private val client = HttpClient(engine) {}

    override suspend fun <T> executeOperation(query: String, variables: Any, context: Any, resultType: Class<T>): GraphQLResult<T> {
        val payload = mapOf<String, Any?>(
            "query" to query,
            "variables" to variables
        )
        val result = client.post<String>(url) {
            body = gson.toJson(payload)
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

data class Film(val id: String)
data class AllFilms(val allFilms: List<Film>)

@KtorExperimentalAPI
fun main() {
    runBlocking {
        val result: GraphQLResult<AllFilms> = KtorGraphQLClient(URL("https://swapi.graph.cool/"))
            .execute("{\n" +
                    "  allFilms{\n" +
                    "    id\n" +
                    "  }\n" +
                    "}", mapOf<String, String>(), mapOf<String, String>())

        println(result)
    }
}