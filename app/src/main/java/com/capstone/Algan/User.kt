package com.capstone.Algan

// 사용자
data class User(
    val id: String, // 아이디
    val username: String, // 이름
    val password: String, // 비밀번호
    val role: String, // 사업주, 고용인
    val phone: String, // 전화번호
    val email: String, // 이메일
    val companyName: String? = null // 회사 이름 (사업주만 필요)
)
