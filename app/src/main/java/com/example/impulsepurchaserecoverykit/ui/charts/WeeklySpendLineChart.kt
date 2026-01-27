package com.example.impulsepurchaserecoverykit.ui.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.impulsepurchaserecoverykit.database.models.WeeklySpend
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry

@Composable
fun WeeklySpendLineChart(
    data: List<WeeklySpend>,
    modifier: Modifier = Modifier
) {
    // x = week index (0..n-1), y = spend total
    val entries = data.mapIndexed { index, row ->
        FloatEntry(
            x = index.toFloat(),
            y = row.total.toFloat()
        )
    }

    val producer = ChartEntryModelProducer(entries)

    Chart(
        chart = lineChart(),
        chartModelProducer = producer,
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    )
}