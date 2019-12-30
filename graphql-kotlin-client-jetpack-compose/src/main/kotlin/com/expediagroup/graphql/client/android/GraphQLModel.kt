package com.expediagroup.graphql.client.android

import androidx.compose.Model
import com.expediagroup.graphql.client.core.GraphQLError

@Model
data class GraphQLModel<T>(
    var loading: Boolean,
    var data: T? = null,
    var errors: List<GraphQLError> = listOf()
)