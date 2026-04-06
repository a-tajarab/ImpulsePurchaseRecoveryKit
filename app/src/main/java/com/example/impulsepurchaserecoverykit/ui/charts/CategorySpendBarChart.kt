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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impulsepurchaserecoverykit.database.models.CategorySpend

private val Teal700 = Color(0xFF2E6B63)
private val Teal500 = Color(0xFF4E9E8F)
private val Teal200 = Color(0xFFA8D5CC)
private val Terra500 = Color(0xFFD4845A)
private val Terra700 = Color(0xFFB5623A)
private val Warning700 = Color(0xFFB06A00)

@Composable
fun CategorySpendBarChart(
    data: List<CategorySpend>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOf { it.total }.takeIf { it > 0 } ?: 1.0
    val barColors = listOf(Teal700, Teal500, Terra500, Warning700, Teal200, Teal700)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        data.forEachIndexed { index, row ->
            val fraction = (row.total / maxValue).toFloat().coerceIn(0.03f, 1f)
            val color = barColors[index % barColors.size]
            val valueLabel = if (row.total >= 1000)
                "£${String.format("%.1f", row.total / 1000)}k"
            else
                "£${String.format("%.2f", row.total)}"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Bar + category label inside + value at end ──
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    // Background track
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
                        // Coloured bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(color),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (fraction > 0.25f) {
                                // Category label inside bar
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

                // ── Value at end of bar ──
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
