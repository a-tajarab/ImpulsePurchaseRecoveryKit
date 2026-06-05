package com.example.impulsepurchaserecoverykit.ui.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.impulsepurchaserecoverykit.database.models.WeeklyRegret
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
// Import theme tokens — never redeclare colours locally
import com.example.impulsepurchaserecoverykit.ui.theme.Terra500
import com.example.impulsepurchaserecoverykit.ui.theme.Error700

@Composable
fun WeeklyRegretLineChart(
    data: List<WeeklyRegret>,
    modifier: Modifier = Modifier
) {
    val entries = data.mapIndexed { index, row ->
        FloatEntry(x = index.toFloat(), y = row.avgRegret.toFloat())
    }

    val producer = ChartEntryModelProducer(entries)

    val lineSpec = LineChart.LineSpec(
        lineColor = Terra500.hashCode(),
        lineThicknessDp = 3f,
        point = shapeComponent(
            shape = Shapes.pillShape,
            color = Terra500,
            strokeWidth = 2.dp,
            strokeColor = Color.White
        ),
        pointSizeDp = 10f
    )

    Chart(
        chart = lineChart(lines = listOf(lineSpec)),
        chartModelProducer = producer,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    )
}