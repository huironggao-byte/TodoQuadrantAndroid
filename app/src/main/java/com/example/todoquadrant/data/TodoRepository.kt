package com.example.todoquadrant.data

import kotlinx.coroutines.flow.Flow

class TodoRepository(
    private val dao: TodoDao,
) {
    val todos: Flow<List<TodoEntity>> = dao.observeTodos()

    suspend fun addTodo(
        title: String,
        note: String?,
        isImportant: Boolean,
        isUrgent: Boolean,
        dueAt: Long?,
        reminderAt: Long?,
        reminderMode: String,
        source: String,
    ): TodoEntity {
        val now = System.currentTimeMillis()
        val id = dao.insert(
            TodoEntity(
                title = title,
                note = note?.takeIf { it.isNotBlank() },
                isImportant = isImportant,
                isUrgent = isUrgent,
                dueAt = dueAt,
                reminderAt = reminderAt,
                reminderMode = reminderMode,
                source = source,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return requireNotNull(dao.getById(id))
    }

    suspend fun updateTodo(todo: TodoEntity): TodoEntity {
        val updated = todo.copy(updatedAt = System.currentTimeMillis())
        dao.update(updated)
        return updated
    }

    suspend fun toggleCompleted(todo: TodoEntity, completed: Boolean): TodoEntity {
        val now = System.currentTimeMillis()
        val updated = todo.copy(
            isCompleted = completed,
            completedAt = if (completed) now else null,
            updatedAt = now,
        )
        dao.update(updated)
        return updated
    }

    suspend fun delete(todo: TodoEntity) {
        dao.delete(todo)
    }

    suspend fun getById(id: Long): TodoEntity? = dao.getById(id)

    suspend fun pendingReminders(now: Long): List<TodoEntity> = dao.pendingReminders(now)
}
