# Order Tracker (OTM)

A personal Android app for tracking orders, customers and products. Built with Kotlin and Jetpack Compose. Data is stored in Firebase Cloud Firestore on the free Spark plan, so it is backed up to your Google account and the app still works offline.

## Download
**[Order Tracker 1.0 (APK, Google Drive)](https://drive.google.com/file/d/1qNWErVGG6vb7320SwYh0NuLFdePKfP0L/view?usp=sharing)**: requires Android 8.0 or newer.

To install, open the link on your Android phone, download the APK and tap it. Allow "Install unknown apps" for your browser or Files app when asked. Then sign in with your Google account. If an older version of Order Tracker installed from Android Studio is on the phone, uninstall it first.

## Features
- **Orders:** create an order for a customer with one or more products, and cancel orders.
  - Track four independent statuses for each order (**Paid to me**, **Paid to supplier**, **Shipped**, **Received**), each with its date.
  - Orders are split into **In progress** and **Completed** panels (mark an order completed from its page). In progress can be filtered by missing status; Completed can be filtered by the date range it was completed in. Canceled orders are hidden from the Orders page but kept in the customer's history.
  - Prices are locked in when the order is placed.
- **Customers:** identified by first and last name. You can add, edit and remove them, and each customer has a full order history.
- **Products:** add, edit and remove products. Prices are editable, and a product can be put on sale with a sale price. Each product is marked **In stock** or **Out of stock**, shown as a badge in the product list and when picking products for an order.
- **Removing** a customer or product archives it. Past orders keep showing it, but it can't be picked for new orders, and it can be restored from "Show archived".
- **Settings:** choose the app language (English by default, Français or 简体中文) and the currency symbol shown on prices (US dollar by default, Canadian dollar or Chinese yuan; only the symbol changes, amounts aren't converted). Both are saved with your account, so they follow you to any device you sign in on. Also shows which Google account you're signed in with, with a Logout button. The app never reads your location.

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
The tests cover:
- Money parsing and formatting, including French-style "12,50" and the currency symbol.
- Order totals, including sale prices, locked-in prices and editing an order.
- Order statuses (independent, any order), completing and reopening, and canceled orders being read-only.
- Search by text, customer, product and completion date range.
- Chinese and French names being stored unchanged and found by search.

## Building a release APK
1. The release signing key is `C:\Users\logan\.android\otm-release.jks`. Its password is in `keystore.properties` in the project root. Both are git-ignored. **Back them up somewhere safe.** Without them you can't publish updates that install over this app.
2. The key's SHA-1 and SHA-256 fingerprints must be added in Firebase (Project settings → Your apps → Add fingerprint), or Google sign-in won't work in the release app.
3. Build with `gradlew assembleRelease`. The APK is written to `app/build/outputs/apk/release/app-release.apk`.
4. Before each new release, raise `versionCode` (and `versionName`) in `app/build.gradle.kts` so it installs as an update.

## Translations
All on-screen text lives in `app/src/main/res/values/strings.xml` (English), with translations in `values-fr/` (French) and `values-zh-rCN/` (Simplified Chinese). To add a string, add it to all three files. To add a language, create another `values-<language>/strings.xml`, list it in `res/xml/locales_config.xml`, and add it to the list in `ui/settings/SettingsScreen.kt`.

## Project layout
- `app/src/main/java/com/otm/ordertracker/data/model/`: Customer, Product, Order, plus the order rules (`OrderRules`, `OrderFilter`)
- `data/repo/`: Firestore repositories (all data lives under `users/{uid}/…`) and Google sign-in
- `ui/orders`, `ui/customers`, `ui/products`: screens and their ViewModels
- `ui/navigation/OtmNavHost.kt`: routes and the bottom navigation bar
