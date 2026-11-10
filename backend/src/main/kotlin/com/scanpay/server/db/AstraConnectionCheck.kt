package com.scanpay.server.db

import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

/** Read-only connectivity check; no schema changes or payment requests. */
fun main() {
    try {
        AstraCassandraClient.connect(migrate = false)
        check(runBlocking { AstraCassandraClient.ping() })
        println("Astra Cassandra CQL connection verified")
    } catch (error: Exception) {
        System.err.println("Astra connection failed: ${error.javaClass.simpleName}")
        exitProcess(1)
    } finally {
        AstraCassandraClient.close()
    }
}
