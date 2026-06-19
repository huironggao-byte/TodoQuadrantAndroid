package com.example.todoquadrant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todoquadrant.data.ReminderMode
import com.example.todoquadrant.data.TodoEntity
import com.example.todoquadrant.data.TodoRepository
import com.example.todoquadrant.data.TodoSource
import com.example.todoquadrant.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodoUiState(
    val todos: List<TodoEntity> = emptyList(),
    val titleDraft: String = "",
    val noteDraft: String = "",
    val isImportantDraft: Boolean = true,
    val isUrgentDraft: Boolean = false,
    val dueAtDraft: Long? = null,
    val reminderAtDraft: Long? = null,
    val reminderModeDraft: String = ReminderMode.NOTIFICATION,
    val sourceDraft: String = TodoSource.TEXT,
    val filter: TodoFilter = TodoFilter.Active,
)

enum class TodoFilter(val label: String) {
    Active("全部"),
    Today("今日"),
    Reminders("提醒"),
    Completed("已完成"),
}

class TodoViewModel(
    private val repository: TodoRepository,
    private val reminderScheduler: ReminderScheduler,
    private val onTodosChanged: () -> Unit,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TodoUiState())
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.todos.collect { todos ->
                _uiState.update { it.copy(todos = todos) }
            }
        }
    }

    fun updateTitle(value: String) {
        _uiState.update { it.copy(titleDraft = value, sourceDraft = TodoSource.TEXT) }
    }

    fun updateNote(value: String) {
        _uiState.update { it.copy(noteDraft = value) }
    }

    fun toggleImportant() {
        _uiState.update { it.copy(isImportantDraft = !it.isImportantDraft) }
    }

    fun toggleUrgent() {
        _uiState.update { it.copy(isUrgentDraft = !it.isUrgentDraft) }
    }

    fun setDueAt(value: Long?) {
        _uiState.update { it.copy(dueAtDraft = value) }
    }

    fun setReminderAt(value: Long?) {
        _uiState.update { it.copy(reminderAtDraft = value) }
    }

    fun setReminderMode(value: String) {
        if (value !in ReminderMode.entries) {
            return
        }
        _uiState.update { it.copy(reminderModeDraft = value) }
    }

    fun selectFilter(filter: TodoFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun addDraft(source: String? = null) {
        val state = _uiState.value
        val title = state.titleDraft.trim()
        if (title.isBlank()) {
            return
        }

        viewModelScope.launch {
            val todo = repository.addTodo(
                title = title,
                note = state.noteDraft.trim(),
                isImportant = state.isImportantDraft,
                isUrgent = state.isUrgentDraft,
                dueAt = state.dueAtDraft,
                reminderAt = state.reminderAtDraft,
                reminderMode = state.reminderModeDraft,
                source = source ?: state.sourceDraft,
            )
            reminderScheduler.schedule(todo)
            onTodosChanged()
            _uiState.update {
                it.copy(
                    titleDraft = "",
                    noteDraft = "",
                    dueAtDraft = null,
                    reminderAtDraft = null,
                    reminderModeDraft = ReminderMode.NOTIFICATION,
                    sourceDraft = TodoSource.TEXT,
                )
            }
        }
    }

    fun fillTitleFromVoice(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            return
        }
        _uiState.update { it.copy(titleDraft = cleanText, sourceDraft = TodoSource.VOICE) }
    }

    fun toggleCompleted(todo: TodoEntity, completed: Boolean) {
        viewModelScope.launch {
            val updated = repository.toggleCompleted(todo, completed)
            reminderScheduler.schedule(updated)
            onTodosChanged()
        }
    }

    fun updateTodo(
        todo: TodoEntity,
        title: String,
        note: String,
        isImportant: Boolean,
        isUrgent: Boolean,
        dueAt: Long?,
        reminderAt: Long?,
        reminderMode: String,
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) {
            return
        }

        viewModelScope.launch {
            val updated = repository.updateTodo(
                todo.copy(
                    title = cleanTitle,
                    note = note.trim().takeIf { it.isNotBlank() },
                    isImportant = isImportant,
                    isUrgent = isUrgent,
                    dueAt = dueAt,
                    reminderAt = reminderAt,
                    reminderMode = reminderMode,
                ),
            )
            reminderScheduler.schedule(updated)
            onTodosChanged()
        }
    }

    fun delete(todo: TodoEntity) {
        viewModelScope.launch {
            repository.delete(todo)
            reminderScheduler.cancel(todo.id)
            onTodosChanged()
        }
    }

    companion object {
        fun factory(
            repository: TodoRepository,
            reminderScheduler: ReminderScheduler,
            onTodosChanged: () -> Unit,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TodoViewModel(repository, reminderScheduler, onTodosChanged) as T
            }
        }
    }
}
