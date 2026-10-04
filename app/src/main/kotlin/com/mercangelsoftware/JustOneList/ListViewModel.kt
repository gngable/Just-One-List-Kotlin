package com.mercangelsoftware.JustOneList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mercangelsoftware.JustOneList.data.ListItemDao
import com.mercangelsoftware.JustOneList.data.ListItemEntity
import com.mercangelsoftware.JustOneList.data.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListUiState(
    val uncheckedItems: List<ListItemEntity> = emptyList(),
    val checkedItems: List<ListItemEntity> = emptyList()
)

class ListViewModel(
    private val dao: ListItemDao,
    private val settings: Settings
) : ViewModel() {

    val keepScreenOn: StateFlow<Boolean> = settings.keepScreenOn

    val uiState: StateFlow<ListUiState> = dao.observeAll()
        .map { entities ->
            ListUiState(
                uncheckedItems = entities.filter { !it.checked },
                checkedItems = entities.filter { it.checked }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ListUiState()
        )

    fun addItem(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch { dao.insert(ListItemEntity(text = trimmed)) }
    }

    fun toggleItem(id: Long, currentlyChecked: Boolean) {
        viewModelScope.launch { dao.setChecked(id, !currentlyChecked) }
    }

    fun editItem(id: Long, text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch { dao.setText(id, trimmed) }
    }

    /** Deletes [items]; pass the same list to [restoreItems] to undo. */
    fun deleteItems(items: List<ListItemEntity>) {
        if (items.isEmpty()) return
        viewModelScope.launch { dao.delete(items) }
    }

    fun restoreItems(items: List<ListItemEntity>) {
        if (items.isEmpty()) return
        viewModelScope.launch { dao.insertAll(items) }
    }

    fun setKeepScreenOn(enabled: Boolean) = settings.setKeepScreenOn(enabled)

    fun reorderItems(orderedIds: List<Long>) {
        viewModelScope.launch {
            orderedIds.forEachIndexed { index, id ->
                dao.updatePosition(id, index.toLong())
            }
        }
    }

    fun importItems(text: String) {
        val items = text.split("\n", "\t", ",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (items.isEmpty()) return
        viewModelScope.launch {
            items.forEach { dao.insert(ListItemEntity(text = it)) }
        }
    }
}

class ListViewModelFactory(
    private val dao: ListItemDao,
    private val settings: Settings
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ListViewModel(dao, settings) as T
}
