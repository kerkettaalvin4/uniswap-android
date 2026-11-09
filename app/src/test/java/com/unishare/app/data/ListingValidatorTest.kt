package com.unishare.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListingValidatorTest {
    @Test fun acceptsValidListingAndZeroPrice() {
        assertNull(ListingValidator.validate("Calculus I", "Clean copy", "Textbooks", 0.0, "Good"))
    }

    @Test fun rejectsMissingOrOversizedTitle() {
        assertEquals("Add a title.", ListingValidator.validate("  ", "", "Dorm", 10.0, "Fair"))
        assertEquals("Keep the title under 80 characters.", ListingValidator.validate("x".repeat(81), "", "Dorm", 10.0, "Fair"))
    }

    @Test fun rejectsInvalidPriceAndLongDescription() {
        assertEquals("Enter a valid price.", ListingValidator.validate("Lamp", "", "Dorm", -0.01, "Good"))
        assertEquals("Enter a valid price.", ListingValidator.validate("Lamp", "", "Dorm", Double.NaN, "Good"))
        assertEquals("Keep the description under 500 characters.", ListingValidator.validate("Lamp", "x".repeat(501), "Dorm", 10.0, "Good"))
    }

    @Test fun rejectsUnknownCategoryAndCondition() {
        assertEquals("Choose a valid category.", ListingValidator.validate("Book", "", "All", 10.0, "Good"))
        assertEquals("Choose a valid condition.", ListingValidator.validate("Book", "", "Textbooks", 10.0, "Broken"))
    }
}
