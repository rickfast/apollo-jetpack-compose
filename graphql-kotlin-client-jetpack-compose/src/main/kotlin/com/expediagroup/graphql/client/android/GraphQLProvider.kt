package com.expediagroup.graphql.client.android

import androidx.compose.Ambient
import androidx.compose.Composable
import java.net.URL

val GraphQLAmbient = Ambient.of<ComposeGraphQLClient>()

@Composable
fun GraphQLProvider(url: URL, children: () -> Unit) {
    GraphQLAmbient.Provider(ComposeGraphQLClient(url), children)
}