package com.expediagroup.graphql.client.core

interface GraphQLClient {
    suspend fun <T> executeOperation(query: String, variables: Any, context: Any, resultType: Class<T>): GraphQLResult<T>
}