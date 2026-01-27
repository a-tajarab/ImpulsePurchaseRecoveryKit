package com.example.impulsepurchaserecoverykit.ui.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.impulsepurchaserecoverykit.database.models.CategorySpend
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry

@Composable
fun CategorySpendBarChart(
    data: List<CategorySpend>,
    modifier: Modifier = Modifier
) {
    val entries = data.mapIndexed { index, row ->
        FloatEntry(
            x = index.toFloat(),
            y = row.total.toFloat()
        )
    }

    val producer = ChartEntryModelProducer(entries)

    Chart(
        chart = columnChart(),
        chartModelProducer = producer,
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    )
}