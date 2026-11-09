package com.unishare.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unishare.app.data.Listing
import com.unishare.app.data.ListingValidator
import com.unishare.app.data.listingCategories
import com.unishare.app.ui.MarketplaceState
import com.unishare.app.ui.MarketplaceViewModel
import com.google.firebase.FirebaseApp
import java.text.NumberFormat
import java.util.Locale

private val Ink = Color(0xFF17241F)
private val Green = Color(0xFF176B50)
private val Canvas = Color(0xFFF6F7F4)
private val Muted = Color(0xFF738078)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(246, 247, 244)
        window.navigationBarColor = android.graphics.Color.rgb(246, 247, 244)
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val firebaseReady = FirebaseApp.initializeApp(this) != null
        setContent { UniShareTheme { if (firebaseReady) UniShareApp() else FirebaseSetupRequired() } }
    }
}

@Composable
private fun UniShareTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, onPrimary = Color.White, background = Canvas, surface = Color.White, onSurface = Ink, secondary = Color(0xFFE6F1E9)), content = content)
}

@Composable
private fun UniShareApp(vm: MarketplaceViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var tab by remember { mutableStateOf("Explore") }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var selectedCollege by remember { mutableStateOf("All colleges") }
    var collegeMenu by remember { mutableStateOf(false) }
    var showPost by remember { mutableStateOf(false) }
    var snackbar by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = tab == "Account") { tab = "Explore" }

    LaunchedEffect(state.uid) {
        if (state.uid != null) {
            tab = "Explore"
            selectedCollege = "All colleges"
        }
    }

    LaunchedEffect(state.message, state.error) {
        if (state.uid != null) {
            if (state.message == "Listing posted") showPost = false
            snackbar = state.message ?: state.error
            if (state.message != null || state.error != null) vm.clearNotice()
        }
    }

    if (state.uid == null) {
        AuthScreen(state, onSignIn = vm::signIn, onRegister = vm::register)
        return
    }

    val visible = state.listings.filter { listing ->
        (tab != "Explore" || category == "All" || listing.category == category) &&
            (tab != "Explore" || selectedCollege == "All colleges" || listing.collegeName.equals(selectedCollege, ignoreCase = true)) &&
            (search.isBlank() || listOf(listing.title, listing.description, listing.category).any { it.contains(search, true) }) &&
            when (tab) {
                "Saved" -> listing.id in state.savedIds
                "My posts" -> listing.sellerId == state.uid
                else -> true
            }
    }
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(snackbar) { snackbar?.let { snackbarHost.showSnackbar(it); snackbar = null } }

    Scaffold(
        containerColor = Canvas,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            Column(Modifier.fillMaxWidth().background(Canvas).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    BrandMark()
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("UniSwap", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
                        Text("A better campus starts here", style = MaterialTheme.typography.labelMedium, color = Muted)
                    }
                    IconButton(onClick = { if (tab == "Account") tab = "Explore" else tab = "Account" }) {
                        Icon(if (tab == "Account") Icons.Default.ArrowBack else Icons.Default.AccountCircle, if (tab == "Account") "Back to marketplace" else "Profile", tint = Green)
                    }
                }
                if (tab != "Account") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Explore", "Saved", "My posts").forEach { item ->
                            FilterChip(selected = tab == item, onClick = { tab = item }, label = { Text(item) }, leadingIcon = if (item == "Saved") ({ Icon(Icons.Default.Favorite, null, Modifier.size(16.dp)) }) else null)
                        }
                    }
                    OutlinedTextField(
                        value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        placeholder = { Text("Search books, supplies, essentials…") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = { if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Default.Close, "Clear search") } },
                        singleLine = true, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8E3), focusedBorderColor = Green)
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab != "Account") ExtendedFloatingActionButton(onClick = { showPost = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Sell something") }, containerColor = Green, contentColor = Color.White)
        }
    ) { padding ->
        when (tab) {
            "Account" -> AccountScreen(state, onSignOut = vm::signOut, onSaveCollege = vm::saveCollege, modifier = Modifier.padding(padding))
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                if (tab == "Explore") {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 3.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listingCategories.forEach { item -> FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) }) }
                    }
                    val colleges = (state.listings.map { it.collegeName.trim() } + state.collegeName.trim())
                        .filter { it.isNotBlank() }.distinctBy { it.lowercase(Locale.ROOT) }.sortedBy { it.lowercase(Locale.ROOT) }
                    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
                        OutlinedButton(onClick = { collegeMenu = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.School, null, tint = Green)
                            Spacer(Modifier.width(8.dp))
                            Text(selectedCollege, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ExpandMore, null)
                        }
                        DropdownMenu(expanded = collegeMenu, onDismissRequest = { collegeMenu = false }) {
                            (listOf("All colleges") + colleges).forEach { collegeName ->
                                DropdownMenuItem(text = { Text(collegeName) }, onClick = { selectedCollege = collegeName; collegeMenu = false })
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Good finds, close by.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                            Text("Pass it on. Find what you need.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                        }
                        Surface(color = Color(0xFFE4F1E8), shape = CircleShape) { Text("CAMPUS", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = Green, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    }
                } else {
                    Text(if (tab == "Saved") "Your saved finds" else "Your listings", Modifier.padding(horizontal = 22.dp, vertical = 14.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                when {
                    state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Green) }
                    state.error != null && state.listings.isEmpty() -> EmptyState("Couldn’t load the marketplace", "Check your connection and Firebase setup.", Icons.Default.CloudOff)
                    visible.isEmpty() -> EmptyState(
                        when (tab) { "Saved" -> "No saved items yet"; "My posts" -> "Your shelf is waiting"; else -> "No matches found" },
                        when (tab) { "Saved" -> "Tap the heart on anything you’d like to keep."; "My posts" -> "Post something useful to your campus."; else -> "Try another search or category." },
                        if (tab == "Saved") Icons.Default.FavoriteBorder else Icons.Default.Inventory2
                    )
                    else -> LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        items(visible, key = { it.id }) { listing ->
                            ListingCard(listing, saved = listing.id in state.savedIds, own = listing.sellerId == state.uid,
                                onSave = { vm.toggleSaved(listing.id) },
                                onContact = {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", listing.sellerEmail, null))
                                    intent.putExtra(Intent.EXTRA_SUBJECT, "UniSwap: ${listing.title}")
                                    runCatching { context.startActivity(intent) }.onFailure { snackbar = "No email app is available on this device." }
                                }, onDelete = { vm.deleteListing(listing.id) })
                        }
                    }
                }
            }
        }
    }
    if (showPost) PostListingDialog(busy = state.busy, collegeName = state.collegeName, onDismiss = { if (!state.busy) showPost = false }, onSubmit = { title, desc, cat, price, condition -> vm.addListing(title, desc, cat, price, condition) })
}

@Composable
private fun BrandMark() {
    Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Green), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Image(painterResource(R.drawable.ic_uniswap_mark), contentDescription = "UniSwap reuse logo", modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun FirebaseSetupRequired() {
    Box(Modifier.fillMaxSize().background(Canvas), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark()
            Spacer(Modifier.height(18.dp))
            Text("Connect your Firebase project", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(9.dp))
            Text("Add your Firebase configuration to enable sign-in and the live campus marketplace.", color = Muted)
            Spacer(Modifier.height(18.dp))
            Text("1. Create an Android app in Firebase Console with package com.unishare.app.\n2. Enable Email/Password Authentication and create Firestore.\n3. Download google-services.json into the app/ folder.\n4. Sync Gradle and launch again.", color = Ink, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ListingCard(listing: Listing, saved: Boolean, own: Boolean, onSave: () -> Unit, onContact: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(listing.category.uppercase(), color = Green, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("·", color = Muted)
                        Text(listing.condition, color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(listing.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Ink)
                }
                Text(money(listing.price), style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
            }
            if (listing.description.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(listing.description, color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF0F2EF))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE8F1EB)), contentAlignment = Alignment.Center) {
                    Text(listing.sellerName.take(1).uppercase(), color = Green, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f).padding(start = 9.dp)) {
                    Text(if (own) "You" else listing.sellerName, style = MaterialTheme.typography.labelLarge, color = Ink, fontWeight = FontWeight.Medium)
                    Text(listing.collegeName.ifBlank { "College not set" }, style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (own) {
                    TextButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.DeleteOutline, null); Spacer(Modifier.width(3.dp)); Text("Remove") }
                } else {
                    IconButton(onClick = onSave) { Icon(if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "${if (saved) "Unsave" else "Save"} listing", tint = if (saved) Color(0xFFC64D63) else Muted) }
                    FilledTonalButton(onClick = onContact, contentPadding = PaddingValues(horizontal = 13.dp)) { Icon(Icons.Default.MailOutline, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Contact") }
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Remove this listing?") }, text = { Text("Students will no longer see it in the marketplace.") }, confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Remove", color = Color(0xFFB33D4B)) } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep it") } })
}

@Composable
private fun AuthScreen(state: MarketplaceState, onSignIn: (String, String) -> Unit, onRegister: (String, String, String, String) -> Unit) {
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var collegeName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(Canvas).imePadding(), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark()
            Spacer(Modifier.height(17.dp))
            Text("Good things find\na second home.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(9.dp))
            Text("Your campus marketplace for the things\nthat make student life easier.", color = Muted)
            Spacer(Modifier.height(27.dp))
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text(if (register) "Create your account" else "Welcome back", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (register) OutlinedTextField(name, { name = it; localError = null }, Modifier.fillMaxWidth(), label = { Text("Your name") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                    if (register) OutlinedTextField(collegeName, { collegeName = it; localError = null }, Modifier.fillMaxWidth(), label = { Text("College / university") }, singleLine = true, leadingIcon = { Icon(Icons.Default.School, null) }, shape = RoundedCornerShape(14.dp))
                    OutlinedTextField(email, { email = it; localError = null }, Modifier.fillMaxWidth(), label = { Text("College email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = RoundedCornerShape(14.dp))
                    OutlinedTextField(password, { password = it; localError = null }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), shape = RoundedCornerShape(14.dp))
                    (localError ?: state.error)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Button(onClick = {
                        localError = when {
                            !email.contains("@") -> "Enter a valid email address."
                            password.length < 6 -> "Password must be at least 6 characters."
                            register && name.isBlank() -> "Enter your name."
                            register && name.trim().length > 80 -> "Keep your name under 80 characters."
                            register && collegeName.trim().length < 3 -> "Enter your college or university name."
                            register && collegeName.trim().length > 120 -> "Keep the college name under 120 characters."
                            else -> null
                        }
                        if (localError == null) if (register) onRegister(name, email, password, collegeName) else onSignIn(email, password)
                    }, Modifier.fillMaxWidth().height(50.dp), enabled = !state.busy, shape = RoundedCornerShape(15.dp)) {
                        if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text(if (register) "Create account" else "Sign in")
                    }
                    TextButton(onClick = { register = !register; localError = null }) { Text(if (register) "Already a member? Sign in" else "New to campus? Create an account") }
                }
            }
            Spacer(Modifier.height(17.dp))
            Text("A little more reuse. A little less waste.", color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun AccountScreen(state: MarketplaceState, onSignOut: () -> Unit, onSaveCollege: (String) -> Unit, modifier: Modifier = Modifier) {
    var collegeName by remember(state.collegeName) { mutableStateOf(state.collegeName) }
    Column(modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Box(Modifier.size(82.dp).clip(CircleShape).background(Color(0xFFE3F0E7)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Green, modifier = Modifier.size(42.dp)) }
        Spacer(Modifier.height(15.dp))
        Text(state.displayName.ifBlank { state.email.substringBefore("@") }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(state.email, color = Muted)
        Spacer(Modifier.height(26.dp))
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("Your campus profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Set your college so students can find listings from their campus.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(collegeName, { collegeName = it }, Modifier.fillMaxWidth(), label = { Text("College / university") }, leadingIcon = { Icon(Icons.Default.School, null) }, singleLine = true, shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onSaveCollege(collegeName) }, enabled = !state.busy && collegeName.trim() != state.collegeName, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                    if (state.busy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text("Save college")
                }
                Spacer(Modifier.height(6.dp))
                Text("Your listings are tagged with this college. Students can filter the marketplace by campus.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Sign out") }
    }
}

@Composable
private fun PostListingDialog(busy: Boolean, collegeName: String, onDismiss: () -> Unit, onSubmit: (String, String, String, Double, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Textbooks") }
    var price by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Good") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Give it a new home", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (collegeName.isBlank()) Text("Add your college in Profile before posting.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                else Text("Visible to students at $collegeName", color = Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(title, { title = it; error = null }, Modifier.fillMaxWidth(), label = { Text("What are you selling?") }, singleLine = true, shape = RoundedCornerShape(13.dp))
                OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Description") }, minLines = 2, maxLines = 4, shape = RoundedCornerShape(13.dp))
                OutlinedTextField(price, { price = it.filter { char -> char.isDigit() || char == '.' }; error = null }, Modifier.fillMaxWidth(), label = { Text("Price") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(13.dp))
                var menu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { menu = true }, Modifier.fillMaxWidth()) { Text("$category  ·  $condition") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        listingCategories.filter { it != "All" }.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { category = it; menu = false }) }
                    }
                }
                var conditionMenu by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { conditionMenu = true }) { Text("Condition: $condition") }
                    DropdownMenu(expanded = conditionMenu, onDismissRequest = { conditionMenu = false }) {
                        listOf("Like new", "Good", "Fair", "For parts").forEach { DropdownMenuItem(text = { Text(it) }, onClick = { condition = it; conditionMenu = false }) }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(enabled = !busy, onClick = {
                val amount = price.toDoubleOrNull()
                error = ListingValidator.validate(title, description, category, amount, condition)
                if (error == null) onSubmit(title, description, category, amount!!, condition)
            }) { if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text("Post listing") }
        }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun EmptyState(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 38.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(66.dp).clip(CircleShape).background(Color(0xFFE8F1EB)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Green, modifier = Modifier.size(29.dp)) }
        Spacer(Modifier.height(13.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun money(amount: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
