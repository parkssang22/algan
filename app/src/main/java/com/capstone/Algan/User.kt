package com.capstone.Algan

// 사업주 데이터 클래스
data class BusinessOwner(
    val uid: String, // 사용자 ID
    val username: String, // 이름
    val role: String = "사업주", // 역할
    val phone: String, // 전화번호
    val email: String, // 이메일
    val companyName: String, // 회사이름
    val companyCode: String // 회사코드
)

data class Employee(
    val uid: String, // 사용자 ID
    val username: String, // 이름
    val role: String = "근로자", // 역할
    val phone: String, // 전화번호
    val email: String, // 이메일
    val companyCode: String, // 회사코드
    val salary: String? = null, // 급여 (null 가능)
    val workingHours: String? = null // 근무시간 (null 가능)
)


