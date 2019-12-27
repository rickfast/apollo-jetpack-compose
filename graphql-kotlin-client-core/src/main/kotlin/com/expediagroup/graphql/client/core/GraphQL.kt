package com.expediagroup.graphql.client.core

suspend inline fun <reified T> GraphQLClient.execute(query: String, variables: Any, context: Any): GraphQLResult<T> {
    return executeOperation(query, variables, context, T::class.java)
}