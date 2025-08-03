package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import services.GoogleSheetsService
import services.OpenAlexService
import services.ResearchSyncService

@Serializable
data class SyncRequest(
    val journalIds: List<String>
)

@Serializable
data class ErrorResponse(
    val error: String,
    val details: String? = null
)

fun Route.researchRoutes(
    credentialsPath: String,
    applicationName: String,
    spreadsheetId: String
) {
    val openAlexService = OpenAlexService()
    val googleSheetsService = GoogleSheetsService(
        credentialsPath = credentialsPath,
        applicationName = applicationName,
        spreadsheetId = spreadsheetId
    )
    val researchSyncService = ResearchSyncService(openAlexService, googleSheetsService)

    route("/api/research") {
        // Trigger sync of new articles
        post("/sync") {
            try {
                val request = call.receive<SyncRequest>()
                if (request.journalIds.isEmpty()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(error = "No journal IDs provided")
                    )
                    return@post
                }
                
                researchSyncService.syncNewArticles(request.journalIds)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Sync completed successfully"))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        error = "Failed to sync articles",
                        details = e.message
                    )
                )
            }
        }

        // Get approved articles
        get("/approved") {
            try {
                val articles = researchSyncService.getApprovedArticles()
                call.respond(articles)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        error = "Failed to fetch approved articles",
                        details = e.message
                    )
                )
            }
        }
    }
}