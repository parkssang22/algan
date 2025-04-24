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
    val workedHours: String = "", //누적시간
    val userName: String = "" // 근로자 이름 (사용자의 username)
)

// 급여기능 근로자 데이터 클래스
data class CalculatedSalary(
    val uid: String = "",               // 근로자 UID
    val userName: String = "",          // 근로자 이름
    val date: String = "",              // 급여 계산 날짜 또는 기준 날짜
    val workedHours: Double = 0.0,      // 총 근무 시간 (Decimal 변환 후)
    val workDays: Int = 0,
    val hourlyRate: Double = 0.0,       // 시급
    val grossPay: Double = 0.0,         // 총 지급액 = 시급 * 근무시간
    val deductionsPercent: Double = 0.0,// 공제율 (ex. 3.3)
    val deductionsAmount: Double = 0.0, // 공제 금액 = grossPay * (deductionsPercent / 100)
    val netPay: Double = 0.0            // 실지급액 = grossPay - deductionsAmount
)


//세부공제항목
data class Deductions(
    val nationalPension: Double,
    val healthInsurance: Double,
    val employmentInsurance: Double,
    val industrialAccident: Double,
    val longTermCare: Double,
    val incomeTax: Double,
    val other: Double
)

