package com.expediagroup.graphql.client.android

import com.expediagroup.graphql.client.ktor.KtorGraphQLClient
import io.ktor.client.engine.android.Android
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.net.URL

class ComposeGraphQLClient(private val url: URL) {
    @KtorExperimentalAPI
    suspend fun <T> execute(query: String, variables: Any, klazz: Class<T>): GraphQLModel<T> = coroutineScope {
        val model: GraphQLModel<T> = GraphQLModel(loading = true)
        val graphQLClient = KtorGraphQLClient(url, Android)

        val deferred = async {
            val result = graphQLClient.executeOperation(query, variables, mapOf<String, String>(), klazz)

            model.data = result.data

            if (result.errors != null) {
                model.errors = result.errors!!
            }
        }

        deferred.invokeOnCompletion { cause -> cause?.printStackTrace() }

        model
    }
}