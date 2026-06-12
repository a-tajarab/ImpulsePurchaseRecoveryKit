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
import com.example.impulsepurchaserecoverykit.ui.theme.Terra500
import com.example.impulsepurchaserecoverykit.ui.theme.Error700

/**
 * Line chart composable displaying the user's average regret score trend
 * over weekly periods.
 *
 * Renders one data point per [WeeklyRegret] entry, with the x-axis
 * representing week index (0 = oldest week, n = most recent) and the
 * y-axis representing the average regret score for that week on a scale
 * of 1.0 to 10.0.
 *
 * The line and data point markers are rendered in [Terra500] (warm stone)
 * to visually associate the regret chart with the app's secondary accent
 * colour, distinguishing it from the spending chart which uses the primary
 * midnight blue. Each data point is rendered as a pill-shaped marker with
 * a white stroke so individual weeks are clearly visible on the line.
 *
 * A downward trend in the chart indicates the user is making more intentional
 * purchase decisions over time — the most meaningful signal the app can show.
 *
 * Built using the Vico chart library with a [ChartEntryModelProducer] to
 * supply data and a custom [LineChart.LineSpec] for styling.
 *
 * Used on the Regret tab of the Stats screen below the top regret list.
 *
 * @param data The list of [WeeklyRegret] entries to plot, one point per week.
 *             The chart renders nothing meaningful if this list is empty.
 * @param modifier Optional [Modifier] applied to the root [Chart] composable
 */
@Composable
fun WeeklyRegretLineChart(
    data: List<WeeklyRegret>,
    modifier: Modifier = Modifier
) {
    // Map each WeeklyRegret to a FloatEntry — x is the week index, y is the average regret score
    val entries = data.mapIndexed { index, row ->
        FloatEntry(x = index.toFloat(), y = row.avgRegret.toFloat())
    }

    // ChartEntryModelProducer supplies the entry list to the Vico chart engine
    val producer = ChartEntryModelProducer(entries)

    // Line styling — warm stone colour with pill-shaped data point markers
    val lineSpec = LineChart.LineSpec(
        lineColor = Terra500.hashCode(),
        lineThicknessDp = 3f,
        point = shapeComponent(
            shape = Shapes.pillShape,
            color = Terra500,
            strokeWidth = 2.dp,
            strokeColor = Color.White  // White stroke makes each data point stand out on the line
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