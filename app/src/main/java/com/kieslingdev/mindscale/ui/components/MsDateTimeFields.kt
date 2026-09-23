package com.kieslingdev.mindscale.ui.components

import android.content.Context
import android.content.res.Configuration
import android.widget.EditText
import android.widget.NumberPicker
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val IsoDate = DateTimeFormatter.ISO_LOCAL_DATE
private val IsoTime = DateTimeFormatter.ofPattern("HH:mm")

fun civilDateFromUtcMillis(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().format(IsoDate)
fun utcMillisFromCivilDate(text: String): Long = LocalDate.parse(text, IsoDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
fun timeText(hour: Int, minute: Int): String = String.format(java.util.Locale.ROOT, "%02d:%02d", hour, minute)

fun readableTime(text: String, format: HourFormat): String {
    val (hour, minute) = wheelTimeOrMidnight(text)
    if (format == HourFormat.TWENTY_FOUR) return timeText(hour, minute)
    val meridiem = if (hour < 12) "AM" else "PM"
    val displayHour = (hour % 12).let { if (it == 0) 12 else it }
    return "$displayHour:${minute.toString().padStart(2, '0')} $meridiem"
}

/** Parses a legacy time defensively; the picker never exposes an out-of-range wheel value. */
fun wheelTimeOrMidnight(text: String): Pair<Int, Int> {
    val parts = text.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
    val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return hour to minute
}

private fun pickerContext(context: Context, dark: Boolean): Context =
    android.view.ContextThemeWrapper(
        context,
        if (dark) android.R.style.Theme_Material_NoActionBar else android.R.style.Theme_Material_Light_NoActionBar
    ).apply {
        applyOverrideConfiguration(Configuration(context.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        })
    }

private fun NumberPicker.configureWheel(textSizePx: Float) {
    wrapSelectorWheel = false
    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        textSize = textSizePx
    } else {
        (0 until childCount).map(::getChildAt).filterIsInstance<EditText>().forEach {
            it.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, textSizePx)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MsDateTimeFields(
    dateText: String,
    timeText: String,
    onDateChanged: (String) -> Unit,
    onTimeChanged: (String) -> Unit,
    hourFormat: HourFormat,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tagPrefix: String = "timestamp"
) {
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MsSpacing.sm)) {
        TextButton(
            enabled = enabled,
            onClick = { showDate = true },
            modifier = Modifier.weight(1f).testTag("${tagPrefix}_date").semantics {
                contentDescription = "Date: $dateText"
            }
        ) { Text(dateText) }
        TextButton(
            enabled = enabled,
            onClick = { showTime = true },
            modifier = Modifier.weight(1f).testTag("${tagPrefix}_time").semantics {
                contentDescription = "Time: ${readableTime(timeText, hourFormat)}"
            }
        ) { Text(readableTime(timeText, hourFormat)) }
    }
    if (showDate) {
        val selected = runCatching { utcMillisFromCivilDate(dateText) }.getOrNull()
        val state = rememberDatePickerState(initialSelectedDateMillis = selected)
        DatePickerDialog(onDismissRequest = { showDate = false }, confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onDateChanged(civilDateFromUtcMillis(it)) }; showDate = false }) { Text("OK") }
        }, dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } }) { DatePicker(state) }
    }
    if (showTime) TimeWheelDialog(timeText, hourFormat, onTimeChanged) { showTime = false }
}

@Composable
private fun TimeWheelDialog(current: String, format: HourFormat, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val initial = wheelTimeOrMidnight(current)
    var hour by rememberSaveable(current, format) { mutableStateOf(initial.first) }
    var minute by rememberSaveable(current, format) { mutableStateOf(initial.second) }
    var am by rememberSaveable { mutableStateOf(hour < 12) }
    val darkPicker = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val wheelTextSizePx = with(LocalDensity.current) { MaterialTheme.typography.bodyLarge.fontSize.toPx() }
    MsDialog(onDismissRequest = onDismiss, title = { Text("Choose time") }, text = {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(MsSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(MsSpacing.sm)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                WheelColumn(label = "Hour", modifier = Modifier.weight(1f)) {
                    AndroidView({ context -> NumberPicker(pickerContext(context, darkPicker)).apply {
                        minValue = if (format == HourFormat.TWENTY_FOUR) 0 else 1
                        maxValue = if (format == HourFormat.TWENTY_FOUR) 23 else 12
                        configureWheel(wheelTextSizePx)
                        contentDescription = "Hour"
                        setOnValueChangedListener { _, _, new -> hour = new }
                    } }, Modifier.testTag("time_hour")) { picker ->
                        picker.value = if (format == HourFormat.TWENTY_FOUR) {
                            hour.coerceIn(0, 23)
                        } else {
                            (hour % 12).let { if (it == 0) 12 else it }
                        }
                    }
                }
                WheelColumn(label = "Minute", modifier = Modifier.weight(1f)) {
                    AndroidView({ context -> NumberPicker(pickerContext(context, darkPicker)).apply {
                        minValue = 0
                        maxValue = 59
                        setFormatter { value -> String.format(java.util.Locale.ROOT, "%02d", value) }
                        configureWheel(wheelTextSizePx)
                        contentDescription = "Minute"
                        setOnValueChangedListener { _, _, new -> minute = new }
                    } }, Modifier.testTag("time_minute")) { picker -> picker.value = minute.coerceIn(0, 59) }
                }
            }
            if (format == HourFormat.TWELVE) {
                Row(
                    Modifier.fillMaxWidth().testTag("time_ampm").semantics { contentDescription = "AM or PM" },
                    horizontalArrangement = Arrangement.Center
                ) {
                    MsChip(text = "AM", selected = am, onClick = { am = true })
                    MsChip(text = "PM", selected = !am, onClick = { am = false })
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { val h = if (format == HourFormat.TWENTY_FOUR) hour else ((hour % 12) + if (am) 0 else 12); onConfirm(timeText(h, minute)); onDismiss() }) { Text("OK") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun WheelColumn(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        content()
    }
}
