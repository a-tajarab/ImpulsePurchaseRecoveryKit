# Module Impulse Purchase Recovery Kit

An Android app for tracking impulse purchases, rating regret, and getting AI spending advice from KIRA.

# Package com.example.impulsepurchaserecoverykit
Core app — MainActivity, Claude parser, impulse scorer, receipt parser.

# Package com.example.impulsepurchaserecoverykit.database
Room database — AppDatabase, ReceiptRepository.

# Package com.example.impulsepurchaserecoverykit.database.dao
Data Access Objects for all Room queries.

# Package com.example.impulsepurchaserecoverykit.database.entities
Room entity classes — ReceiptEntity, ItemEntity, GoalEntity, etc.

# Package com.example.impulsepurchaserecoverykit.database.models
Query result models — CategorySpend, WeeklySpend, WeeklyRegret.

# Package com.example.impulsepurchaserecoverykit.navigation
Navigation routes and bottom nav item definitions.

# Package com.example.impulsepurchaserecoverykit.ui
AppRoot — the top-level composable owning the NavHost.

# Package com.example.impulsepurchaserecoverykit.ui.screens
All app screens — Home, Stats, KIRA, Receipt detail, Spending goal, etc.

# Package com.example.impulsepurchaserecoverykit.ui.charts
Custom Compose chart composables — spend, regret, and category charts.

# Package com.example.impulsepurchaserecoverykit.ui.theme
Material 3 colour palette, typography, and theme configuration.

# Package com.example.impulsepurchaserecoverykit.viewmodel
ReceiptViewModel and SuggestionBotViewModel.

# Package com.example.impulsepurchaserecoverykit.debug
Debug-only tools for generating and rendering synthetic test receipts.
