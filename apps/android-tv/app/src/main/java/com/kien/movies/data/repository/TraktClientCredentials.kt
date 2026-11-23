package com.kien.movies.data.repository

data class TraktClientCredentials(val clientId: String, val clientSecret: String) {
    val isComplete: Boolean
        get() = clientId.isNotBlank() && clientSecret.isNotBlank()
}
