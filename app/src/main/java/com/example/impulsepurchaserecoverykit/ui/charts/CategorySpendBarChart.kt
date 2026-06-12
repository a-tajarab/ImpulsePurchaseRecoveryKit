package com.example.impulsepurchaserecoverykit.ui.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impulsepurchaserecoverykit.database.models.CategorySpend
// Import theme tokens — never redeclare colours locally
import com.example.impulsepurchaserecoverykit.ui.theme.Teal700
import com.example.impulsepurchaserecoverykit.ui.theme.Teal500
import com.example.impulsepurchaserecoverykit.ui.theme.Teal200
import com.example.impulsepurchaserecoverykit.ui.theme.Terra500
import com.example.impulsepurchaserecoverykit.ui.theme.Terra700
import com.example.impulsepurchaserecoverykit.ui.theme.Warning700

/**
 * Horizontal bar chart composable displaying spending totals grouped by category.
 *
 * Renders one bar per [CategorySpend] entry, scaled proportionally against the
 * highest-spending category so the largest bar always fills the full available
 * width. Each bar is colour-coded by cycling through the app's theme palette,
 * making it easy to visually distinguish categories at a glance.
 *
 * The category label is rendered inside the bar when the bar is wide enough
 * (fraction > 0.25 of the total width), and outside the bar to the right
 * when the bar is too narrow to fit the text — ensuring the label is always
 * legible regardless of the spend amount.
 *
 * The spend value is shown to the right of each bar, formatted as £X.XX for
 * values under £1,000 and £X.Xk for values of £1,000 or more.
 *
 * Used on the Categories tab of the Stats screen to show the user where
 * their money is going each month by item type.
 *
 * @param data The list of [CategorySpend] entries to render, one bar per entry.
 *             The composable renders nothing if this list is empty.
 * @param modifier Optional [Modifier] applied to the root [Column]
 */
@Composable
fun CategorySpendBarChart(
    data: List<CategorySpend>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    // Scale all bars relative to the highest-spending category
    val maxValue = data.maxOf { it.total }.takeIf { it > 0 } ?: 1.0

    // Colour palette cycled across bars — sourced from theme tokens, never hardcoded
    val barColors = listOf(Teal700, Teal500, Terra500, Warning700, Teal200, Teal700)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        data.forEachIndexed { index, row ->
            // fraction is the bar width as a proportion of the maximum, clamped to a
            // minimum of 0.03 so even tiny values remain visible as a sliver
            val fraction = (row.total / maxValue).toFloat().coerceIn(0.03f, 1f)
            val color = barColors[index % barColors.size]

            // Format the spend value — use 'k' suffix for values >= £1,000
            val valueLabel = if (row.total >= 1000)
                "£${String.format("%.1f", row.total / 1000)}k"
            else
                "£${String.format("%.2f", row.total)}"

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    // Background track — full-width surfaceVariant pill behind the bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coloured bar — width proportional to the category's spend fraction
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(color),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // Label inside the bar — only shown when bar is wide enough
                            if (fraction > 0.25f) {
                                Text(
                                    text = row.category.replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }
                        }
                        // Label outside the bar — shown when bar is too narrow for inline text
                        if (fraction <= 0.25f) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = row.category.replaceFirstChar { it.uppercase() },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Spend value shown to the right of each bar in the matching category colour
                Text(
                    text = valueLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.width(52.dp)
                )
            }
        }
    }
}