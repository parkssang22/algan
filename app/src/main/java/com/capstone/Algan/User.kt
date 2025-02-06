package com.capstone.Algan

// 사용자
data class User(
    val uid: String, // 사용자 ID
    val username: String, // 이름
    val role: String, // 사업주, 고용인
    val phone: String, // 전화번호
    val email: String, // 이메일
    val companyName : String // 회사이름
)
