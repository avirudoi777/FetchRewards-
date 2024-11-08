package com.example.fetchrewards.data.repository

import com.example.fetchrewards.data.model.Item
import com.example.fetchrewards.network.ApiService
import com.example.fetchrewards.network.RetrofitClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ItemRepository(
    private val apiService: ApiService = RetrofitClient.apiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun getItems(): Result<List<Item>> = withContext(dispatcher) {
        try {
            Result.success(apiService.getItems())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}