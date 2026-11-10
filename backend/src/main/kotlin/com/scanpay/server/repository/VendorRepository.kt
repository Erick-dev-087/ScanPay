package com.scanpay.server.repository

import com.scanpay.server.db.AstraCassandraClient
import com.scanpay.server.models.Vendor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object VendorRepository {
    private const val COLLECTION = "vendors"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(vendor: Vendor): Boolean {
        val doc = json.encodeToJsonElement(Vendor.serializer(), vendor)
        return AstraCassandraClient.insertOne(COLLECTION, doc as kotlinx.serialization.json.JsonObject)
    }

    suspend fun findByEmail(email: String): Vendor? {
        val filter = buildJsonObject { put("email", email.trim().lowercase()) }
        val doc = AstraCassandraClient.findOne(COLLECTION, filter) ?: return null
        return try { json.decodeFromJsonElement(Vendor.serializer(), doc) } catch (e: Exception) { null }
    }

    suspend fun findByPhone(phone: String): Vendor? {
        val filter = buildJsonObject { put("phone", phone.trim()) }
        val doc = AstraCassandraClient.findOne(COLLECTION, filter) ?: return null
        return try { json.decodeFromJsonElement(Vendor.serializer(), doc) } catch (e: Exception) { null }
    }

    suspend fun findByShortcode(shortcode: String): Vendor? {
        val filter = buildJsonObject { put("business_shortcode", shortcode.trim()) }
        val doc = AstraCassandraClient.findOne(COLLECTION, filter) ?: return null
        return try { json.decodeFromJsonElement(Vendor.serializer(), doc) } catch (e: Exception) { null }
    }

    suspend fun findById(id: Long): Vendor? {
        val filter = buildJsonObject { put("id", id) }
        val doc = AstraCassandraClient.findOne(COLLECTION, filter) ?: return null
        return try { json.decodeFromJsonElement(Vendor.serializer(), doc) } catch (e: Exception) { null }
    }
}

