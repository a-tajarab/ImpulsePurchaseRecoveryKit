package com.example.impulsepurchaserecoverykit.database.models

/**
 * Query result model representing the average regret score for a single week.
 *
 * WeeklyRegret is not a full Room entity — it is a lightweight data class
 * used to hold the result of an aggregated SQL query that groups emotion
 * check-ins by week and calculates the average regret score for each period.
 *
 * It is used to populate the weekly regret trend line chart on the Regret
 * Stats screen, allowing the user to see at a glance whether their regret
 * is improving or worsening over time. A downward trend indicates the user
 * is making more intentional purchase decisions.
 *
 * Example: WeeklyRegret(weekStart = 1748736000000L, avgRegret = 4.5)
 *
 * @property weekStart The timestamp (in milliseconds since epoch) of the
 *                     Monday that begins this week, used as the x-axis
 *                     value on the regret trend chart
 * @property avgRegret The average regret score across all rated purchases
 *                     in this week, on a scale of 1.0 to 10.0
 */
data class WeeklyRegret(
    val weekStart: Long,
    val avgRegret: Double
)