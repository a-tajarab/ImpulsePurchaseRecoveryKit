package com.example.impulsepurchaserecoverykit.database.models

data class CategorySpend(
    val category: String,
    val total: Double
)

data class CategoryCount(
    val category: String,
    val count: Int
)