package services

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import models.OpenAlexWork
import java.io.File
import java.io.FileInputStream

class GoogleSheetsService(
    private val credentialsPath: String,
    private val applicationName: String,
    private val spreadsheetId: String
) {
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val scopes = listOf(SheetsScopes.SPREADSHEETS, SheetsScopes.SPREADSHEETS_READONLY)
    private val transport = GoogleNetHttpTransport.newTrustedTransport()

    init {
        println("Initializing GoogleSheetsService with spreadsheetId: $spreadsheetId")
    }

    private fun getCredentials(): GoogleCredential {
        val credentialsFile = File(credentialsPath).absoluteFile
        if (!credentialsFile.exists()) {
            throw IllegalStateException("Service account credentials file not found at ${credentialsFile.absolutePath}")
        }

        try {
            val credential = GoogleCredential.fromStream(FileInputStream(credentialsFile))
                .createScoped(scopes)
            println("Successfully loaded credentials for service account: ${credential.serviceAccountId}")
            return credential
        } catch (e: Exception) {
            val errorMessage = "Error loading service account credentials: ${e.message}"
            println(errorMessage)
            e.printStackTrace()
            throw IllegalStateException(errorMessage, e)
        }
    }

    private fun getSheetsService(): Sheets {
        return try {
            val credential = getCredentials()
            Sheets.Builder(transport, jsonFactory, credential)
                .setApplicationName(applicationName)
                .build()
        } catch (e: Exception) {
            val errorMessage = "Error creating Sheets service: ${e.message}"
            println(errorMessage)
            e.printStackTrace()
            throw IllegalStateException(errorMessage, e)
        }
    }

    private suspend fun formatSheet() = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()

            // Get the sheet ID (usually 0 for the first sheet)
            val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
            val sheetId = spreadsheet.sheets[0].properties.sheetId

            val requests = mutableListOf<Request>()

            // 1. Freeze the header row
            requests.add(Request().setUpdateSheetProperties(
                UpdateSheetPropertiesRequest()
                    .setProperties(SheetProperties().setSheetId(sheetId).setGridProperties(
                        GridProperties().setFrozenRowCount(1)
                    ))
                    .setFields("gridProperties.frozenRowCount")
            ))

            // 2. Format headers
            requests.add(Request().setRepeatCell(
                RepeatCellRequest()
                    .setRange(GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(0)
                        .setEndRowIndex(1)
                    )
                    .setCell(CellData()
                        .setUserEnteredFormat(CellFormat()
                            .setBackgroundColor(Color().setRed(0.2f).setGreen(0.2f).setBlue(0.2f))
                            .setTextFormat(TextFormat()
                                .setBold(true)
                                .setForegroundColor(Color().setRed(1f).setGreen(1f).setBlue(1f))
                            )
                        )
                    )
                    .setFields("userEnteredFormat(backgroundColor,textFormat)")
            ))

            // 3. Add alternating row colors
            requests.add(Request().setAddBanding(
                AddBandingRequest()
                    .setBandedRange(BandedRange()
                        .setRange(GridRange()
                            .setSheetId(sheetId)
                            .setStartRowIndex(1)
                        )
                        .setRowProperties(BandingProperties()
                            .setHeaderColor(Color().setRed(0.9f).setGreen(0.9f).setBlue(0.9f))
                            .setFirstBandColor(Color().setRed(1f).setGreen(1f).setBlue(1f))
                            .setSecondBandColor(Color().setRed(0.95f).setGreen(0.95f).setBlue(0.95f))
                        )
                    )
            ))

            // 4. Auto-resize columns
            requests.addAll((0..6).map { colIndex ->
                Request().setAutoResizeDimensions(
                    AutoResizeDimensionsRequest()
                        .setDimensions(DimensionRange()
                            .setSheetId(sheetId)
                            .setDimension("COLUMNS")
                            .setStartIndex(colIndex)
                            .setEndIndex(colIndex + 1)
                        )
                )
            })

            // 5. Add data validation for Status column (column F, index 5)
            requests.add(Request().setSetDataValidation(
                SetDataValidationRequest()
                    .setRange(GridRange()
                        .setSheetId(sheetId)
                        .setStartColumnIndex(5)
                        .setEndColumnIndex(6)
                        .setStartRowIndex(1)
                    )
                    .setRule(DataValidationRule()
                        .setCondition(BooleanCondition()
                            .setType("ONE_OF_LIST")
                            .setValues(listOf(
                                ConditionValue().setUserEnteredValue("Pending"),
                                ConditionValue().setUserEnteredValue("Approved"),
                                ConditionValue().setUserEnteredValue("Rejected")
                            ))
                        )
                        .setShowCustomUi(true)
                        .setStrict(true)
                    )
            ))

            // 6. Add filters
            requests.add(Request().setSetBasicFilter(
                SetBasicFilterRequest()
                    .setFilter(BasicFilter()
                        .setRange(GridRange()
                            .setSheetId(sheetId)
                            .setStartRowIndex(0)
                            .setStartColumnIndex(0)
                            .setEndColumnIndex(7)
                        )
                    )
            ))

            // Apply all formatting
            val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(requests)
            service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
            
            println("Sheet formatting completed successfully")
        } catch (e: Exception) {
            println("Error formatting sheet: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun initializeSheetIfNeeded() = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
            
            // Check if headers exist
            val response = service.spreadsheets().values()
                .get(spreadsheetId, "A1:G1")
                .execute()

            if (response.getValues()?.isEmpty() != false) {
                // Add headers if they don't exist
                val headers = listOf(
                    listOf(
                        "Article ID",
                        "Title",
                        "Publication Date",
                        "DOI",
                        "Abstract",
                        "Status",
                        "Comments"
                    )
                )
                
                val body = ValueRange().setValues(headers)
                service.spreadsheets().values()
                    .update(spreadsheetId, "A1:G1", body)
                    .setValueInputOption("RAW")
                    .execute()
                
                println("Initialized sheet with headers")
                
                // Apply formatting after adding headers
                formatSheet()
            }
        } catch (e: Exception) {
            println("Error initializing sheet: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun appendResearchArticles(articles: List<OpenAlexWork>) = withContext(Dispatchers.IO) {
        try {
            println("Starting to append ${articles.size} articles to spreadsheet: $spreadsheetId")
            
            // Initialize sheet with headers if needed
            initializeSheetIfNeeded()
            
            val service = getSheetsService()
            
            // Get the last row number
            val response = service.spreadsheets().values()
                .get(spreadsheetId, "A:A")
                .execute()
            val lastRow = response.getValues()?.size ?: 1
            println("Last row in sheet: $lastRow")
            
            val values = articles.map { article ->
                listOf(
                    article.id,
                    article.title,
                    article.publicationDate,
                    article.doi ?: "",
                    article.abstract ?: "",
                    "Pending", // Approval status
                    "" // Comments
                )
            }

            val body = ValueRange().setValues(values)
            
            // Append after the last row
            val range = "A${lastRow + 1}"
            println("Appending data to range: $range")
            
            val result = service.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("RAW")
                .execute()

            println("Successfully updated spreadsheet. Updated range: ${result.updatedRange}")
            
            // Ensure formatting is maintained
            formatSheet()
            
            result
        } catch (e: Exception) {
            val errorMessage = "Error appending articles to spreadsheet: ${e.message}"
            println(errorMessage)
            e.printStackTrace()
            throw IllegalStateException(errorMessage, e)
        }
    }

    suspend fun getApprovedArticles(): List<String> = withContext(Dispatchers.IO) {
        try {
            println("Fetching approved articles from spreadsheet: $spreadsheetId")
            val service = getSheetsService()
            
            val response = service.spreadsheets().values()
                .get(spreadsheetId, "A:G")
                .execute()

            val approvedArticles = response.getValues()?.drop(1)?.mapNotNull { row ->
                if (row.size >= 6 && row[5] == "Approved") {
                    row[0].toString() // Return the article ID
                } else null
            } ?: emptyList()

            println("Found ${approvedArticles.size} approved articles")
            approvedArticles
        } catch (e: Exception) {
            val errorMessage = "Error fetching approved articles: ${e.message}"
            println(errorMessage)
            e.printStackTrace()
            throw IllegalStateException(errorMessage, e)
        }
    }
}