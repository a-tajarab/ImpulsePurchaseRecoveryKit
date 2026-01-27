package com.example.impulsepurchaserecoverykit.ui.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.impulsepurchaserecoverykit.database.models.WeeklyRegret
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry

@Composable
fun WeeklyRegretLineChart(
    data: List<WeeklyRegret>,
    modifier: Modifier = Modifier
) {
    val entries = data.mapIndexed { index, row ->
        FloatEntry(
            x = index.toFloat(),
            y = row.avgRegret.toFloat()
        )
    }

    val producer = ChartEntryModelProducer(entries)

    Chart(
        chart = lineChart(),
        chartModelProducer = producer,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    )
}