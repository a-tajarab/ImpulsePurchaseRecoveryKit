package com.example.impulsepurchaserecoverykit.ui.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.impulsepurchaserecoverykit.database.models.WeeklySpend
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry

private val Teal700 = Color(0xFF2E6B63)
private val Teal100 = Color(0xFFD4EDE9)

@Composable
fun WeeklySpendLineChart(
    data: List<WeeklySpend>,
    modifier: Modifier = Modifier
) {
    // x = week index (0..n-1), y = spend total
    val entries = data.mapIndexed { index, row ->
        FloatEntry(x = index.toFloat(), y = row.total.toFloat())
    }

    val producer = ChartEntryModelProducer(entries)

    val lineSpec = LineChart.LineSpec(
        lineColor = Teal700.hashCode(),
        lineThicknessDp = 3f,
        lineBackgroundShader = null,
        point = shapeComponent(
            shape = Shapes.pillShape,
            color = Teal700,
            strokeWidth = 2.dp,
            strokeColor = Color.White
        ),
        pointSizeDp = 10f
    )

    Chart(
        chart = lineChart(
            lines = listOf(lineSpec)),
        chartModelProducer = producer,
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    )
}