package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import services.OpenAlexService

fun Route.openAlexRoutes() {
    val openAlexService = OpenAlexService()

    route("/api/openalex") {
        get("/search") {
            val query = call.parameters["query"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "Query parameter is required"
            )
            val page = call.parameters["page"]?.toIntOrNull() ?: 1
            val perPage = call.parameters["perPage"]?.toIntOrNull() ?: 25

            val response = openAlexService.searchWorks(query, page, perPage)
            call.respond(response)
        }

        get("/works/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "Work ID is required"
            )
            
            val work = openAlexService.getWorkById(id)
            call.respond(work)
        }

        get("/author/{authorId}/works") {
            val authorId = call.parameters["authorId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "Author ID is required"
            )
            val page = call.parameters["page"]?.toIntOrNull() ?: 1
            val perPage = call.parameters["perPage"]?.toIntOrNull() ?: 25

            val response = openAlexService.getWorksByAuthor(authorId, page, perPage)
            call.respond(response)
        }
    }
}