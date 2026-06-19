package com.example.todoquadrant

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todoquadrant.data.TodoDatabase
import com.example.todoquadrant.data.TodoRepository
import com.example.todoquadrant.reminder.NotificationHelper
import com.example.todoquadrant.reminder.ReminderScheduler
import com.example.todoquadrant.ui.TodoApp
import com.example.todoquadrant.ui.TodoViewModel
import com.example.todoquadrant.ui.theme.TodoQuadrantTheme
import com.example.todoquadrant.widget.TodoWidgetProvider
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: TodoViewModel

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            return@registerForActivityResult
        }

        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()

        if (text.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.speech_no_result), Toast.LENGTH_SHORT).show()
        } else {
            viewModel.fillTitleFromVoice(text)
            Toast.makeText(this, getString(R.string.speech_result_filled), Toast.LENGTH_SHORT).show()
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchSpeechRecognizer()
        } else {
            Toast.makeText(this, getString(R.string.audio_permission_needed), Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val message = if (granted) {
            getString(R.string.notification_permission_granted)
        } else {
            getString(R.string.notification_permission_denied)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.ensureChannel(this)

        val database = TodoDatabase.get(applicationContext)
        val repository = TodoRepository(database.todoDao())
        val scheduler = ReminderScheduler(applicationContext)
        viewModel = ViewModelProvider(
            this,
            TodoViewModel.factory(
                repository = repository,
                reminderScheduler = scheduler,
                onTodosChanged = { TodoWidgetProvider.updateAll(applicationContext) },
            ),
        )[TodoViewModel::class.java]
        TodoWidgetProvider.updateAll(applicationContext)

        setContent {
            TodoQuadrantTheme {
                val state = viewModel.uiState.collectAsStateWithLifecycle().value

                TodoApp(
                    state = state,
                    onTitleChange = viewModel::updateTitle,
                    onNoteChange = viewModel::updateNote,
                    onImportantToggle = viewModel::toggleImportant,
                    onUrgentToggle = viewModel::toggleUrgent,
                    onDueSelected = viewModel::setDueAt,
                    onReminderSelected = viewModel::setReminderAt,
                    onReminderModeSelected = viewModel::setReminderMode,
                    onFilterSelected = viewModel::selectFilter,
                    onAddClick = {
                        if (state.reminderAtDraft != null) {
                            maybeRequestNotificationPermission()
                        }
                        viewModel.addDraft()
                    },
                    onVoiceClick = ::startVoiceInput,
                    onTodoCheckedChange = viewModel::toggleCompleted,
                    onTodoUpdate = { todo, title, note, important, urgent, dueAt, reminderAt, reminderMode ->
                        if (reminderAt != null) {
                            maybeRequestNotificationPermission()
                        }
                        viewModel.updateTodo(
                            todo = todo,
                            title = title,
                            note = note,
                            isImportant = important,
                            isUrgent = urgent,
                            dueAt = dueAt,
                            reminderAt = reminderAt,
                            reminderMode = reminderMode,
                        )
                    },
                    onTodoDelete = viewModel::delete,
                )
            }
        }
    }

    private fun startVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchSpeechRecognizer()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun launchSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt))
        }

        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.speech_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
