package com.capstone.Algan
import com.google.firebase.firestore.IgnoreExtraProperties;
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
    val userName: String = "", // 근로자 이름 (사용자의 username)
    val worktype:String=""
)

@IgnoreExtraProperties  // 데이터베이스에 추가될 수 있는 예상치 못한 필드 무시
data class CalculatedSalary(
    val uid: String = "",                 // 근로자 고유 ID
    val userName: String = "",            // 근로자 이름
    val date: String = "",                // 급여 계산 기간 (예: "2023-08-01 ~ 2023-08-31")
    val workedHours: Double = 0.0,        // 선택 기간 동안의 총 근무시간 (소수점 가능)
    val workDays: Int = 0,                 // 선택 기간 동안의 근무일수 (출근한 날 수)
    val hourlyRate: Double = 0.0,          // 시간 당 시급
    val grossPay: Double = 0.0,            // 세금 및 공제 전에 계산된 총 급여
    val deductionsPercent: Double = 0.0,   // 전체 공제율(%) (예: 10.0이면 10%)
    val deductionsAmount: Double = 0.0,    // 공제율에 따른 공제 금액
    val netPay: Double = 0.0,               // 공제 후 실 지급 급여
    val deductions: Deductions? = null     // 상세 공제 내역 (국민연금, 건강보험 등)
)


//세부공제항목
@IgnoreExtraProperties  // 예상치 못한 필드 무시
data class Deductions(
    val nationalPension: Double = 0.0,  // 국민연금 공제액
    val healthInsurance: Double = 0.0,  // 건강보험 공제액
    val employmentInsurance: Double = 0.0, // 고용보험 공제액
    val industrialAccident: Double = 0.0,  // 산재보험 공제액
    val longTermCare: Double = 0.0,          // 장기요양 보험 공제액
    val incomeTax: Double = 0.0,             // 소득세 공제액
    val other: Double = 0.0                  // 그 외 기타 공제 항목
)