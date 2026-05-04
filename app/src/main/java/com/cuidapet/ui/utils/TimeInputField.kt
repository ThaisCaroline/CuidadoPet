package com.cuidadopet.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions

// Campo de horário reutilizável com auto-formatação HH:mm.
// Toda a lógica de formatação vive aqui — edite este arquivo para corrigir bugs
// em todos os cadastros de horário do app de uma só vez.
@Composable
fun TimeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "08:00",
    supportingText: (@Composable () -> Unit)? = null
) {
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    LaunchedEffect(value) {
        if (fieldValue.text != value) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { new ->
            val formatted = if (new.text.length >= fieldValue.text.length)
                autoFormatTime(new.text) else new.text
            val cursor = cursorAfterFormat(new.text, new.selection.end, formatted)
            fieldValue = TextFieldValue(formatted, TextRange(cursor))
            onValueChange(formatted)
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = modifier.onFocusChanged { fs ->
            if (!fs.hasFocus && fieldValue.text.isNotBlank()) {
                val normalized = normalizeTime(fieldValue.text)
                fieldValue = TextFieldValue(normalized, TextRange(normalized.length))
                onValueChange(normalized)
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        leadingIcon = {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        suffix = { Text("h") },
        supportingText = supportingText
    )
}
