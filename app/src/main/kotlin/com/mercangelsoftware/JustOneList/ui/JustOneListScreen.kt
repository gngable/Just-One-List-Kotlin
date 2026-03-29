package com.mercangelsoftware.JustOneList.ui

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mercangelsoftware.JustOneList.ListViewModel
import com.mercangelsoftware.JustOneList.R
import com.mercangelsoftware.JustOneList.data.ListItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JustOneListScreen(viewModel: ListViewModel) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as ComponentActivity).window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.hint_add_item)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (inputText.isNotBlank()) {
                            viewModel.addItem(inputText)
                            inputText = ""
                        }
                    })
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.addItem(inputText)
                        inputText = ""
                    },
                    enabled = inputText.isNotBlank()
                ) {
                    Text(stringResource(R.string.button_add))
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.uncheckedItems, key = { it.id }) { item ->
                    ItemRow(item = item, onToggle = { viewModel.toggleItem(item.id, item.checked) })
                }
                if (uiState.checkedItems.isNotEmpty()) {
                    item {
                        SectionDivider()
                    }
                    items(uiState.checkedItems, key = { it.id }) { item ->
                        ItemRow(item = item, onToggle = { viewModel.toggleItem(item.id, item.checked) })
                    }
                }
            }

            TextButton(
                onClick = { viewModel.clearAll() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.button_clear))
            }
        }
    }
}

@Composable
private fun ItemRow(item: ListItemEntity, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.checked,
            onCheckedChange = { onToggle() }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.text,
            textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
            color = if (item.checked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SectionDivider() {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        HorizontalDivider()
        Text(
            text = "Done",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}
