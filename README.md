# Impulse Purchase Recovery Kit 🛍️🧠
An Android application designed to help users process, reflect on, and learn from their impulse spending habits by blending practical finance tracking with emotional awareness and a touch of humor.

### 🎯 Project Aim
The core aim of the Impulse Purchase Recovery Kit is to provide a space where users can be more organized with their finances while simultaneously raising emotional awareness. By using humor and compassionate reflection, the app encourages users to make healthier, more intentional spending decisions and learn from past purchases without judgment.

## ✨ Core Features
### The application is built around a set of unique tools designed to integrate spending data with personal emotional feedback:

### 🧾 Receipt Scanner (OCR)
Effortlessly logs purchases by scanning physical receipts, translating the purchase data directly into the app's database using Optical Character Recognition (OCR).

### 💔 Regret Score System 
Allows users to log how they truly feel about a purchase using a customizable regret scale. This metric is key to analyzing the emotional impact of spending.

### 📈 Impulse Tracker
Visualizes user purchases on a timeline in a graph format, providing a clear trend of spending habits over time.

### 🤖 Suggestion Bot
An interactive feature where users can inquire about a recent or future purchase. The bot provides personalized, prompt advice based on the user's recorded spending patterns and financial status (e.g., "Maybe wait 24 hours?").

### 🫂 Emotional Response Engine
A critical support feature. If a user logs an exceptionally high regret score, the engine immediately prompts a response to comfort the user, encourage a positive outlook, and provide actionable, gentle advice on how to avoid a similar situation next time.


#### Sep - Oct 
Core Setup + OCR Foundation

Task Details

🔹Android Studio Setup - Created a new project in Kotlin. ✅

🔹A ML Kit dependency is added and is implemented 'com.google.mlkit:text-recognition:16.0.0'✅

🔹Built an image input flow	to allow user capture or upload receipt by using camera & storage.✅

🔹Implemented the OCR to process and used the ML Kit to extract text from receipt and log the order to Logcat.✅

🔹Parsed text into key fields it extracts the item name, price, quantity, total amount, and the store name.~ somewhat completed

🔹It stores sample outputs and saves the OCR text as CSV for quick testing. ✅


#### Nov - Dec
Data Storage + App Skeleton (Backend + Basic UI)

Task Details

🔹 Create Room Database	Entities: Receipt, Item, Emotion.✅

🔹 Implement DAOs & Repository	Handle insert, update, delete, query.✅

🔹 Build ViewModel	To bridge UI ↔ data safely.✅

🔹 Create basic screens	Home, Scan, Results, History.✅

🔹 Connect OCR to database	After scanning, save extracted receipt text to Room DB. ~ to some extent

🔹 Add navigation	Use Navigation Component or simple intents.✅


#### Jan - Feb
Emotional Input + Analytics Dashboard

Task Details

🔹 Added a regret score screen with a slider (1–10) and a text box for feelings (“Why did I buy this?”).~ slider is completed + text box is there but can be improved 

🔹 Linked the regret score to the receipts - Each receipt has 1 emotional entry. 

🔹 Added “Did You Know?” popups, added random facts from my literature data. ~ very basic, also needs enhancing

🔹 Built an analytics dashboard that shows expenditure by category, average regret, etc. ~ most products from receipts have been categorised but there can be more added 

🔹 Use chart library	MPAndroidChart or Compose Charts.



#### Mar - Apr
Design, Testing & Polish

Task Details

🔹 Design final UI	Use consistent color palette, icons, typography.

🔹 Add animations	Simple transitions for smooth experience.

🔹 Add splash & onboarding screens	Short intro explaining the app’s purpose.

🔹 Add error handling	Handle OCR failures, blank fields, missing permissions.

🔹 Test on multiple devices	Check performance, layout, and bugs.

🔹 Final demo recording	Record a short walkthrough video for documentation.

--
--
--








