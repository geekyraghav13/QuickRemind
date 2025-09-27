package com.quickremind.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quickremind.data.AppDatabase
import com.quickremind.data.Reminder
import com.quickremind.util.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application) : ViewModel() {
    private val reminderDao = AppDatabase.getDatabase(application).reminderDao()

    val allReminders: StateFlow<List<Reminder>> = reminderDao.getAllReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun addReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderDao.insert(reminder)
        }
    }

    // --- THIS IS THE NEW FUNCTION ---
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            // We should also cancel the alarm here in a real app
            reminderDao.delete(reminder)
        }
    }

    fun scheduleAlarm(context: Context, reminder: Reminder) {
        AlarmScheduler.schedule(context, reminder)
    }
}

// This factory is needed to create the ViewModel
class ReminderViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReminderViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}