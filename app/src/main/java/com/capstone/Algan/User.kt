package com.capstone.Algan


// 사업주 데이터 클래스
data class BusinessOwner(
    val uid: String,
    val username: String,
    val role: String = "사업주",
    val phone: String,
    val email: String,
    val companyName: String, // 회사이름 (사업주만 갖는 필드)
    val companyCode: String
)


// 근로자 데이터 클래스
data class Employee(
    val uid: String,
    val username: String,
    val role: String = "근로자",
    val phone: String,
    val email: String,
    val companyCode: String,
    val companyName: String,
    val salary: String? = null, // 급여 (null 가능)
    val workingHours: String? = null // 근무시간 (null 가능)
)





