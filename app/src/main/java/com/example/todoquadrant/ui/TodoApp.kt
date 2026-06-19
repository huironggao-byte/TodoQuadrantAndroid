@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.todoquadrant.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoquadrant.R
import com.example.todoquadrant.data.Quadrant
import com.example.todoquadrant.data.ReminderMode
import com.example.todoquadrant.data.TodoEntity
import com.example.todoquadrant.data.TodoSource
import com.example.todoquadrant.data.quadrant
import java.util.Calendar

@Composable
fun TodoApp(
    state: TodoUiState,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onImportantToggle: () -> Unit,
    onUrgentToggle: () -> Unit,
    onDueSelected: (Long?) -> Unit,
    onReminderSelected: (Long?) -> Unit,
    onReminderModeSelected: (String) -> Unit,
    onFilterSelected: (TodoFilter) -> Unit,
    onAddClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onTodoCheckedChange: (TodoEntity, Boolean) -> Unit,
    onTodoUpdate: (TodoEntity, String, String, Boolean, Boolean, Long?, Long?, String) -> Unit,
    onTodoDelete: (TodoEntity) -> Unit,
) {
    var editingTodo by remember { mutableStateOf<TodoEntity?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                QuickEntryPanel(
                    state = state,
                    onTitleChange = onTitleChange,
                    onNoteChange = onNoteChange,
                    onImportantToggle = onImportantToggle,
                    onUrgentToggle = onUrgentToggle,
                    onDueSelected = onDueSelected,
                    onReminderSelected = onReminderSelected,
                    onReminderModeSelected = onReminderModeSelected,
                    onAddClick = onAddClick,
                    onVoiceClick = onVoiceClick,
                )
            }

            item {
                FilterRow(
                    selected = state.filter,
                    onSelected = onFilterSelected,
                )
            }

            item {
                ProgressLine(todos = state.todos)
            }

            item {
                QuadrantBoard(
                    todos = visibleTodos(state.todos, state.filter),
                    onTodoCheckedChange = onTodoCheckedChange,
                    onTodoEdit = { editingTodo = it },
                    onTodoDelete = onTodoDelete,
                )
            }
        }
    }

    editingTodo?.let { todo ->
        EditTodoDialog(
            todo = todo,
            onDismiss = { editingTodo = null },
            onSave = { target, title, note, important, urgent, dueAt, reminderAt, reminderMode ->
                onTodoUpdate(target, title, note, important, urgent, dueAt, reminderAt, reminderMode)
                editingTodo = null
            },
        )
    }
}

@Composable
private fun QuickEntryPanel(
    state: TodoUiState,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onImportantToggle: () -> Unit,
    onUrgentToggle: () -> Unit,
    onDueSelected: (Long?) -> Unit,
    onReminderSelected: (Long?) -> Unit,
    onReminderModeSelected: (String) -> Unit,
    onAddClick: () -> Unit,
    onVoiceClick: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val titleFocusRequester = remember { FocusRequester() }
    val addDraft = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onAddClick()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = state.titleDraft,
                onValueChange = onTitleChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester),
                placeholder = { Text(stringResource(R.string.quick_entry_placeholder)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (state.titleDraft.isNotBlank()) {
                        addDraft()
                    }
                }),
                maxLines = 3,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        titleFocusRequester.requestFocus()
                        keyboardController?.show()
                    },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Rounded.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.keyboard_dictation))
                }
                OutlinedButton(
                    onClick = onVoiceClick,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.system_voice))
                }
            }

            OutlinedTextField(
                value = state.noteDraft,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.note_placeholder)) },
                maxLines = 2,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = state.isImportantDraft,
                    onClick = onImportantToggle,
                    label = { Text(stringResource(R.string.important)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
                FilterChip(
                    selected = state.isUrgentDraft,
                    onClick = onUrgentToggle,
                    label = { Text(stringResource(R.string.urgent)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.PriorityHigh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }

            DateTimePickButton(
                label = state.dueAtDraft?.let {
                    stringResource(R.string.due_time_value, TimeText.format(it))
                } ?: stringResource(R.string.due_time),
                icon = Icons.Rounded.Event,
                value = state.dueAtDraft,
                onSelected = onDueSelected,
                onClear = { onDueSelected(null) },
            )

            DateTimePickButton(
                label = state.reminderAtDraft?.let {
                    stringResource(R.string.reminder_time_value, TimeText.format(it))
                } ?: stringResource(R.string.reminder_time),
                icon = Icons.Rounded.Notifications,
                value = state.reminderAtDraft,
                onSelected = onReminderSelected,
                onClear = { onReminderSelected(null) },
            )

            ReminderModeSelector(
                selected = state.reminderModeDraft,
                onSelected = onReminderModeSelected,
            )

            Button(
                onClick = addDraft,
                enabled = state.titleDraft.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.add_todo))
            }
        }
    }
}

@Composable
private fun ReminderModeSelector(
    selected: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.reminder_mode),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReminderMode.entries.forEach { mode ->
                FilterChip(
                    selected = selected == mode,
                    onClick = { onSelected(mode) },
                    label = { Text(reminderModeLabel(mode)) },
                    leadingIcon = {
                        Icon(
                            reminderModeIcon(mode),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun reminderModeLabel(mode: String): String =
    when (mode) {
        ReminderMode.VIBRATE -> stringResource(R.string.reminder_mode_vibrate)
        ReminderMode.ALARM -> stringResource(R.string.reminder_mode_alarm)
        ReminderMode.ALARM_VIBRATE -> stringResource(R.string.reminder_mode_alarm_vibrate)
        else -> stringResource(R.string.reminder_mode_notification)
    }

private fun reminderModeIcon(mode: String): ImageVector =
    when (mode) {
        ReminderMode.VIBRATE -> Icons.Rounded.Vibration
        ReminderMode.ALARM -> Icons.Rounded.Alarm
        ReminderMode.ALARM_VIBRATE -> Icons.Rounded.Alarm
        else -> Icons.Rounded.Notifications
    }

@Composable
private fun DateTimePickButton(
    label: String,
    icon: ImageVector,
    value: Long?,
    onSelected: (Long) -> Unit,
    onClear: () -> Unit,
) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedButton(
            onClick = {
                showDateTimePicker(
                    context = context,
                    initialMillis = value,
                    onSelected = onSelected,
                )
            },
            shape = RoundedCornerShape(8.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        if (value != null) {
            IconButton(onClick = onClear) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_time))
            }
        }
    }
}

@Composable
private fun FilterRow(
    selected: TodoFilter,
    onSelected: (TodoFilter) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TodoFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(filterLabel(filter)) },
                leadingIcon = if (selected == filter) {
                    {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun ProgressLine(todos: List<TodoEntity>) {
    val activeCount = todos.count { !it.isCompleted }
    val completedCount = todos.count { it.isCompleted }

    Text(
        text = stringResource(R.string.progress_counts, activeCount, completedCount),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun filterLabel(filter: TodoFilter): String =
    when (filter) {
        TodoFilter.Active -> stringResource(R.string.filter_active)
        TodoFilter.Today -> stringResource(R.string.filter_today)
        TodoFilter.Reminders -> stringResource(R.string.filter_reminders)
        TodoFilter.Completed -> stringResource(R.string.filter_completed)
    }

@Composable
private fun QuadrantBoard(
    todos: List<TodoEntity>,
    onTodoCheckedChange: (TodoEntity, Boolean) -> Unit,
    onTodoEdit: (TodoEntity) -> Unit,
    onTodoDelete: (TodoEntity) -> Unit,
) {
    val grouped = Quadrant.entries.map { quadrant ->
        quadrant to todos.filter { it.quadrant == quadrant }
    }
    val isWide = LocalConfiguration.current.screenWidthDp >= 600

    if (isWide) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            grouped.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { (quadrant, items) ->
                        QuadrantPanel(
                            quadrant = quadrant,
                            todos = items,
                            onTodoCheckedChange = onTodoCheckedChange,
                            onTodoEdit = onTodoEdit,
                            onTodoDelete = onTodoDelete,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            grouped.forEach { (quadrant, items) ->
                QuadrantPanel(
                    quadrant = quadrant,
                    todos = items,
                    onTodoCheckedChange = onTodoCheckedChange,
                    onTodoEdit = onTodoEdit,
                    onTodoDelete = onTodoDelete,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun QuadrantPanel(
    quadrant: Quadrant,
    todos: List<TodoEntity>,
    onTodoCheckedChange: (TodoEntity, Boolean) -> Unit,
    onTodoEdit: (TodoEntity) -> Unit,
    onTodoDelete: (TodoEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = quadrantStyle(quadrant)

    Surface(
        modifier = modifier.heightIn(min = 148.dp),
        shape = RoundedCornerShape(8.dp),
        color = style.background,
        border = BorderStroke(1.dp, style.accent.copy(alpha = 0.32f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = quadrantLabel(quadrant),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = style.accent,
                ) {
                    Text(
                        text = todos.size.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            if (todos.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_todos),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    todos.forEach { todo ->
                        TodoRow(
                            todo = todo,
                            onCheckedChange = { checked -> onTodoCheckedChange(todo, checked) },
                            onEdit = { onTodoEdit(todo) },
                            onDelete = { onTodoDelete(todo) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoRow(
    todo: TodoEntity,
    onCheckedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
            .padding(start = 2.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = todo.isCompleted,
            onCheckedChange = onCheckedChange,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = todo.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
            )

            val details = todo.detailsText()
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        IconButton(onClick = onEdit) {
            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_todo))
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete_todo))
        }
    }
}

@Composable
private fun quadrantLabel(quadrant: Quadrant): String =
    when (quadrant) {
        Quadrant.ImportantUrgent -> stringResource(R.string.quadrant_important_urgent)
        Quadrant.ImportantNotUrgent -> stringResource(R.string.quadrant_important_not_urgent)
        Quadrant.UrgentNotImportant -> stringResource(R.string.quadrant_urgent_not_important)
        Quadrant.NotImportantNotUrgent -> stringResource(R.string.quadrant_not_important_not_urgent)
    }

@Composable
private fun EditTodoDialog(
    todo: TodoEntity,
    onDismiss: () -> Unit,
    onSave: (TodoEntity, String, String, Boolean, Boolean, Long?, Long?, String) -> Unit,
) {
    var title by remember(todo.id) { mutableStateOf(todo.title) }
    var note by remember(todo.id) { mutableStateOf(todo.note.orEmpty()) }
    var isImportant by remember(todo.id) { mutableStateOf(todo.isImportant) }
    var isUrgent by remember(todo.id) { mutableStateOf(todo.isUrgent) }
    var dueAt by remember(todo.id) { mutableStateOf(todo.dueAt) }
    var reminderAt by remember(todo.id) { mutableStateOf(todo.reminderAt) }
    var reminderMode by remember(todo.id) { mutableStateOf(todo.reminderMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_todo)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.todo_title)) },
                    maxLines = 3,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.note)) },
                    maxLines = 3,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = isImportant,
                        onClick = { isImportant = !isImportant },
                        label = { Text(stringResource(R.string.important)) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Star,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    FilterChip(
                        selected = isUrgent,
                        onClick = { isUrgent = !isUrgent },
                        label = { Text(stringResource(R.string.urgent)) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.PriorityHigh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
                DateTimePickButton(
                    label = dueAt?.let {
                        stringResource(R.string.due_time_value, TimeText.format(it))
                    } ?: stringResource(R.string.due_time),
                    icon = Icons.Rounded.Event,
                    value = dueAt,
                    onSelected = { dueAt = it },
                    onClear = { dueAt = null },
                )
                DateTimePickButton(
                    label = reminderAt?.let {
                        stringResource(R.string.reminder_time_value, TimeText.format(it))
                    } ?: stringResource(R.string.reminder_time),
                    icon = Icons.Rounded.Notifications,
                    value = reminderAt,
                    onSelected = { reminderAt = it },
                    onClear = { reminderAt = null },
                )
                ReminderModeSelector(
                    selected = reminderMode,
                    onSelected = { reminderMode = it },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(todo, title, note, isImportant, isUrgent, dueAt, reminderAt, reminderMode)
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun visibleTodos(todos: List<TodoEntity>, filter: TodoFilter): List<TodoEntity> =
    when (filter) {
        TodoFilter.Active -> todos.filter { !it.isCompleted }
        TodoFilter.Today -> todos.filter { todo ->
            !todo.isCompleted &&
                (todo.dueAt?.let(TimeText::isToday) == true ||
                    todo.reminderAt?.let(TimeText::isToday) == true)
        }
        TodoFilter.Reminders -> todos.filter { !it.isCompleted && it.reminderAt != null }
        TodoFilter.Completed -> todos.filter { it.isCompleted }
    }

@Composable
private fun TodoEntity.detailsText(): String {
    val details = buildList {
        dueAt?.let { add(stringResource(R.string.due_time_value, TimeText.format(it))) }
        reminderAt?.let {
            add(stringResource(R.string.reminder_time_value, TimeText.format(it)))
            add(reminderModeLabel(reminderMode))
        }
        if (source == TodoSource.VOICE) add(stringResource(R.string.source_voice))
    }
    return details.joinToString(" · ")
}

private data class QuadrantStyle(
    val background: Color,
    val accent: Color,
)

private fun quadrantStyle(quadrant: Quadrant): QuadrantStyle =
    when (quadrant) {
        Quadrant.ImportantUrgent -> QuadrantStyle(
            background = Color(0xFFFFF1ED),
            accent = Color(0xFFD84C2F),
        )
        Quadrant.ImportantNotUrgent -> QuadrantStyle(
            background = Color(0xFFEFF7F4),
            accent = Color(0xFF1C7C6F),
        )
        Quadrant.UrgentNotImportant -> QuadrantStyle(
            background = Color(0xFFFFF8E8),
            accent = Color(0xFFC98512),
        )
        Quadrant.NotImportantNotUrgent -> QuadrantStyle(
            background = Color(0xFFF1F3F8),
            accent = Color(0xFF5E6D8C),
        )
    }

private fun showDateTimePicker(
    context: Context,
    initialMillis: Long?,
    onSelected: (Long) -> Unit,
) {
    val initial = Calendar.getInstance().apply {
        timeInMillis = initialMillis ?: System.currentTimeMillis()
    }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val selected = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onSelected(selected.timeInMillis)
                },
                initial.get(Calendar.HOUR_OF_DAY),
                initial.get(Calendar.MINUTE),
                true,
            ).show()
        },
        initial.get(Calendar.YEAR),
        initial.get(Calendar.MONTH),
        initial.get(Calendar.DAY_OF_MONTH),
    ).show()
}
