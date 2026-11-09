package com.unishare.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unishare.app.data.Listing
import com.unishare.app.data.ListingValidator
import com.unishare.app.data.MarketplaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class MarketplaceState(
    val uid: String? = null,
    val email: String = "",
    val displayName: String = "",
    val collegeName: String = "",
    val listings: List<Listing> = emptyList(),
    val savedIds: Set<String> = emptySet(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class MarketplaceViewModel(private val repo: MarketplaceRepository = MarketplaceRepository()) : ViewModel() {
    private val _state = MutableStateFlow(MarketplaceState())
    val state = _state.asStateFlow()
    private var listingsJob: Job? = null
    private var savedJob: Job? = null

    init {
        _state.value = _state.value.copy(uid = repo.user?.uid, email = repo.user?.email.orEmpty(), displayName = repo.user?.displayName.orEmpty())
        _state.value.uid?.let { uid ->
            viewModelScope.launch {
                runCatching { repo.getCollege(uid) }.onSuccess { _state.value = _state.value.copy(collegeName = it) }
            }
            observeMarketplace()
        }
    }

    private fun observeMarketplace() {
        listingsJob?.cancel()
        listingsJob = viewModelScope.launch {
            repo.observeListings().collectLatest { result ->
                result.onSuccess { _state.value = _state.value.copy(listings = it, loading = false, error = null) }
                    .onFailure { _state.value = _state.value.copy(loading = false, error = message(it)) }
            }
        }
        val uid = _state.value.uid ?: return
        savedJob?.cancel()
        savedJob = viewModelScope.launch {
            repo.observeSavedIds(uid).collectLatest { result ->
                result.onSuccess { _state.value = _state.value.copy(savedIds = it) }
                    .onFailure { _state.value = _state.value.copy(error = message(it)) }
            }
        }
    }

    fun signIn(email: String, password: String) = action {
        repo.signIn(email, password); refreshUser(); "Welcome back"
    }

    fun register(name: String, email: String, password: String, collegeName: String) = action {
        repo.createAccount(name, email, password, collegeName); refreshUser(); "Your account is ready"
    }

    private suspend fun refreshUser() {
        val uid = repo.user?.uid
        val collegeName = uid?.let { runCatching { repo.getCollege(it) }.getOrDefault("") }.orEmpty()
        _state.value = _state.value.copy(uid = uid, email = repo.user?.email.orEmpty(), displayName = repo.user?.displayName.orEmpty(), collegeName = collegeName, savedIds = emptySet(), listings = emptyList(), loading = true)
        observeMarketplace()
    }

    fun saveCollege(collegeName: String) = action {
        val uid = _state.value.uid ?: error("Sign in to update your college")
        val cleaned = collegeName.trim()
        if (cleaned.length < 3 || cleaned.length > 120) error("Enter a college name between 3 and 120 characters.")
        repo.updateCollege(uid, cleaned)
        _state.value = _state.value.copy(collegeName = cleaned)
        "College saved"
    }

    fun signOut() {
        listingsJob?.cancel(); listingsJob = null
        savedJob?.cancel(); savedJob = null
        repo.signOut()
        _state.value = _state.value.copy(uid = null, email = "", displayName = "", collegeName = "", listings = emptyList(), savedIds = emptySet(), loading = true, error = null, message = "Signed out")
    }

    fun addListing(title: String, description: String, category: String, price: Double, condition: String) = action {
        ListingValidator.validate(title, description, category, price, condition)?.let { error(it) }
        val college = _state.value.collegeName.trim()
        if (college.isBlank()) error("Add your college in Profile before posting a listing.")
        val created = repo.createListing(title, description, category, price, condition, college)
        _state.value = _state.value.copy(listings = (listOf(created) + _state.value.listings.filterNot { it.id == created.id }).sortedByDescending { it.createdAt?.seconds ?: 0L })
        "Listing posted"
    }

    fun toggleSaved(id: String) {
        val uid = _state.value.uid ?: return
        val save = id !in _state.value.savedIds
        viewModelScope.launch {
            runCatching { repo.toggleSaved(uid, id, save) }.onFailure { _state.value = _state.value.copy(error = message(it)) }
        }
    }

    fun deleteListing(id: String) = action { repo.removeListing(id); "Listing removed" }

    fun clearNotice() { _state.value = _state.value.copy(error = null, message = null) }

    private fun action(block: suspend () -> String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null, message = null)
            runCatching { block() }.onSuccess { _state.value = _state.value.copy(busy = false, message = it) }
                .onFailure { _state.value = _state.value.copy(busy = false, error = message(it)) }
        }
    }

    private fun message(error: Throwable) = when {
        error.message?.contains("PERMISSION_DENIED", true) == true -> "Access denied. Check your Firebase rules."
        error.message?.contains("network", true) == true -> "Network problem. Check your connection and try again."
        else -> error.localizedMessage ?: "Something went wrong. Please try again."
    }
}
