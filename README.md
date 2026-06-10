# Impulse Purchase Recovery Kit 🛍️🧠
A smart Android spending coach that helps you scan receipts, understand your impulse patterns, make intentional financial decision - powered by Claude AI and ML Kit OCR. 

### What is it? 
Most budgeting apps tell you what you spent, but this one asks you how you feel about it.

The Impulse Purchase Recovery Kit combines receipt scanning, AI-powered analysis, emotional reflection, and a conversational spending coach all in one app. The goal is not to stop you spending, it's to make spending a conscious choice rather than a reflex.

### Key Features
### The application is built around a set of unique tools designed to integrate spending data with personal emotional feedback:

### Receipt Scanning
Point your camera at any receipt. The ML Kit OCR extracts the text on the device instantly, the text is then transferred to Claude AI which parses the text into structred data; store name, date, items, quantities and totals. 

### Impulse Scoring 
Every receipt gets an automatic impulse risk score from 0-100, calculated by the device without a network call. The score uses time of day, category, spend amount, and purchase patterns to label each receipt LOW, MEDIUM, or HIGH. 

### Regret Tracking
Rate how you feel about each purchase trip from 1-10. React to individual items (happy/neutral/regret). The app tracks whether your regret trends up or down over a period of time, which is more useful than a raw spend figure. 

### Monthly Budget & Saving Goals
Set a monthly spending limit, visualised as a donut that shifts from green to amber to red as you approach your limit. Add as many saving goals as you want; 'holiday, laptop, trainers' etc. and rank them by priority. The app estimates how many months away each goal is based on your current saving rate.

### KIRA - AI Spending Coach
KIRA is a Claude-powered conversational assistant that processes your purchase history. Ask it "should I buy this?", "how am I doing this month?", or "what do I regret most?" and KIRA will respond with specific, and reliable advice that is aimed to help you make good financial habits, and give you insight of your spending behaviour under a variety of emotions. It applies frameworks like the 24 hour rule, the Cost-Per-Use Test, and the Regret Test automatically. 

### Stats Dashboard
- Weekly spend trend
- Category breakdown
- Average regret trend over time
- High-regret purchase history
















### Tech Stack
| | Technology | Why |
|---|---|---|
| Language | Kotlin | Modern, concise, null-safe |
| UI | Jetpack Compose + Material 3 | Declarative UI, 2025 design system |
| Architecture | MVVM + StateFlow | Reactive, testable, lifecycle-safe |
| Database | Room DB | On-device storage, no user data leaves the phone |
| AI parsing | Anthropic Claude API | Handles messy real-world receipt formats reliably |
| AI chat | Anthropic Claude API | Personalised responses using live spending data |
| OCR | Google ML Kit | Fast, on-device text extraction |
| Charts | Vico | Smooth, composable chart library |
| Images | Coil | Async image loading with pinch-to-zoom |
| Navigation | Jetpack Navigation Compose | Type-safe, single-activity |
| Async | Kotlin Coroutines + Flow | Structured concurrency throughout |


### Architecture 
The app follows a clean MVVM architecture with a repository pattern separating the database from the UI layer.

```
app/
├── MainActivity.kt                  Entry point, receipt scan orchestration
├── ClaudeReceiptParser.kt           Claude API receipt parsing
├── AnthropicApiClient.kt            HTTP client for Anthropic API
├── ImpulseScorer.kt                 On-device impulse scoring engine
├── ParsedReceipt.kt                 Receipt data model
│
├── database/
│   ├── AppDatabase.kt               Room database, version management
│   ├── ReceiptRepository.kt         Single source of truth for all data
│   ├── dao/                         Data Access Objects (6 DAOs)
│   ├── entities/                    Room entities — Receipt, Item, Goal,
│   │                                SavingGoal, Emotion, ItemReaction
│   └── models/                      Query result models (CategorySpend etc.)
│
├── navigation/
│   ├── Screen.kt                    Type-safe navigation routes
│   └── BottomNavItem.kt             Bottom nav configuration
│
├── ui/
│   ├── AppRoot.kt                   Scaffold, NavHost, bottom nav with FAB
│   ├── screens/                     All composable screens (13 screens)
│   ├── charts/                      Vico chart composables
│   └── theme/                       Material 3 colour scheme and typography
│
└── viewmodel/
    ├── ReceiptViewModel.kt           All receipt/goal/stats state
    └── SuggestionBotViewModel.kt     KIRA chat state and system prompt
```
---
## Why On-Device Impulse Scoring? 
The impulse score runs without a network call using written rules. This keeps it fast (instant feedback), consistent (same input always give the same score), and explainable (the users can understand why a purchase is scored HIGH or LOW). Claude is used for conversation purposes, to make users feel comfortable enough to find their underlying trigger for their financial choices.

### Why Room instead of a Remote Database?
Spending data is sensitive, hence why Room is used, it allows everything to stay on the device. The only data that leaves the phone is a receipt text which is sent for AI parsing, this requires a privacy consent before the first scan. 


## Setup
### Conditions
- Android Studio Hedgehog or later
- Android SDK 26+
- An [Anthropic API key](https://console.anthropic.com)

### Installation
 
**1. Clone the repo**
```bash
git clone https://github.com/a-tajarab/ImpulsePurchaseRecoveryKit.git
```
 
**2. Add your API key**
 
Create `local.properties` in the project root (it is gitignored and will never be committed):
```
ANTHROPIC_API_KEY=your_key_here
```
 
**3. Verify `app/build.gradle.kts` exposes the key to BuildConfig**
```kotlin
buildConfigField("String", "ANTHROPIC_API_KEY",
    "\"${localProperties["ANTHROPIC_API_KEY"] ?: ""}\"")
```
 
**4. Run**
 
Open in Android Studio and press Run, or:
```bash
./gradlew assembleDebug
```
---
## Author
 
**Ayesha Tajarab** - Android Developer

--








