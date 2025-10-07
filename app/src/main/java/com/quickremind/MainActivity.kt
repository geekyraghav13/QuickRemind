package com.quickremind

import android.Manifest
import android.app.Application
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.widget.DatePicker
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.quickremind.data.AppDatabase
import com.quickremind.data.Reminder
import com.quickremind.ui.theme.QuickRemindTheme
import com.quickremind.util.OnboardingManager
import com.quickremind.viewmodel.ReminderViewModel
import com.quickremind.viewmodel.ReminderViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuickRemindTheme {
                // This is the main entry point for the app UI
                App()
            }
        }
    }
}

// --- NEW LOADING SCREEN ---
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier.size(150.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}

// This new Composable will manage the app's state (loading, onboarding, or main app)
@Composable
fun App() {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var startDestination by remember { mutableStateOf("loading") }

    // This effect runs only once when the App starts
    LaunchedEffect(key1 = true) {
        // We do heavy work in the background so the UI doesn't freeze
        withContext(Dispatchers.IO) {
            // Pre-initialize the database
            AppDatabase.getDatabase(context)
            delay(1000) // Simulate a short loading time
        }

        // After loading, we decide where to go next
        startDestination = if (OnboardingManager.isOnboardingComplete(context)) {
            "main"
        } else {
            "onboarding"
        }
        isLoading = false
    }

    if (isLoading) {
        LoadingScreen()
    } else {
        AppNavigator(startDestination = startDestination)
    }
}


// --- THE REST OF THE APP REMAINS THE SAME ---
// Data class to hold the content for each onboarding page
data class OnboardingPage(
    val imageRes: Int,
    val title: String,
    val description: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current

    val pages = listOf(
        OnboardingPage(
            imageRes = R.drawable.onboarding_page1,
            title = "Welcome to QuickRemind",
            description = "Capture ideas and tasks in seconds. Never let a thought slip away."
        ),
        OnboardingPage(
            imageRes = R.drawable.onboarding_page2,
            title = "Stay on Track",
            description = "Get timely notifications exactly when you need them, so you never miss a deadline."
        ),
        OnboardingPage(
            imageRes = R.drawable.onboarding_page3,
            title = "Ready to Get Organized?",
            description = "Enjoy all features for free, with an option to go Pro to remove ads."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        Row(
            Modifier
                .height(50.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(12.dp)
                )
            }
        }

        if (pagerState.currentPage == pages.size - 1) {
            Button(
                onClick = {
                    OnboardingManager.setOnboardingComplete(context)
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Get Started")
            }
        } else {
            Spacer(modifier = Modifier.height(64.dp).padding(16.dp))
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = page.imageRes),
            contentDescription = page.title,
            modifier = Modifier.size(250.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}


@Composable
fun AppNavigator(startDestination: String) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val viewModel: ReminderViewModel = viewModel(
        factory = ReminderViewModelFactory(context.applicationContext as Application)
    )

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") { OnboardingScreen(navController = navController) }
        composable("main") { MainScreen(navController = navController, viewModel = viewModel) }
        composable("add") { AddReminderScreen(navController = navController, viewModel = viewModel) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, viewModel: ReminderViewModel) {
    val reminders by viewModel.allReminders.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var selectedReminder by remember { mutableStateOf<Reminder?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> }
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reminders", color = Color(0xFFFF9500)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Reminder")
            }
        }
    ) { padding ->
        if (showDeleteDialog && selectedReminder != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Mark as Complete?") },
                text = { Text("Are you sure you want to mark this reminder as complete?") },
                confirmButton = {
                    Button(onClick = {
                        selectedReminder?.let { viewModel.deleteReminder(it) }
                        showDeleteDialog = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    Button(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showDetailsDialog && selectedReminder != null) {
            AlertDialog(
                onDismissRequest = { showDetailsDialog = false },
                title = { Text(selectedReminder?.title ?: "Details") },
                text = { Text(selectedReminder?.notes ?: "No notes for this reminder.") },
                confirmButton = {
                    Button(onClick = { showDetailsDialog = false }) { Text("OK") }
                }
            )
        }

        Column(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.StartToEnd) {
                                viewModel.deleteReminder(reminder)
                                return@rememberSwipeToDismissBoxState true
                            }
                            return@rememberSwipeToDismissBoxState false
                        }
                    )

                    SwipeToDismissBox(
                        state = swipeToDismissBoxState,
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = false,
                        backgroundContent = {
                            val color by animateColorAsState(
                                targetValue = if (swipeToDismissBoxState.targetValue == SwipeToDismissBoxValue.StartToEnd) Color.Red else Color.LightGray
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Icon", tint = Color.White)
                            }
                        }
                    ) {
                        ReminderItem(
                            reminder = reminder,
                            onMarkAsComplete = {
                                selectedReminder = reminder
                                showDeleteDialog = true
                            },
                            onClick = {
                                selectedReminder = reminder
                                showDetailsDialog = true
                            }
                        )
                    }
                }
            }
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = "ca-app-pub-6555268506098745/8429340384"
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(navController: NavController, viewModel: ReminderViewModel) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val context = LocalContext.current

    val calendar = Calendar.getInstance()
    var reminderTime by remember { mutableLongStateOf(calendar.timeInMillis) }
    var dateText by remember { mutableStateOf("Set Date") }
    var timeText by remember { mutableStateOf("Set Time") }

    val datePickerDialog = DatePickerDialog(context, { _: DatePicker, y, m, d ->
        calendar.set(y, m, d)
        reminderTime = calendar.timeInMillis
        dateText = formatShortDate(calendar.timeInMillis)
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    val timePickerDialog = TimePickerDialog(context, { _, h, m ->
        calendar.set(Calendar.HOUR_OF_DAY, h)
        calendar.set(Calendar.MINUTE, m)
        reminderTime = calendar.timeInMillis
        timeText = formatShortTime(calendar.timeInMillis)
    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("New Reminder") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }, actions = {
                TextButton(onClick = {
                    val newReminder = Reminder(title = title, notes = notes, reminderTime = reminderTime)
                    viewModel.addReminder(newReminder)
                    viewModel.scheduleAlarm(context, newReminder)
                    navController.popBackStack()
                }) {
                    Text("Add", fontSize = 16.sp)
                }
            })
    }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { datePickerDialog.show() }) { Text(dateText) }
                Button(onClick = { timePickerDialog.show() }) { Text(timeText) }
            }
        }
    }
}

@Composable
fun ReminderItem(
    reminder: Reminder,
    onMarkAsComplete: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = false, onClick = onMarkAsComplete)
            Column {
                Text(text = reminder.title, fontSize = 18.sp)
                Text(text = formatDateTime(reminder.reminderTime), color = Color.Gray)
            }
        }
    }
}

fun formatDateTime(timeInMillis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    return sdf.format(Date(timeInMillis))
}

fun formatShortDate(timeInMillis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timeInMillis))
}

fun formatShortTime(timeInMillis: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timeInMillis))
}