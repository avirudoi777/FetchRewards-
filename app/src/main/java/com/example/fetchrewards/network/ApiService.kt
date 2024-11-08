package com.example.fetchrewards.network

import com.example.fetchrewards.data.model.Item
import retrofit2.http.GET

interface ApiService {
    @GET("hiring.json")
    suspend fun getItems(): List<Item>
}