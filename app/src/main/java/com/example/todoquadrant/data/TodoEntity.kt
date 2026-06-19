package com.example.todoquadrant.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todos",
    indices = [
        Index(value = ["is_completed"]),
        Index(value = ["is_important", "is_urgent"]),
    ],
)
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val note: String? = null,
    @ColumnInfo(name = "is_important")
    val isImportant: Boolean = false,
    @ColumnInfo(name = "is_urgent")
    val isUrgent: Boolean = false,
    @ColumnInfo(name = "due_at")
    val dueAt: Long? = null,
    @ColumnInfo(name = "reminder_at")
    val reminderAt: Long? = null,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    val source: String = TodoSource.TEXT,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
)

val TodoEntity.quadrant: Quadrant
    get() = Quadrant.fromFlags(isImportant, isUrgent)

enum class Quadrant(
    val title: String,
    val isImportant: Boolean,
    val isUrgent: Boolean,
) {
    ImportantUrgent("重要紧急", isImportant = true, isUrgent = true),
    ImportantNotUrgent("重要不紧急", isImportant = true, isUrgent = false),
    UrgentNotImportant("紧急不重要", isImportant = false, isUrgent = true),
    NotImportantNotUrgent("不重要不紧急", isImportant = false, isUrgent = false);

    companion object {
        fun fromFlags(isImportant: Boolean, isUrgent: Boolean): Quadrant =
            entries.first { it.isImportant == isImportant && it.isUrgent == isUrgent }
    }
}

object TodoSource {
    const val TEXT = "TEXT"
    const val VOICE = "VOICE"
}
