package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAlexWork(
    val id: String,
    val doi: String? = null,
    val title: String,
    @SerialName("publication_year")
    val publicationYear: Int? = null,
    @SerialName("publication_date")
    val publicationDate: String? = null,
    @SerialName("cited_by_count")
    val citedByCount: Int = 0,
    val type: String? = null,
    @SerialName("primary_location")
    val primaryLocation: OpenAlexLocation? = null,
    val authorships: List<OpenAlexAuthorship> = emptyList(),
    val abstract: String? = null
)

@Serializable
data class OpenAlexLocation(
    val source: OpenAlexSource? = null,
    val version: String? = null,
    val license: String? = null
)

@Serializable
data class OpenAlexSource(
    val id: String? = null,
    val displayName: String? = null,
    val type: String? = null
)

@Serializable
data class OpenAlexAuthorship(
    val author: OpenAlexAuthor,
    @SerialName("author_position")
    val authorPosition: String? = null,
    val institutions: List<OpenAlexInstitution> = emptyList()
)

@Serializable
data class OpenAlexAuthor(
    val id: String,
    @SerialName("display_name")
    val displayName: String
)

@Serializable
data class OpenAlexInstitution(
    val id: String,
    @SerialName("display_name")
    val displayName: String,
    val type: String? = null,
    @SerialName("country_code")
    val countryCode: String? = null
)

@Serializable
data class OpenAlexResponse(
    val meta: OpenAlexMeta,
    val results: List<OpenAlexWork>
)

@Serializable
data class OpenAlexMeta(
    val count: Int,
    @SerialName("page")
    val currentPage: Int,
    @SerialName("per_page")
    val perPage: Int
)