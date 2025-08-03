package models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object GoogleTokens : IntIdTable() {
    val accessToken = varchar("access_token", 1000)
    val refreshToken = varchar("refresh_token", 1000)
    val expiresAt = datetime("expires_at")
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
}

class GoogleToken(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<GoogleToken>(GoogleTokens)

    var accessToken by GoogleTokens.accessToken
    var refreshToken by GoogleTokens.refreshToken
    var expiresAt by GoogleTokens.expiresAt
    var createdAt by GoogleTokens.createdAt
    var updatedAt by GoogleTokens.updatedAt

    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)
}