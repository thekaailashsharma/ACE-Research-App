package services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import models.OpenAlexResponse
import models.OpenAlexWork

class OpenAlexService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    private val baseUrl = "https://api.openalex.org"

    suspend fun searchWorks(
        query: String,
        page: Int = 1,
        perPage: Int = 25
    ): OpenAlexResponse {
        return client.get("$baseUrl/works") {
            url {
                parameters.append("search", query)
                parameters.append("page", page.toString())
                parameters.append("per-page", perPage.toString())
            }
        }.body()
    }

    suspend fun getWorkById(id: String): OpenAlexWork {
        return client.get("$baseUrl/works/$id").body()
    }

    suspend fun getWorksByAuthor(
        authorId: String,
        page: Int = 1,
        perPage: Int = 25
    ): OpenAlexResponse {
        return client.get("$baseUrl/works") {
            url {
                parameters.append("filter", "author.id:$authorId")
                parameters.append("page", page.toString())
                parameters.append("per-page", perPage.toString())
            }
        }.body()
    }

    fun close() {
        client.close()
    }
}