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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quickremind.data.Reminder
import com.quickremind.ui.theme.QuickRemindTheme
import com.quickremind.viewmodel.ReminderViewModel
import com.quickremind.viewmodel.ReminderViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuickRemindTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val viewModel: ReminderViewModel = viewModel(
        factory = ReminderViewModelFactory(context.applicationContext as Application)
    )

    NavHost(navController = navController, startDestination = "main") {
        composable("main") { MainScreen(navController = navController, viewModel = viewModel) }
        composable("add") { AddReminderScreen(navController = navController, viewModel = viewModel) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, viewModel: ReminderViewModel) {
    val reminders by viewModel.allReminders.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
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
        if (showDialog && selectedReminder != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Mark as Complete?") },
                text = { Text("Are you sure you want to mark this reminder as complete?") },
                confirmButton = {
                    Button(onClick = {
                        selectedReminder?.let { viewModel.deleteReminder(it) }
                        showDialog = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) { Text("Cancel") }
                }
            )
        }

        LazyColumn(modifier = Modifier.padding(padding)) {
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
                        title = reminder.title,
                        dateTime = formatDateTime(reminder.reminderTime),
                        onMarkAsComplete = {
                            selectedReminder = reminder
                            showDialog = true
                        }
                    )
                }
            }
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
        TopAppBar( // <-- THIS WAS THE ERROR. IT'S NOW CORRECTED.
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
fun ReminderItem(title: String, dateTime: String, onMarkAsComplete: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = false, onClick = onMarkAsComplete)
            Column {
                Text(text = title, fontSize = 18.sp)
                Text(text = dateTime, color = Color.Gray)
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

