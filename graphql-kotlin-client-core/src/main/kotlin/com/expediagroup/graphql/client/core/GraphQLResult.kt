package com.expediagroup.graphql.client.core

data class GraphQLResult<T>(
    val data: T?,
    val errors: List<GraphQLError>?,
    val extensions: Map<String, String>
)