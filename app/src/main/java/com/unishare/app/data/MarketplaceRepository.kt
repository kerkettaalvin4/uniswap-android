package com.unishare.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MarketplaceRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val user get() = auth.currentUser

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeListings() = callbackFlow<Result<List<Listing>>> {
        val registration = db.collection("listings")
            .whereEqualTo("status", "available")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) trySend(Result.failure(error))
                else trySend(Result.success(snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.toObject(Listing::class.java)?.copy(id = doc.id)
                }))
            }
        awaitClose { registration.remove() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeSavedIds(uid: String) = callbackFlow<Result<Set<String>>> {
        val registration = db.collection("users").document(uid).collection("saved")
            .addSnapshotListener { snapshot, error ->
                if (error != null) trySend(Result.failure(error))
                else trySend(Result.success(snapshot?.documents.orEmpty().map { it.id }.toSet()))
            }
        awaitClose { registration.remove() }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun createAccount(name: String, email: String, password: String, collegeName: String) {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val uid = result.user?.uid ?: error("Account could not be created")
        result.user?.updateProfile(com.google.firebase.auth.UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build())?.await()
        db.collection("users").document(uid).set(mapOf("displayName" to name.trim(), "email" to email.trim(), "collegeName" to collegeName.trim(), "createdAt" to System.currentTimeMillis())).await()
    }

    fun signOut() = auth.signOut()

    suspend fun updateCollege(uid: String, collegeName: String) {
        db.collection("users").document(uid).update("collegeName", collegeName.trim()).await()
    }

    suspend fun getCollege(uid: String): String =
        db.collection("users").document(uid).get().await().getString("collegeName").orEmpty()

    suspend fun createListing(title: String, description: String, category: String, price: Double, condition: String, collegeName: String): Listing {
        val user = auth.currentUser ?: error("Sign in to post a listing")
        val ref = db.collection("listings").document()
        val listing = Listing(
            id = ref.id, title = title.trim(), description = description.trim(), category = category,
            price = price, condition = condition, sellerId = user.uid,
            sellerName = (user.displayName?.takeIf { it.isNotBlank() } ?: user.email?.substringBefore("@") ?: "Student").take(80),
            sellerEmail = user.email ?: "", collegeName = collegeName.trim(),
            createdAt = com.google.firebase.Timestamp.now()
        )
        ref.set(mapOf(
            "title" to title.trim(), "description" to description.trim(), "category" to category,
            "price" to price, "condition" to condition, "sellerId" to user.uid,
            "sellerName" to listing.sellerName, "sellerEmail" to listing.sellerEmail,
            "collegeName" to collegeName.trim(), "createdAt" to FieldValue.serverTimestamp(), "status" to "available"
        )).await()
        return listing
    }

    suspend fun toggleSaved(uid: String, listingId: String, save: Boolean) {
        val ref = db.collection("users").document(uid).collection("saved").document(listingId)
        if (save) ref.set(mapOf("savedAt" to System.currentTimeMillis())).await() else ref.delete().await()
    }

    suspend fun removeListing(listingId: String) {
        db.collection("listings").document(listingId).delete().await()
    }
}
