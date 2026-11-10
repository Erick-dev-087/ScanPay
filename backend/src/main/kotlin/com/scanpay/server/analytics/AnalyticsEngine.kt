package com.scanpay.server.analytics

import com.scanpay.server.models.*
import com.scanpay.server.repository.TransactionRepository
import java.time.*

object AnalyticsEngine {
    val zone: ZoneId = ZoneId.of("Africa/Nairobi")
    suspend fun getMerchantAnalytics(id: Long, days: Int = 30) = load("vendor", id, days)
    suspend fun getUserAnalytics(id: Long, days: Int = 30) = load("user", id, days)
    private suspend fun load(owner: String, id: Long, days: Int): AnalyticsResponse {
        require(days in 1..365) { "days must be between 1 and 365" }
        val today = LocalDate.now(zone)
        val since = today.minusDays(days.toLong() - 1).atStartOfDay(zone).toInstant()
        return calculate(TransactionRepository.history(owner, id, since), days, today)
    }
    fun calculate(transactions: List<Transaction>, days: Int, today: LocalDate = LocalDate.now(zone)): AnalyticsResponse {
        require(days in 1..365)
        val first = today.minusDays(days.toLong() - 1)
        val paid = transactions.filter { tx ->
            val date = Instant.parse(tx.initiated_at).atZone(zone).toLocalDate()
            tx.status == "success" && date >= first && date <= today
        }
        val total = paid.sumOf { it.amount }
        val daily = (0 until days).map { offset ->
            val date = first.plusDays(offset.toLong())
            val rows = paid.filter { Instant.parse(it.initiated_at).atZone(zone).toLocalDate() == date }
            DailyAnalytics(date.toString(), rows.sumOf { it.amount }, rows.size)
        }
        val categories = paid.groupBy { category(it.merchant_category) }.map { (name, rows) ->
            val amount = rows.sumOf { it.amount }
            CategorySpending(name, amount, rows.size, if (total > 0) amount / total * 100 else 0.0)
        }.sortedByDescending { it.amount }
        return AnalyticsResponse(total, paid.size, daily.last().total, daily.last().count,
            if (paid.isNotEmpty()) total / paid.size else 0.0, daily, categories)
    }
    private fun category(mcc: String?) = when (mcc) {
        "5411" -> "Groceries"; "4900" -> "Utilities"; "5812", "5814" -> "Dining"
        "4111", "4121", "5541" -> "Transport"; "5912", "8099" -> "Health"
        else -> "Other payments"
    }
}

