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


