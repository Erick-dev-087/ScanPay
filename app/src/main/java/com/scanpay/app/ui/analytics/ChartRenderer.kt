package com.scanpay.app.ui.analytics

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.scanpay.app.R
import com.scanpay.app.data.response.DailyAnalytics

object ChartRenderer {
    fun show(chart: LineChart, daily: List<DailyAnalytics>, label: String) {
        val color = chart.context.getColor(R.color.ui_secondary)
        val entries = daily.mapIndexed { index, day -> Entry(index.toFloat(), day.total.toFloat()) }
        val set = LineDataSet(entries, label).apply {
            this.color = color; setCircleColor(color); lineWidth = 2.5f; circleRadius = 3f
            setDrawValues(false); setDrawFilled(true); fillColor = color; fillAlpha = 25
        }
        chart.apply {
            data = LineData(set)
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            axisLeft.textColor = context.getColor(R.color.ui_on_surface_variant)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = context.getColor(R.color.ui_on_surface_variant)
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            xAxis.valueFormatter = IndexAxisValueFormatter(daily.map { it.date.drop(5) })
            setNoDataText("No confirmed payments in this period")
            invalidate()
        }
    }
}

