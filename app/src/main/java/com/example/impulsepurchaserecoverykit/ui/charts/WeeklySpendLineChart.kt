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
import com.example.impulsepurchaserecoverykit.ui.theme.Teal700
import com.example.impulsepurchaserecoverykit.ui.theme.Teal100

/**
 * Line chart composable displaying the user's total spending trend
 * over weekly periods.
 *
 * Renders one data point per [WeeklySpend] entry, with the x-axis
 * representing week index (0 = oldest week, n = most recent) and the
 * y-axis representing the total amount spent in GBP during that week.
 *
 * The line and data point markers are rendered in [Teal700] (midnight navy)
 * to visually associate the spending chart with the app's primary colour,
 * distinguishing it from the regret chart which uses the warm stone accent.
 * Each data point is rendered as a pill-shaped marker with a white stroke
 * so individual weeks are clearly identifiable on the line.
 *
 * The background shader is explicitly set to null to render a clean line
 * without any fill area beneath it, keeping the chart uncluttered when
 * multiple weeks of data are displayed.
 *
 * Peaks in the chart highlight weeks with unusually high expenditure that
 * may be worth the user reflecting on or discussing with KIRA.
 *
 * Built using the Vico chart library with a [ChartEntryModelProducer] to
 * supply data and a custom [LineChart.LineSpec] for styling.
 *
 * Used on the Spending tab of the Stats screen, toggled between weekly
 * and monthly views via the toggle control above the chart.
 *
 * @param data The list of [WeeklySpend] entries to plot, one point per week.
 *             The chart renders nothing meaningful if this list is empty.
 * @param modifier Optional [Modifier] applied to the root [Chart] composable
 */
@Composable
fun WeeklySpendLineChart(
    data: List<WeeklySpend>,
    modifier: Modifier = Modifier
) {
    // Map each WeeklySpend to a FloatEntry — x is the week index, y is the total spend in GBP
    val entries = data.mapIndexed { index, row ->
        FloatEntry(x = index.toFloat(), y = row.total.toFloat())
    }

    // ChartEntryModelProducer supplies the entry list to the Vico chart engine
    val producer = ChartEntryModelProducer(entries)

    // Line styling — midnight navy with pill-shaped data point markers
    val lineSpec = LineChart.LineSpec(
        lineColor = Teal700.hashCode(),
        lineThicknessDp = 3f,
        lineBackgroundShader = null,  // No fill area beneath the line — keeps the chart clean
        point = shapeComponent(
            shape = Shapes.pillShape,
            color = Teal700,
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
            .height(220.dp)  // Taller than WeeklyRegretLineChart to give spend data more space
    )
}