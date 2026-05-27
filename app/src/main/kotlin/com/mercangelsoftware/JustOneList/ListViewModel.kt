package com.mercangelsoftware.JustOneList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mercangelsoftware.JustOneList.data.ListDatabase
import com.mercangelsoftware.JustOneList.data.ListItemDao
import com.mercangelsoftware.JustOneList.data.ListItemEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListUiState(
    val uncheckedItems: List<ListItemEntity> = emptyList(),
    val checkedItems: List<ListItemEntity> = emptyList()
)

class ListViewModel(private val dao: ListItemDao) : ViewModel() {

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

    fun clearAll() {
        viewModelScope.launch { dao.deleteAll() }
    }

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

class ListViewModelFactory(private val dao: ListItemDao) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ListViewModel(dao) as T
}
