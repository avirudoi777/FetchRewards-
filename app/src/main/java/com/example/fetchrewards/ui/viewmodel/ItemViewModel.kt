package com.example.fetchrewards.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fetchrewards.data.model.Item
import com.example.fetchrewards.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class ItemViewModel(
    private val repository: ItemRepository = ItemRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<ItemUiState>(ItemUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        fetchItems()
    }

    fun fetchItems() {
        viewModelScope.launch {
            try {
                _uiState.value = ItemUiState.Loading

                repository.getItems()
                    .fold(
                        onSuccess = { items ->
                            try {
                                if (items.isEmpty()) {
                                    _uiState.value = ItemUiState.Error(ItemError.EmptyList)
                                    return@fold
                                }

                                val filteredAndSortedItems = items
                                    .filter { !it.name.isNullOrBlank() }
                                    .sortedWith(
                                        compareBy<Item> { it.listId }
                                            .thenBy { it.name ?: "" }
                                    )
                                    .groupBy { it.listId }

                                if (filteredAndSortedItems.isEmpty()) {
                                    _uiState.value = ItemUiState.Error(ItemError.NoValidItems)
                                } else {
                                    _uiState.value = ItemUiState.Success(filteredAndSortedItems)
                                }
                            } catch (e: Exception) {
                                _uiState.value = ItemUiState.Error(ItemError.ProcessingError(e))
                            }
                        },
                        onFailure = { exception ->
                            val error = when (exception) {
                                is IOException -> ItemError.Network
                                is retrofit2.HttpException -> {
                                    when (exception.code()) {
                                        404 -> ItemError.NotFound
                                        500 -> ItemError.Server
                                        else -> ItemError.Http(exception.code())
                                    }
                                }
                                else -> ItemError.Unexpected(exception)
                            }
                            _uiState.value = ItemUiState.Error(error)
                        }
                    )
            } catch (e: Exception) {
                _uiState.value = ItemUiState.Error(ItemError.Unexpected(e))
            }
        }
    }

    sealed interface ItemUiState {
        data class Success(val groupedItems: Map<Int, List<Item>>) : ItemUiState
        object Loading : ItemUiState
        data class Error(val error: ItemError) : ItemUiState
    }

    sealed class ItemError {
        object EmptyList : ItemError()
        object NoValidItems : ItemError()
        object Network : ItemError()
        object NotFound : ItemError()
        object Server : ItemError()
        data class Http(val code: Int) : ItemError()
        data class ProcessingError(val exception: Exception) : ItemError()
        data class Unexpected(val exception: Throwable) : ItemError()
    }
}
