# UniSwap

**A campus-first marketplace for giving useful student essentials a second life.** UniSwap helps students find affordable textbooks, electronics, dorm items, and supplies from other students. It is a complete native Android application built with Kotlin, Jetpack Compose, Firebase Authentication, and Cloud Firestore.

## Features

- Email and password account creation and sign-in with Firebase Authentication.
- Live marketplace listings from Cloud Firestore, with category filters and text search.
- College/university on each account and listing, with an Explore filter for all colleges or any campus represented in the marketplace.
- Create a listing with title, description, category, condition, and price validation.
- Save and unsave listings in a private per-user collection.
- Contact a seller using the device's email app.
- View your own posts and remove a post you own.
- Profile navigation has an explicit back action and handles Android's system Back button.
- Prices use Indian Rupees (INR), and the app includes a custom reuse-themed launcher and in-app mark.
- Loading, empty, validation, network, and Firebase permission error states.
- Firestore's Android client persistence supports offline cache reads and queued writes; the app surfaces sync errors rather than claiming a write succeeded before Firestore confirms it.
- Firestore Security Rules validate fields and enforce ownership on the server.

## Stack

- Kotlin, Android SDK 24+, Jetpack Compose, Material 3
- MVVM-style `MarketplaceViewModel` + repository boundary
- Firebase Authentication (email/password)
- Cloud Firestore (real-time snapshots and local persistence)
- Gradle Kotlin DSL and version catalog

## Architecture

```text
Compose UI (MainActivity)
        │ observes StateFlow / sends actions
MarketplaceViewModel (screen state, validation feedback, coroutines)
        │
MarketplaceRepository (Firebase Auth + Firestore operations)
        │
Firebase Authentication ── Cloud Firestore ── Security Rules
```

The app keeps presentation state in one immutable `MarketplaceState`. Firestore listeners feed listings and saved IDs as observable streams. Newly posted listings are added to local screen state after Firestore confirms the write, while the snapshot listener remains the source of truth for later updates. Account and ownership checks are enforced again in Firestore rules, so UI state is not treated as a security boundary. Prices display in Indian Rupees.

## Firebase setup

1. In [Firebase Console](https://console.firebase.google.com/), create a Firebase project.
2. Add an **Android app** with package name `com.unishare.app` (the package must match exactly).
3. Download `google-services.json` and put it at `app/google-services.json`. This file contains project identifiers, not a service-account private key; it is excluded by `.gitignore` here.
4. In **Build > Authentication > Sign-in method**, enable **Email/Password**.
5. Create a **Cloud Firestore** database. Start in production mode.
6. In Firestore **Rules**, paste the contents of [`firestore.rules`](firestore.rules) and publish. Republish the rules whenever this file changes. The same rules and composite index are also declared in [`firebase.json`](firebase.json) and [`firestore.indexes.json`](firestore.indexes.json).
7. Open this folder in Android Studio, install Android SDK 36 if prompted, sync Gradle, and run on an emulator/device with Google Play services and network access.

The first query may prompt Firestore to create a composite index for `status` + `createdAt`. If Firebase shows a link in the error, open it to create the suggested index. Authenticated users can browse listings. Listing documents are public to signed-in app users; only the seller can delete or change the status. Saved IDs and profile data are readable only by their owner. No privileged service account or server key is used in the app.

## Run

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

Open this folder in Android Studio if you prefer the IDE. The Gradle wrapper is included and pins Gradle 9.3.1. Android Gradle Plugin 9.1.1 supports Android API 37; Android Studio's embedded JDK 17 or a separate JDK 17+ is required. Install SDK Platform 37 and Build Tools if Android Studio prompts you.

`ListingValidatorTest` covers required titles, input boundaries, zero/negative/non-finite prices, descriptions, categories, and conditions. The project owner has confirmed that the updated app builds, launches, and its main user flows work on a connected Android device. Automated JVM tests have not been confirmed; run them with `.\gradlew.bat testDebugUnitTest` before publishing if you want to include a test result.

## Screenshots checklist

Capture these in Android Studio's emulator after Firebase setup:

1. Sign-in / create-account screen, including college entry.
2. Explore screen showing search, category and college filters, and multiple listings with INR prices.
3. Create listing dialog with validation feedback.
4. Saved items and My posts tabs.
5. Profile screen showing saved college and the return-to-marketplace action.

Use realistic demo listings, but do not put personal student data or real email addresses in public screenshots. Add selected images under `docs/screenshots/` and embed them here before publishing your GitHub repository.

## GitHub showcase

The source repository is [`kerkettaalvin4/uniswap-android`](https://github.com/kerkettaalvin4/uniswap-android). To get a local copy:

```bash
git clone https://github.com/kerkettaalvin4/uniswap-android.git
```

Add your own Firebase `app/google-services.json` after cloning; the file is intentionally excluded from Git. Never commit a service-account JSON file or signing key.

## Resume entry

**UniSwap — Android Developer (Personal Project)** | Kotlin, Jetpack Compose, Firebase Authentication, Cloud Firestore

- Built a native campus marketplace with authenticated accounts, live listings, search and category filters, saved items, and seller contact.
- Designed a repository + ViewModel architecture with reactive UI state and Firestore offline persistence.
- Wrote Firestore Security Rules that validate listing fields, use server timestamps to prevent client-controlled listing order, and restrict profile/saved-item access and listing deletion to the owning user.

Keep this entry truthful: add emulator/device testing details and screenshots after you complete Firebase setup and verify the app end to end.

## Interview talking points

**Why Firebase?** It fits the project's real-time marketplace and the constraints of shipping without operating a custom server. Authentication and Firestore provide managed identity and persistence; rules are the authorization boundary.

**How is data access protected?** The client sends the signed-in Firebase UID with each request. Firestore rules require authentication, match `sellerId` to the authenticated UID for writes/deletes, and scope profile/saved paths to that same UID. Client-side UI checks are only presentation.

**How does the UI stay current?** A Firestore snapshot listener emits listing updates; the ViewModel converts them into immutable `StateFlow` state consumed by Compose. Listener registrations are removed when their flow is cancelled.

**What would you improve next?** Add listing photos with Firebase Storage and content moderation/abuse reporting, a sold-state workflow, pagination for a large campus, and Emulator Suite rule tests. A production deployment should also decide whether listings are campus-email restricted and add rate limiting/abuse controls through trusted backend logic or App Check.

## Current scope and limitations

- Listings are available to signed-in users of the configured Firebase project; campus-domain restrictions are not enabled by default.
- Contact starts an email draft; negotiation and payment happen outside the app.
- There is no image upload, chat, payment, or moderation backend in this first release.
- Firebase setup and a real-device/emulator smoke test are required before calling the app verified or publishing screenshots.
