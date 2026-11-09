package com.unishare.app.data

import com.google.firebase.Timestamp

data class Listing(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Textbooks",
    val price: Double = 0.0,
    val condition: String = "Good",
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerEmail: String = "",
    val collegeName: String = "",
    val createdAt: Timestamp? = null,
    val status: String = "available"
)

val listingCategories = listOf("All", "Textbooks", "Electronics", "Dorm", "Supplies", "Other")
