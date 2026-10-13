package com.example.offlineqr.domain.model

data class Folder(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val itemCount: Int = 0
)
