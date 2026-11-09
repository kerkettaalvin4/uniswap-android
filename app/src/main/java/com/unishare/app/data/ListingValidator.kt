package com.unishare.app.data

object ListingValidator {
    private val categories = listingCategories.filterNot { it == "All" }.toSet()
    private val conditions = setOf("Like new", "Good", "Fair", "For parts")

    fun validate(title: String, description: String, category: String, price: Double?, condition: String): String? = when {
        title.isBlank() -> "Add a title."
        title.trim().length > 80 -> "Keep the title under 80 characters."
        description.length > 500 -> "Keep the description under 500 characters."
        category !in categories -> "Choose a valid category."
        price == null || !price.isFinite() || price < 0 -> "Enter a valid price."
        condition !in conditions -> "Choose a valid condition."
        else -> null
    }
}
