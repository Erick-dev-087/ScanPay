package com.scanpay.server.analytics

import com.scanpay.server.models.Transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AnalyticsEngineTest {
    @Test fun totalsOnlyIncludeConfirmedPaymentsInRequestedNairobiDateRange() {
        val rows = listOf(
            Transaction(amount = 100.0, status = "success", initiated_at = "2026-11-09T21:30:00Z", merchant_category = "5411"),
            Transaction(amount = 300.0, status = "success", initiated_at = "2026-11-08T12:00:00Z", merchant_category = "4900"),
            Transaction(amount = 999.0, status = "pending", initiated_at = "2026-11-10T10:00:00Z"),
            Transaction(amount = 999.0, status = "failed", initiated_at = "2026-11-10T10:00:00Z"),
            Transaction(amount = 2000.0, status = "success", initiated_at = "2026-10-01T10:00:00Z"),
            Transaction(amount = 2000.0, status = "success", initiated_at = "2026-11-11T10:00:00Z")
        )
        val result = AnalyticsEngine.calculate(rows, 7, LocalDate.of(2026, 11, 10))
        assertEquals(400.0, result.total_amount)
        assertEquals(2, result.total_count)
        assertEquals(100.0, result.today_amount)
        assertEquals(7, result.daily.size)
        assertEquals(400.0, result.daily.sumOf { it.total })
        assertEquals(100.0, result.categories.sumOf { it.percentage })
        assertEquals(200.0, result.average_transaction)
    }
    @Test fun emptyPeriodHasZeroTotalsAndCompleteChartDates() {
        val result = AnalyticsEngine.calculate(emptyList(), 30, LocalDate.of(2026, 11, 10))
        assertEquals(0.0, result.total_amount)
        assertEquals(0.0, result.average_transaction)
        assertEquals(30, result.daily.size)
        assertEquals("2026-10-12", result.daily.first().date)
        assertTrue(result.categories.isEmpty())
    }
    @Test fun invalidPeriodsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { AnalyticsEngine.calculate(emptyList(), 0) }
        assertThrows(IllegalArgumentException::class.java) { AnalyticsEngine.calculate(emptyList(), 1000) }
    }
}

