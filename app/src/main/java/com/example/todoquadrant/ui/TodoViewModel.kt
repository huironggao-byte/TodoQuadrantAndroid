package com.example.todoquadrant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
        _uiState.update { it.copy(titleDraft = value) }
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

    fun selectFilter(filter: TodoFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun addDraft(source: String = TodoSource.TEXT) {
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
                source = source,
            )
            reminderScheduler.schedule(todo)
            _uiState.update {
                it.copy(
                    titleDraft = "",
                    noteDraft = "",
                    dueAtDraft = null,
                    reminderAtDraft = null,
                )
            }
        }
    }

    fun addVoiceTodo(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            return
        }
        _uiState.update { it.copy(titleDraft = cleanText) }
        addDraft(TodoSource.VOICE)
    }

    fun toggleCompleted(todo: TodoEntity, completed: Boolean) {
        viewModelScope.launch {
            val updated = repository.toggleCompleted(todo, completed)
            reminderScheduler.schedule(updated)
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
                ),
            )
            reminderScheduler.schedule(updated)
        }
    }

    fun delete(todo: TodoEntity) {
        viewModelScope.launch {
            repository.delete(todo)
            reminderScheduler.cancel(todo.id)
        }
    }

    companion object {
        fun factory(
            repository: TodoRepository,
            reminderScheduler: ReminderScheduler,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TodoViewModel(repository, reminderScheduler) as T
            }
        }
    }
}
