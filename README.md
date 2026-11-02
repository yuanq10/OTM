# Order Tracker (OTM)

A personal Android app for tracking orders, customers and products. Built with Kotlin and Jetpack Compose. Data is stored in Firebase Cloud Firestore on the free Spark plan, so it is backed up to your Google account and the app still works offline.

## Features
- **Orders:** create an order for a customer with one or more products, and cancel orders.
  - Track four independent statuses for each order (**Paid to me**, **Paid to supplier**, **Shipped**, **Received**), each with its date.
  - Orders are split into **In progress** and **Completed** panels (mark an order completed from its page). In progress can be filtered by missing status; Completed can be filtered by the date range it was completed in. Canceled orders are hidden from the Orders page but kept in the customer's history.
  - Prices are locked in when the order is placed.
- **Customers:** identified by first and last name. You can add, edit and remove them, and each customer has a full order history.
- **Products:** add, edit and remove products. Prices are editable, and a product can be put on sale with a sale price.
- **Removing** a customer or product archives it. Past orders keep showing it, but it can't be picked for new orders, and it can be restored from "Show archived".
- **Settings:** choose the app language (same as phone, English, Français or 简体中文), choose the currency symbol shown on prices (US dollar, Canadian dollar or Chinese yuan; only the symbol changes, amounts aren't converted), and see which Google account you're signed in with, with a Logout button.

## One-time setup
1. Install [Android Studio](https://developer.android.com/studio). It includes the JDK, Android SDK and emulator.
2. In the [Firebase console](https://console.firebase.google.com) (project `otms-600ac`):
   1. Go to **Build → Firestore Database** and create a database.
   2. Open the **Rules** tab, paste in the contents of [firestore.rules](firestore.rules), and click **Publish**.
   3. Go to **Build → Authentication → Sign-in method** and enable **Google**.
   4. Get your debug SHA-1 by running `gradlew signingReport` in Android Studio's terminal. In **Project settings → Your apps → Android app**, click **Add fingerprint** and paste the SHA-1.
   5. Download `google-services.json` again and replace `app/google-services.json`. The new file includes the Google sign-in client ID.
3. Open this folder in Android Studio, wait for Gradle sync, then click **Run** with an emulator or a phone connected over USB debugging.

`google-services.json` is in `.gitignore` on purpose, so it is never committed.

## Running the tests
```
gradlew test
```
The tests cover money parsing and formatting, order totals (including sale prices and locked-in prices), and the status rules:
- An order must be shipped before it can be received.
- Shipped can't be turned off while the order is received.
- Canceled orders are read-only.

## Translations
All on-screen text lives in `app/src/main/res/values/strings.xml` (English), with translations in `values-fr/` (French) and `values-zh-rCN/` (Simplified Chinese). To add a string, add it to all three files. To add a language, create another `values-<language>/strings.xml`, list it in `res/xml/locales_config.xml`, and add it to the list in `ui/settings/SettingsScreen.kt`.

## Project layout
- `app/src/main/java/com/otm/ordertracker/data/model/`: Customer, Product, Order, plus the order rules (`OrderRules`, `OrderFilter`)
- `data/repo/`: Firestore repositories (all data lives under `users/{uid}/…`) and Google sign-in
- `ui/orders`, `ui/customers`, `ui/products`: screens and their ViewModels
- `ui/navigation/OtmNavHost.kt`: routes and the bottom navigation bar
