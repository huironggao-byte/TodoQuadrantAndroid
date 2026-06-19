package com.example.todoquadrant.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoquadrant.data.TodoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMIND) {
            return
        }

        val todoId = intent.getLongExtra(ReminderScheduler.EXTRA_TODO_ID, -1L)
        if (todoId <= 0L) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val todo = TodoDatabase.get(context).todoDao().getById(todoId)
                if (todo != null && !todo.isCompleted) {
                    NotificationHelper.showReminder(context, todo)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
