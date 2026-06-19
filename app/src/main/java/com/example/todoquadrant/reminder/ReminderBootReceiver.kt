package com.example.todoquadrant.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoquadrant.data.TodoDatabase
import com.example.todoquadrant.data.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = TodoRepository(TodoDatabase.get(context).todoDao())
                val scheduler = ReminderScheduler(context.applicationContext)
                repository.pendingReminders(System.currentTimeMillis()).forEach(scheduler::schedule)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
