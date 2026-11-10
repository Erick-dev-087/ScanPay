package com.scanpay.server.repository

import com.scanpay.server.db.AstraCassandraClient
import com.scanpay.server.models.Transaction
import kotlinx.serialization.json.*
import java.time.*

object TransactionRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private fun encode(tx: Transaction) = json.encodeToJsonElement(Transaction.serializer(), tx).jsonObject
    private fun decode(doc: JsonObject) = json.decodeFromJsonElement(Transaction.serializer(), doc)
    suspend fun save(tx: Transaction) = AstraCassandraClient.insertOne("transactions", encode(tx))
    suspend fun findById(id: Long) = AstraCassandraClient.findOne("transactions", buildJsonObject { put("id", id) })?.let(::decode)
    suspend fun findByCheckoutRequestId(id: String) = AstraCassandraClient.findOne("transactions", buildJsonObject { put("checkout_request_id", id) })?.let(::decode)
    suspend fun findByUserId(id: Long, limit: Int = 50) = history("user", id, Instant.now().minus(Duration.ofDays(366)), limit)
    suspend fun findByVendorId(id: Long, limit: Int = 50) = history("vendor", id, Instant.now().minus(Duration.ofDays(366)), limit)
    suspend fun history(owner: String, id: Long, since: Instant, limit: Int = Int.MAX_VALUE) =
        AstraCassandraClient.history(owner, id, since, limit).map(::decode)
    suspend fun attachCheckout(tx: Transaction, checkout: String): Transaction {
        AstraCassandraClient.linkCheckout(tx.id, checkout)
        val updated = tx.copy(status = "pending", checkout_request_id = checkout)
        check(AstraCassandraClient.transition(tx.id, tx.status, encode(updated))) { "Cannot attach checkout" }
        return updated
    }
    suspend fun complete(tx: Transaction, status: String, receipt: String? = null): Boolean =
        AstraCassandraClient.transition(tx.id, tx.status, encode(tx.copy(status = status, mpesa_receipt = receipt, completed_at = Instant.now().toString())))
}

