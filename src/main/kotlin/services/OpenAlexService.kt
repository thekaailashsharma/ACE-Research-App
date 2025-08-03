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
            level = LogLevel.ALL
        }
    }

    private val baseUrl = "https://api.openalex.org"

    suspend fun searchWorks(
        query: String,
        filter: String? = null,
        page: Int = 1,
        perPage: Int = 25
    ): OpenAlexResponse {
        println("\nSearching OpenAlex works:")
        println("Query: '$query'")
        println("Filter: $filter")
        println("Page: $page")
        println("Per page: $perPage")
        
        try {
            val response = client.get("$baseUrl/works") {
                url {
                    if (query.isNotEmpty()) {
                        parameters.append("search", query)
                    }
                    if (filter != null) {
                        parameters.append("filter", filter)
                    }
                    parameters.append("page", page.toString())
                    parameters.append("per-page", perPage.toString())
                }
            }.body<OpenAlexResponse>()
            
            println("OpenAlex API response:")
            println("Total results: ${response.meta.count}")
            println("Results in this page: ${response.results.size}")
            println("First few article IDs: ${response.results.take(3).map { it.id }}")
            
            return response
        } catch (e: Exception) {
            println("Error searching OpenAlex works: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getWorkById(id: String): OpenAlexWork {
        println("\nFetching OpenAlex work by ID: $id")
        try {
            val work = client.get("$baseUrl/works/$id").body<OpenAlexWork>()
            println("Successfully fetched work: ${work.title}")
            return work
        } catch (e: Exception) {
            println("Error fetching work $id: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getWorksByAuthor(
        authorId: String,
        page: Int = 1,
        perPage: Int = 25
    ): OpenAlexResponse {
        println("\nFetching works by author: $authorId")
        try {
            val response = client.get("$baseUrl/works") {
                url {
                    parameters.append("filter", "author.id:$authorId")
                    parameters.append("page", page.toString())
                    parameters.append("per-page", perPage.toString())
                }
            }.body<OpenAlexResponse>()
            
            println("Found ${response.results.size} works for author")
            return response
        } catch (e: Exception) {
            println("Error fetching works for author $authorId: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun close() {
        client.close()
    }
}