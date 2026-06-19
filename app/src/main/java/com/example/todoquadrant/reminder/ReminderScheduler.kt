package com.example.todoquadrant.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.todoquadrant.data.TodoEntity

class ReminderScheduler(
    private val context: Context,
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    fun schedule(todo: TodoEntity) {
        cancel(todo.id)

        val reminderAt = todo.reminderAt ?: return
        if (todo.isCompleted || reminderAt <= System.currentTimeMillis()) {
            return
        }

        val operation = pendingIntent(todo.id)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, operation)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminderAt, operation)
        }
    }

    fun cancel(todoId: Long) {
        alarmManager.cancel(pendingIntent(todoId))
    }

    private fun pendingIntent(todoId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMIND)
            .putExtra(EXTRA_TODO_ID, todoId)

        return PendingIntent.getBroadcast(
            context,
            todoId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_REMIND = "com.example.todoquadrant.ACTION_REMIND"
        const val EXTRA_TODO_ID = "extra_todo_id"
    }
}
