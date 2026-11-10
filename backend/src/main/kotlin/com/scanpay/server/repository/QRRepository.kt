package com.scanpay.server.repository

import com.scanpay.server.db.AstraCassandraClient
import com.scanpay.server.models.QRCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object QRRepository {
    private const val COLLECTION = "qr_codes"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(qrCode: QRCode): Boolean {
        val doc = json.encodeToJsonElement(QRCode.serializer(), qrCode)
        return AstraCassandraClient.insertOne(COLLECTION, doc as kotlinx.serialization.json.JsonObject)
    }

    suspend fun findById(id: Long): QRCode? {
        val filter = buildJsonObject { put("id", id) }
        val doc = AstraCassandraClient.findOne(COLLECTION, filter) ?: return null
        return try { json.decodeFromJsonElement(QRCode.serializer(), doc) } catch (e: Exception) { null }
    }

    suspend fun findByPayload(payload: String): QRCode? {
        val doc = AstraCassandraClient.findOne(COLLECTION, buildJsonObject { put("payload_data", payload) }) ?: return null
        return json.decodeFromJsonElement(QRCode.serializer(), doc)
    }

    suspend fun findByVendorId(vendorId: Long): List<QRCode> {
        val filter = buildJsonObject { put("vendor_id", vendorId) }
        val docs = AstraCassandraClient.findMany(COLLECTION, filter)
        return docs.mapNotNull {
            try { json.decodeFromJsonElement(QRCode.serializer(), it) } catch (e: Exception) { null }
        }
    }

    suspend fun findActiveStaticByVendor(vendorId: Long): QRCode? {
        val filter = buildJsonObject {
            put("vendor_id", vendorId)
            put("qr_type", "STATIC")
            put("status", "active")
        }
        val doc = AstraCassandraClient.findOne(COLLECTION, filter) ?: return null
        return try { json.decodeFromJsonElement(QRCode.serializer(), doc) } catch (e: Exception) { null }
    }
}

