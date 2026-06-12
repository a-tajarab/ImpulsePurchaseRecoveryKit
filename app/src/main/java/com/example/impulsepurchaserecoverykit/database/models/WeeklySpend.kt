package com.example.impulsepurchaserecoverykit.database.models

/**
 * Query result model representing the total amount spent within a single week.
 *
 * WeeklySpend is not a full Room entity — it is a lightweight data class
 * used to hold the result of an aggregated SQL query that groups receipts
 * by week and sums their totals for each period.
 *
 * It is used to populate the weekly spend trend line chart on the Spending
 * Stats screen, giving the user a clear visual picture of how their spending
 * fluctuates from week to week. Peaks in the chart highlight weeks with
 * unusually high expenditure that may be worth reflecting on.
 *
 * Example: WeeklySpend(weekStart = 1748736000000L, total = 448.68)
 *
 * @property weekStart The timestamp (in milliseconds since epoch) of the
 *                     Monday that begins this week, used as the x-axis
 *                     value on the spending trend chart
 * @property total The total amount spent across all receipts logged
 *                 during this week, in GBP
 */
data class WeeklySpend(
    val weekStart: Long,
    val total: Double
)