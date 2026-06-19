package com.example.todoquadrant.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.example.todoquadrant.MainActivity
import com.example.todoquadrant.R
import com.example.todoquadrant.data.Quadrant
import com.example.todoquadrant.data.TodoDatabase
import com.example.todoquadrant.data.TodoEntity
import com.example.todoquadrant.data.quadrant
import com.example.todoquadrant.reminder.ReminderScheduler
import com.example.todoquadrant.ui.TimeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodoWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REFRESH,
            ACTION_COMPLETE,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED
            -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (intent.action == ACTION_COMPLETE) {
                            completeTodo(context, intent.getLongExtra(EXTRA_TODO_ID, -1L))
                        }
                        updateAllNow(context.applicationContext)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            else -> super.onReceive(context, intent)
        }
    }

    override fun onEnabled(context: Context) {
        updateAll(context)
    }

    companion object {
        private const val ACTION_REFRESH = "com.example.todoquadrant.widget.ACTION_REFRESH"
        private const val ACTION_COMPLETE = "com.example.todoquadrant.widget.ACTION_COMPLETE"
        private const val EXTRA_TODO_ID = "extra_todo_id"
        private const val MAX_ROWS = 8

        private val rowIds = intArrayOf(
            R.id.widget_row_1,
            R.id.widget_row_2,
            R.id.widget_row_3,
            R.id.widget_row_4,
            R.id.widget_row_5,
            R.id.widget_row_6,
            R.id.widget_row_7,
            R.id.widget_row_8,
        )
        private val rowCheckIds = intArrayOf(
            R.id.widget_row_check_1,
            R.id.widget_row_check_2,
            R.id.widget_row_check_3,
            R.id.widget_row_check_4,
            R.id.widget_row_check_5,
            R.id.widget_row_check_6,
            R.id.widget_row_check_7,
            R.id.widget_row_check_8,
        )
        private val rowTitleIds = intArrayOf(
            R.id.widget_row_title_1,
            R.id.widget_row_title_2,
            R.id.widget_row_title_3,
            R.id.widget_row_title_4,
            R.id.widget_row_title_5,
            R.id.widget_row_title_6,
            R.id.widget_row_title_7,
            R.id.widget_row_title_8,
        )
        private val rowMetaIds = intArrayOf(
            R.id.widget_row_meta_1,
            R.id.widget_row_meta_2,
            R.id.widget_row_meta_3,
            R.id.widget_row_meta_4,
            R.id.widget_row_meta_5,
            R.id.widget_row_meta_6,
            R.id.widget_row_meta_7,
            R.id.widget_row_meta_8,
        )
        private val rowStarIds = intArrayOf(
            R.id.widget_row_star_1,
            R.id.widget_row_star_2,
            R.id.widget_row_star_3,
            R.id.widget_row_star_4,
            R.id.widget_row_star_5,
            R.id.widget_row_star_6,
            R.id.widget_row_star_7,
            R.id.widget_row_star_8,
        )

        fun updateAll(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                updateAllNow(context.applicationContext)
            }
        }

        private suspend fun completeTodo(context: Context, todoId: Long) {
            if (todoId <= 0L) {
                return
            }
            val dao = TodoDatabase.get(context).todoDao()
            val todo = dao.getById(todoId) ?: return
            if (todo.isCompleted) {
                return
            }

            val now = System.currentTimeMillis()
            dao.update(
                todo.copy(
                    isCompleted = true,
                    completedAt = now,
                    updatedAt = now,
                ),
            )
            ReminderScheduler(context.applicationContext).cancel(todoId)
        }

        private suspend fun updateAllNow(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TodoWidgetProvider::class.java))
            if (ids.isEmpty()) {
                return
            }

            val dao = TodoDatabase.get(context).todoDao()
            val todos = dao.activeWidgetTodos(MAX_ROWS)
            val activeCount = dao.activeCount()
            ids.forEach { widgetId ->
                val options = manager.getAppWidgetOptions(widgetId)
                val visibleRows = rowCountForHeight(
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 220),
                )
                val views = buildViews(
                    context = context,
                    todos = todos,
                    activeCount = activeCount,
                    visibleRows = visibleRows,
                )
                manager.updateAppWidget(widgetId, views)
            }
        }

        private fun buildViews(
            context: Context,
            todos: List<TodoEntity>,
            activeCount: Int,
            visibleRows: Int,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.todo_widget)
            val openIntent = PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                pendingIntentFlags(),
            )
            val refreshIntent = PendingIntent.getBroadcast(
                context,
                2,
                Intent(context, TodoWidgetProvider::class.java).setAction(ACTION_REFRESH),
                pendingIntentFlags(),
            )

            views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_filter_all))
            views.setOnClickPendingIntent(R.id.widget_title, openIntent)
            views.setOnClickPendingIntent(R.id.widget_checkmark, openIntent)
            views.setOnClickPendingIntent(R.id.widget_add, openIntent)
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshIntent)

            val rowsToShow = todos.take(visibleRows)
            views.setViewVisibility(R.id.widget_empty, if (todos.isEmpty()) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_list, if (todos.isEmpty()) View.GONE else View.VISIBLE)

            rowIds.forEachIndexed { index, rowId ->
                val todo = rowsToShow.getOrNull(index)
                if (todo == null) {
                    views.setViewVisibility(rowId, View.GONE)
                    return@forEachIndexed
                }

                views.setViewVisibility(rowId, View.VISIBLE)
                views.setTextViewText(rowTitleIds[index], todo.title)
                views.setTextViewText(rowMetaIds[index], metaText(context, todo))
                views.setTextViewText(rowStarIds[index], if (todo.isImportant) "★" else "☆")
                views.setOnClickPendingIntent(rowIds[index], openIntent)
                views.setOnClickPendingIntent(rowCheckIds[index], completeIntent(context, todo.id))
            }

            val hiddenCount = (activeCount - rowsToShow.size).coerceAtLeast(0)
            if (hiddenCount > 0) {
                views.setViewVisibility(R.id.widget_more, View.VISIBLE)
                views.setTextViewText(R.id.widget_more, context.getString(R.string.widget_more, hiddenCount))
            } else {
                views.setViewVisibility(R.id.widget_more, View.GONE)
            }

            return views
        }

        private fun completeIntent(context: Context, todoId: Long): PendingIntent {
            val requestCode = 10_000 + (todoId % 1_000_000).toInt()
            val intent = Intent(context, TodoWidgetProvider::class.java)
                .setAction(ACTION_COMPLETE)
                .putExtra(EXTRA_TODO_ID, todoId)
            return PendingIntent.getBroadcast(context, requestCode, intent, pendingIntentFlags())
        }

        private fun pendingIntentFlags(): Int =
            PendingIntent.FLAG_UPDATE_CURRENT or (
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    0
                }
                )

        private fun rowCountForHeight(minHeightDp: Int): Int =
            when {
                minHeightDp < 190 -> 3
                minHeightDp < 300 -> 5
                else -> MAX_ROWS
            }

        private fun metaText(context: Context, todo: TodoEntity): String {
            val parts = buildList {
                add(context.getString(R.string.widget_task_type))
                todo.dueAt?.let { add(context.getString(R.string.due_time_value, TimeText.format(it))) }
                todo.reminderAt?.let {
                    add(context.getString(R.string.reminder_time_value, TimeText.format(it)))
                }
                add(quadrantLabel(context, todo.quadrant))
            }
            return parts.joinToString(" · ")
        }

        private fun quadrantLabel(context: Context, quadrant: Quadrant): String =
            when (quadrant) {
                Quadrant.ImportantUrgent -> context.getString(R.string.quadrant_important_urgent)
                Quadrant.ImportantNotUrgent -> context.getString(R.string.quadrant_important_not_urgent)
                Quadrant.UrgentNotImportant -> context.getString(R.string.quadrant_urgent_not_important)
                Quadrant.NotImportantNotUrgent -> context.getString(R.string.quadrant_not_important_not_urgent)
            }
    }
}
