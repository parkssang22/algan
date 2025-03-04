package com.capstone.Algan.models

data class Message(
    val content: String,
    val timestamp: String,
    val username: String,
    val profileImageUrl: String? = null, // 프로필 이미지 URL (기본값 null)
    val imageUri: String? = null // 메시지에 첨부된 이미지 URI (기본값 null)
)
