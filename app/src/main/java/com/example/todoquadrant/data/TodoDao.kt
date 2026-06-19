package com.example.todoquadrant.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query(
        """
        SELECT * FROM todos
        ORDER BY
            is_completed ASC,
            is_important DESC,
            is_urgent DESC,
            CASE WHEN reminder_at IS NULL THEN 1 ELSE 0 END ASC,
            reminder_at ASC,
            created_at DESC
        """,
    )
    fun observeTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TodoEntity?

    @Query(
        """
        SELECT * FROM todos
        WHERE reminder_at IS NOT NULL
            AND reminder_at > :now
            AND is_completed = 0
        ORDER BY reminder_at ASC
        """,
    )
    suspend fun pendingReminders(now: Long): List<TodoEntity>

    @Query(
        """
        SELECT * FROM todos
        WHERE is_completed = 0
        ORDER BY
            is_important DESC,
            is_urgent DESC,
            CASE WHEN reminder_at IS NULL THEN 1 ELSE 0 END ASC,
            reminder_at ASC,
            created_at DESC
        LIMIT :limit
        """,
    )
    suspend fun activeWidgetTodos(limit: Int): List<TodoEntity>

    @Query("SELECT COUNT(*) FROM todos WHERE is_completed = 0")
    suspend fun activeCount(): Int

    @Insert
    suspend fun insert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity)

    @Delete
    suspend fun delete(todo: TodoEntity)
}
