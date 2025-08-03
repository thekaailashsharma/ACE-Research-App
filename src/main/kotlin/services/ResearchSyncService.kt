package services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ResearchSyncService(
    private val openAlexService: OpenAlexService,
    private val googleSheetsService: GoogleSheetsService
) {
    suspend fun syncNewArticles(journalIds: List<String>) = withContext(Dispatchers.IO) {
        try {
            println("Starting sync process for journal IDs: $journalIds")
            
            val today = LocalDate.now()
            val fromDate = today.minusDays(7) // Sync last 7 days of articles
            
            // Format dates for OpenAlex API
            val dateFormatter = DateTimeFormatter.ISO_DATE
            val dateFilter = "from_publication_date:${fromDate.format(dateFormatter)}"
            println("Date filter: $dateFilter")
            
            // Fetch already approved articles to avoid duplicates
            println("Fetching approved articles...")
            val approvedArticleIds = googleSheetsService.getApprovedArticles().toSet()
            println("Found ${approvedArticleIds.size} approved articles")
            
            // Fetch new articles for each journal
            var totalNewArticles = 0
            for (journalId in journalIds) {
                println("\nProcessing journal ID: $journalId")
                val filter = "primary_location.source.id:$journalId,$dateFilter"
                println("Using filter: $filter")
                
                val response = openAlexService.searchWorks(
                    query = "",
                    filter = filter
                )
                
                println("OpenAlex API returned ${response.results.size} articles")
                
                // Filter out already approved articles
                val newArticles = response.results.filterNot { 
                    it.id in approvedArticleIds 
                }
                
                println("After filtering approved articles: ${newArticles.size} new articles to add")
                
                if (newArticles.isNotEmpty()) {
                    println("Adding new articles to Google Sheet...")
                    println("Article IDs to be added: ${newArticles.map { it.id }}")
                    googleSheetsService.appendResearchArticles(newArticles)
                    totalNewArticles += newArticles.size
                } else {
                    println("No new articles to add for this journal")
                }
            }
            
            println("\nSync completed successfully!")
            println("Total new articles added: $totalNewArticles")
            
        } catch (e: Exception) {
            println("Error during sync process: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getApprovedArticles() = withContext(Dispatchers.IO) {
        try {
            println("Fetching all approved articles...")
            val approvedIds = googleSheetsService.getApprovedArticles()
            println("Found ${approvedIds.size} approved article IDs")
            
            val articles = approvedIds.mapNotNull { id ->
                try {
                    println("Fetching details for article ID: $id")
                    openAlexService.getWorkById(id)
                } catch (e: Exception) {
                    println("Failed to fetch article $id: ${e.message}")
                    null // Skip articles that can't be fetched
                }
            }
            
            println("Successfully fetched ${articles.size} approved articles")
            articles
            
        } catch (e: Exception) {
            println("Error fetching approved articles: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}