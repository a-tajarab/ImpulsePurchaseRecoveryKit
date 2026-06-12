package com.example.impulsepurchaserecoverykit.database.models

/**
 * Query result model representing the total amount spent within a single
 * spending category.
 *
 * CategorySpend is not a full Room entity — it is a lightweight data class
 * used to hold the result of an aggregated SQL query that groups items by
 * category and sums their totals. It is used to populate the Category Stats
 * bar chart on the Stats screen, giving the user a clear breakdown of where
 * their money is going each month.
 *
 * Example: CategorySpend(category = "accessories", total = 318.99)
 *
 * @property category The name of the spending category, for example
 *                    "tops", "beverage", "accessories", or "other"
 * @property total The total amount spent across all items in this category,
 *                 in GBP
 */
data class CategorySpend(
    val category: String,
    val total: Double
)

/**
 * Query result model representing the number of receipts within a single
 * spending category.
 *
 * CategoryCount is not a full Room entity — it is a lightweight data class
 * used to hold the result of an aggregated SQL query that groups receipts
 * by category and counts the number of entries in each group. It is used
 * alongside [CategorySpend] to provide frequency context on the Stats screen,
 * helping the user understand not just how much they spent in a category
 * but how often they shop there.
 *
 * Example: CategoryCount(category = "accessories", count = 3)
 *
 * @property category The name of the spending category, for example
 *                    "tops", "beverage", "accessories", or "other"
 * @property count The number of receipts or items that fall within this category
 */
data class CategoryCount(
    val category: String,
    val count: Int
)