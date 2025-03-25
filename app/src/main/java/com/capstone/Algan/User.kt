package com.capstone.Algan

// 사업주 데이터 클래스
data class BusinessOwner(
    val uid: String = "",
    val username: String = "",
    val role: String = "사업주",
    val phone: String = "",
    val email: String = "",
    val companyName: String = "", // 회사이름 (사업주만 갖는 필드)
    val companyCode: String = ""
)

// 근로자 데이터 클래스
data class Employee(
    val uid: String = "",
    val username: String = "",
    val role: String = "근로자",
    val phone: String = "",
    val email: String = "",
    val companyCode: String = "",
    val companyName: String = "",
    val salary: String? = null, // 급여 (null 가능)
    val workingHours: String? = null // 근무시간 (null 가능)
)

//체크리스트 데이터 클래스
data class CheckList(
    val uid: String = "",
    val username: String = "",
    val contents: String = "",
    val date: String = "",
    val time: String = "",
    var status: String = "미완료"
)

// 출퇴근 기록 데이터 클래스
data class WorkTime(
    val id: String? = null, // Firebase에서 자동 생성되는 키를 사용하므로 String? 타입으로 변경
    val uid: String = "",
    val date: String = "",
    val clockIn: String = "",
    val clockOut: String = "",
    val workedHours: String = "",
    val hourlyRate: String = "", // 시급
    val userName: String = "" // 근로자 이름 (사용자의 username)
)