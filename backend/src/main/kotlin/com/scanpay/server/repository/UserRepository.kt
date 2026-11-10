package com.scanpay.server.repository

import com.scanpay.server.db.AstraCassandraClient
import com.scanpay.server.models.User
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object UserRepository {
    private const val COLLECTION = "users"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(user: User): Boolean {
        val doc = json.encodeToJsonElement(User.serializer(), user)
        return AstraCassandraClient.insertOne(COLLECTION, doc as kotlinx.serialization.json.JsonObject)
    }

    suspend fun findByEmail(email: String): User? {
        val filter = buildJsonObject { put("email", email.trim().lowercase()) }
        val doc = AstraCassandraClient.findOne(COLLECTION, filter) ?: return null
        return try { json.decodeFromJsonElement(User.serializer(), doc) } catch (e: Exception) { null }
    }

    suspend fun findByPhone(phone: String): User? {
        val filter = buildJsonObject { put("phone_number", phone.trim()) }
        val doc = AstraCassandraClient.findOne(COLLECTION, filter) ?: return null
        return try { json.decodeFromJsonElement(User.serializer(), doc) } catch (e: Exception) { null }
    }

    suspend fun findById(id: Long): User? {
        val filter = buildJsonObject { put("id", id) }
        val doc = AstraCassandraClient.findOne(COLLECTION, filter) ?: return null
        return try { json.decodeFromJsonElement(User.serializer(), doc) } catch (e: Exception) { null }
    }
}

