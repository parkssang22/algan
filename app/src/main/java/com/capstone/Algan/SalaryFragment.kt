package com.capstone.Algan

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.Algan.databinding.FragmentSalaryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

// 근로자 데이터 클래스
data class Worker(
    val uid: String = "",
    val name: String = "",
    val workDays: Int = 0,
    val totalWorkHours: Double = 0.0,
    val hourlyRate: Double = 0.0,
    val salaryAmount: Double = 0.0,
    val deductions: Double = 0.0,
    val totalDeductions: Double = 0.0
)

class SalaryFragment : Fragment() {
    private lateinit var binding: FragmentSalaryBinding
    private lateinit var database: DatabaseReference
    private lateinit var workerList: MutableList<Worker>
    private lateinit var salaryRecordAdapter: SalaryRecordAdapter

    private var isBusinessOwner = false
    private var companyCode = ""

    // 날짜 형식 지정
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSalaryBinding.inflate(inflater, container, false)

        database = FirebaseDatabase.getInstance().reference
        workerList = mutableListOf()

        setupUI()

        return binding.root
    }

    private fun setupUI() {
        // RecyclerView 설정
        salaryRecordAdapter = SalaryRecordAdapter(requireContext(), mutableListOf())
        binding.SalaryRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = salaryRecordAdapter
        }

        // 근로자 선택 스피너 설정
        binding.spinnerWorker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position >= 0 && position < workerList.size) {
                    val selectedWorker = workerList[position]
                    fetchWorkerSalaryDetails(selectedWorker.uid)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // 날짜 선택 버튼 설정
        binding.tvStartDate.setOnClickListener { showDatePicker(true) }
        binding.tvEndDate.setOnClickListener { showDatePicker(false) }

        // 급여 계산기 버튼 추가
        binding.btnShowSalaryInput.setOnClickListener {
            showSalaryCalculatorDialog() // 급여 계산기 다이얼로그
        }

        // 시급/공제 설정 버튼 추가 - 수정된 부분
        binding.btnShowSalaryInputOwner.setOnClickListener {
            showSalaryInputWholeDialog() // 전체 공제 다이얼로그
        }

        // 사용자 유형 및 데이터 불러오기
        fetchUserTypeAndData()
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = "$selectedYear-${selectedMonth + 1}-$selectedDay"
            if (isStartDate) {
                binding.tvStartDate.text = selectedDate
            } else {
                binding.tvEndDate.text = selectedDate
            }

            // 날짜가 선택되면 급여 데이터 새로고침
            if (binding.tvStartDate.text.isNotEmpty() && binding.tvEndDate.text.isNotEmpty()) {
                refreshSalaryData()
            }
        }, year, month, day).show()
    }

    private fun refreshSalaryData() {
        if (isBusinessOwner) {
            fetchEmployeeList()
        } else {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            calculateAndDisplayEmployeeSalary(userId)
        }
    }

    private fun fetchUserTypeAndData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // 사용자가 비즈니스 소유자인지 확인
        database.child("companies")
            .orderByChild("owner/uid")
            .equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) { // 사용자가 사업주
                        isBusinessOwner = true

                        snapshot.children.forEach { company ->
                            companyCode = company.key ?: ""
                            fetchEmployeeList()
                        }
                    } else { // 사용자가 근로자
                        isBusinessOwner = false

                        database.child("employees").child(userId).get().addOnSuccessListener {
                            companyCode = it.child("companyCode").getValue(String::class.java) ?: ""
                            calculateAndDisplayEmployeeSalary(userId)
                        }
                    }

                    updateVisibilityBasedOnRole()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "데이터 로딩 오류: ${error.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }


    private fun updateVisibilityBasedOnRole() {
        if (isBusinessOwner) {
            binding.LinOwner.visibility = View.VISIBLE
            binding.btnShowSalaryInputOwner.visibility = View.VISIBLE
            binding.spinnerWorker.visibility = View.VISIBLE
        } else {
            binding.LinOwner.visibility = View.GONE
            binding.btnShowSalaryInputOwner.visibility = View.GONE
            binding.spinnerWorker.visibility = View.GONE
        }
    }

    private fun fetchEmployeeList() {
        if (companyCode.isEmpty()) return

        database.child("companies").child(companyCode).child("employees")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    workerList.clear()

                    for (employee in snapshot.children) {
                        val uid = employee.key ?: continue
                        val workerName =
                            employee.child("username").getValue(String::class.java) ?: "Unknown"

                        // Worker 객체 생성 및 추가 (기본값으로)
                        workerList.add(Worker(uid = uid, name = workerName))

                        // 급여 데이터 가져오기
                        fetchWorkerSalaryData(uid, workerName)
                    }

                    // 스피너 업데이트
                    updateSpinner()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "직원 목록 로딩 오류: ${error.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun fetchWorkerSalaryData(uid: String, workerName: String) {
        database.child("worktimes").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val workedHours =
                        snapshot.child("workedHours").getValue(Double::class.java) ?: 0.0
                    val hourlyRate =
                        snapshot.child("hourlyRate").getValue(Double::class.java) ?: 0.0
                    val deductionsPercent =
                        snapshot.child("deductionsPercent").getValue(Double::class.java) ?: 0.0
                    var workDaysCount = 0
                    val grossPay = workedHours * hourlyRate
                    val deductionsAmount = grossPay * (deductionsPercent / 100)
                    val netPay = grossPay - deductionsAmount

                    // 해당 Worker 객체 찾아서 업데이트
                    val index = workerList.indexOfFirst { it.uid == uid }
                    if (index != -1) {
                        workerList[index] = Worker(
                            uid = uid,
                            name = workerName,
                            workDays = workDaysCount,
                            totalWorkHours = workedHours,
                            hourlyRate = hourlyRate,
                            salaryAmount = netPay,
                            deductions = deductionsPercent,
                            totalDeductions = deductionsAmount
                        )

                        // RecyclerView 업데이트
                        salaryRecordAdapter.updateData(workerList)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "급여 데이터 로딩 오류: ${error.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }


    private fun fetchWorkerSalaryDetails(uid: String) {
        val startDate = binding.tvStartDate.text.toString()
        val endDate = binding.tvEndDate.text.toString()

        println("DEBUG: 급여 조회 시작 - UID: $uid, 기간: $startDate ~ $endDate")

        if (startDate.isEmpty() || endDate.isEmpty() ||
            startDate == "시작 날짜를 선택하세요" || endDate == "종료 날짜를 선택하세요") {
            Toast.makeText(context, "날짜 범위를 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }


        // 시급과 공제율 정보 가져오기
        database.child("worktimes").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    // 기본값 설정 (값이 없을 경우)
                    val hourlyRate = userSnapshot.child("hourlyRate").getValue(Double::class.java) ?: 9860.0
                    val deductionsPercent = userSnapshot.child("deductionsPercent").getValue(Double::class.java) ?: 10.0

                    println("DEBUG: 사용자 정보 로드 - 시급: $hourlyRate, 공제율: $deductionsPercent")

                    // 근무 기록 조회 - 회사 코드를 지정하여 조회
                    database.child("worktimes").child(companyCode)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(companySnapshot: DataSnapshot) {
                                var totalWorkedHours = 0.0
                                val workDates = mutableSetOf<String>() // 근무한 날짜를 저장할 Set
                                var username = "Unknown"

                                println("DEBUG: 회사 $companyCode 총 근무 기록 수: ${companySnapshot.childrenCount}")

                                // 근무 기록 순회하며 데이터 수집
                                for (record in companySnapshot.children) {
                                    val recordUid = record.child("uid").getValue(String::class.java)

                                    // 선택한 사용자의 기록만 처리
                                    if (recordUid == uid) {
                                        val date = record.child("date").getValue(String::class.java) ?: continue
                                        val name = record.child("userName").getValue(String::class.java) ?: username
                                        val workedHoursStr = record.child("workedHours").getValue(String::class.java) ?: continue

                                        username = name  // 사용자 이름 업데이트

                                        println("DEBUG: 기록 발견 - 키: ${record.key}, 날짜: $date, 근무시간: $workedHoursStr, UID: $recordUid")

                                        // 날짜가 범위 내에 있는지 확인
                                        if (isDateInRange(date, startDate, endDate)) {
                                            // 근무일수 계산을 위해 날짜 저장
                                            workDates.add(date)

                                            // 근무시간을 시간 단위로 변환
                                            val hours = convertTimeStringToHours(workedHoursStr)

                                            if (hours > 0) {
                                                totalWorkedHours += hours
                                                println("DEBUG: 시간 추가 - 날짜: $date, 시간: $hours, 누적: $totalWorkedHours")
                                            }
                                        }
                                    }
                                }

                                // 세부 공제율 정보 가져오기
                                loadDetailedDeductions(uid) { detailedDeductions ->
                                    // 근무일수는 중복 없는 날짜 개수
                                    val workDaysCount = workDates.size

                                    println("DEBUG: 최종 계산 - 총 근무일수: $workDaysCount, 날짜 목록: $workDates")
                                    println("DEBUG: 최종 계산 - 총 근무시간: $totalWorkedHours")

                                    // 급여 계산
                                    val grossPay = hourlyRate * totalWorkedHours
                                    val deductionsAmount = grossPay * (deductionsPercent / 100)
                                    val netPay = grossPay - deductionsAmount

                                    println("DEBUG: 급여 계산 - 시급: $hourlyRate, 총급여: $grossPay, 공제액: $deductionsAmount, 실수령액: $netPay")

                                    // 화면 업데이트
                                    displayIndividualEmployeeDetails(
                                        CalculatedSalary(
                                            uid = uid,
                                            userName = username,
                                            date = "$startDate ~ $endDate",
                                            workDays = workDaysCount,
                                            workedHours = totalWorkedHours,
                                            hourlyRate = hourlyRate,
                                            grossPay = grossPay,
                                            deductionsPercent = deductionsPercent,
                                            deductionsAmount = deductionsAmount,
                                            netPay = netPay,
                                            deductions = detailedDeductions
                                        )
                                    )

                                    // 근로자 목록 업데이트
                                    val index = workerList.indexOfFirst { it.uid == uid }
                                    if (index != -1) {
                                        workerList[index] = workerList[index].copy(
                                            workDays = workDaysCount,
                                            totalWorkHours = totalWorkedHours,
                                            hourlyRate = hourlyRate,
                                            salaryAmount = netPay,
                                            deductions = deductionsPercent,
                                            totalDeductions = deductionsAmount
                                        )
                                        salaryRecordAdapter.updateData(workerList)
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                println("ERROR: 근무 기록 로드 실패 - ${error.message}")
                                Toast.makeText(context, "근무 기록 로딩 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    println("ERROR: 사용자 정보 로드 실패 - ${error.message}")
                    Toast.makeText(context, "사용자 정보 로딩 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // 세부 공제율 정보 가져오기
    private fun loadDetailedDeductions(uid: String, callback: (Deductions) -> Unit) {
        database.child("worktimes").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val deductions = Deductions(
                        nationalPension = snapshot.child("nationalPension").getValue(Double::class.java) ?: 4.5,
                        healthInsurance = snapshot.child("healthInsurance").getValue(Double::class.java) ?: 3.43,
                        employmentInsurance = snapshot.child("employmentInsurance").getValue(Double::class.java) ?: 0.8,
                        industrialAccident = snapshot.child("industrialAccident").getValue(Double::class.java) ?: 0.65,
                        longTermCare = snapshot.child("longTermCare").getValue(Double::class.java) ?: 1.27,
                        incomeTax = snapshot.child("incomeTax").getValue(Double::class.java) ?: 1.0,
                        other = snapshot.child("other").getValue(Double::class.java) ?: 0.0
                    )
                    callback(deductions)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "상세 공제 정보 로딩 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                    callback(Deductions())
                }
            })
    }

    // 시급과 공제율 정보 가져오기
    private fun loadWorkerRate(uid: String, callback: (hourlyRate: Double, deductions: Double) -> Unit) {
        database.child("worktimes").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val hourlyRate = snapshot.child("hourlyRate").getValue(Double::class.java) ?: 9860.0
                    val deductions = snapshot.child("deductionsPercent").getValue(Double::class.java) ?: 10.0
                    callback(hourlyRate, deductions)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "시급 정보 로딩 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                    callback(9860.0, 10.0) // 최저시급과 기본 공제율
                }
            })
    }

    // "HH:mm" 형식의 문자열을 시간으로 변환 (예: "00:19" -> 0.32시간)
    private fun convertTimeStringToHours(timeStr: String): Double {
        try {
            println("DEBUG: 시간 변환 시작 - 입력: $timeStr")

            // "22:30:01" 형식 처리
            val parts = timeStr.split(":")
            when (parts.size) {
                3 -> { // HH:MM:SS 형식
                    val hours = parts[0].toIntOrNull() ?: 0
                    val minutes = parts[1].toIntOrNull() ?: 0
                    val seconds = parts[2].toIntOrNull() ?: 0

                    val totalHours = hours + (minutes / 60.0) + (seconds / 3600.0)
                    println("DEBUG: HH:MM:SS 형식 변환 결과 - $hours $minutes $seconds = $totalHours")
                    return totalHours
                }
                2 -> { // HH:MM 형식
                    val hours = parts[0].toIntOrNull() ?: 0
                    val minutes = parts[1].toIntOrNull() ?: 0

                    val totalHours = hours + (minutes / 60.0)
                    println("DEBUG: HH:MM 형식 변환 결과 - $hours $ = $totalHours")
                    return totalHours
                }
                1 -> { // 단일 숫자 (시간)
                    val hours = parts[0].toDoubleOrNull() ?: 0.0
                    println("DEBUG: 단일 숫자 형식 변환 결과 - $hours")
                    return hours
                }
                else -> {
                    println("ERROR: 인식할 수 없는 시간 형식 - $timeStr")
                    return 0.0
                }
            }
        } catch (e: Exception) {
            println("ERROR: 시간 변환 오류 - ${e.message}")
            e.printStackTrace()
            return 0.0
        }
    }
    // 출퇴근 시간으로부터 근무 시간을 계산하는 함수
    private fun calculateWorkHours(checkInTime: String, checkOutTime: String): Double {
        try {
            // 시간 형식: HH:mm 또는 HH:mm:ss
            val format = if (checkInTime.count { it == ':' } == 1) {
                SimpleDateFormat("HH:mm", Locale.getDefault())
            } else {
                SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            }

            val checkIn = format.parse(checkInTime)
            val checkOut = format.parse(checkOutTime)

            if (checkIn != null && checkOut != null) {
                // 밀리초 단위로 차이 계산 후 시간으로 변환
                val diffMillis = checkOut.time - checkIn.time
                val diffHours = diffMillis / (1000.0 * 60 * 60)
                println("시간 차이 계산: $checkInTime ~ $checkOutTime = $diffHours 시간")
                return if (diffHours > 0) diffHours else 0.0
            }
            return 0.0
        } catch (e: Exception) {
            println("근무 시간 계산 오류: ${e.message}")
            e.printStackTrace()
            return 0.0
        }
    }

    private fun convertWorkedHoursToDouble(time: String): Double {
        try {
            // 시:분 형식 처리
            val parts = time.split(":")
            if (parts.size == 2 || parts.size == 3) {
                val hours = parts[0].toDoubleOrNull() ?: 0.0
                val minutes = parts[1].toDoubleOrNull() ?: 0.0
                val result = hours + (minutes / 60)
                println("시간 변환: $time -> $result")
                return result
            }

            // 숫자만 있는 경우 처리
            val numericValue = time.toDoubleOrNull()
            if (numericValue != null) {
                println("숫자 시간 변환: $time -> $numericValue")
                return numericValue
            }

            println("시간 변환 실패: $time")
            return 0.0
        } catch (e: Exception) {
            println("시간 변환 중 오류: ${e.message}")
            return 0.0
        }
    }

    // 날짜가 범위 내에 있는지 확인하는 함수
    private fun isDateInRange(dateStr: String, startDateStr: String, endDateStr: String): Boolean {
        try {
            println("DEBUG: 날짜 범위 확인 - 검사할 날짜: $dateStr, 시작일: $startDateStr, 종료일: $endDateStr")

            // 빈 값 체크
            if (startDateStr.isEmpty() || endDateStr.isEmpty() ||
                startDateStr == "시작 날짜를 선택하세요" || endDateStr == "종료 날짜를 선택하세요") {
                return false
            }

            // 날짜 포맷 설정
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // 날짜 문자열 정규화 (YYYY-MM-DD 형식으로)
            val normalizedDate = normalizeDateString(dateStr)
            val normalizedStart = normalizeDateString(startDateStr)
            val normalizedEnd = normalizeDateString(endDateStr)

            println("DEBUG: 정규화된 날짜 - 검사할 날짜: $normalizedDate, 시작일: $normalizedStart, 종료일: $normalizedEnd")

            // 날짜 객체로 변환
            val date = dateFormat.parse(normalizedDate)
            val startDate = dateFormat.parse(normalizedStart)
            val endDate = dateFormat.parse(normalizedEnd)

            if (date == null || startDate == null || endDate == null) {
                println("ERROR: 날짜 변환 실패")
                return false
            }

            // 날짜 범위 확인 (시작일과 종료일 포함)
            val isInRange = (date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0)
            println("DEBUG: 날짜 $normalizedDate 는 범위 내? $isInRange")
            return isInRange

        } catch (e: Exception) {
            println("ERROR: 날짜 범위 확인 중 오류 - ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    // 날짜 문자열을 YYYY-MM-DD 형식으로 정규화하는 함수
    private fun normalizeDateString(dateStr: String): String {
        try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1].padStart(2, '0')
                val day = parts[2].padStart(2, '0')
                val normalized = "$year-$month-$day"
                println("날짜 정규화: $dateStr -> $normalized")
                return normalized
            }
            println("날짜 정규화 실패: $dateStr")
            return dateStr
        } catch (e: Exception) {
            println("날짜 정규화 중 오류: ${e.message}")
            return dateStr
        }
    }
    private fun updateSpinner() {
        val workerNames = workerList.map { it.name }
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, workerNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerWorker.adapter = adapter
    }

    // 개별 근로자의 급여 계산 (근로자 화면용)
    private fun calculateAndDisplayEmployeeSalary(uid: String) {
        // Get start date and end date
        val startDate = binding.tvStartDate.text.toString()
        val endDate = binding.tvEndDate.text.toString()

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(context, "날짜 범위를 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // 시급과 공제율 정보 가져오기
        loadWorkerRate(uid) { hourlyRate, deductionsPercent ->
            // Firebase 구조에 맞게 경로 수정
            database.child("worktimes")
                .orderByChild("uid")
                .equalTo(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        println("DEBUG: Worker records found: ${snapshot.childrenCount}")
                        var totalWorkedHours = 0.0
                        var workDaysCount = 0
                        var username = "Unknown"

                        // 날짜 파싱을 위한 형식 설정
                        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                        // 시작일과 종료일을 Date 객체로 변환
                        val startDateObj = try {
                            dateFormat.parse(normalizeDateString(startDate))
                        } catch (e: Exception) {
                            null
                        }

                        val endDateObj = try {
                            dateFormat.parse(normalizeDateString(endDate))
                        } catch (e: Exception) {
                            null
                        }

                        if (startDateObj == null || endDateObj == null) {
                            Toast.makeText(context, "날짜 형식이 올바르지 않습니다", Toast.LENGTH_SHORT).show()
                            return
                        }

                        // 각 근무 기록 확인
                        for (recordSnapshot in snapshot.children) {
                            val recordDate = recordSnapshot.child("date").getValue(String::class.java) ?: ""
                            val username_record = recordSnapshot.child("userName").getValue(String::class.java)
                            if (username_record != null) {
                                username = username_record
                            }

                            // 날짜 객체로 변환
                            val recordDateObj = try {
                                dateFormat.parse(recordDate)
                            } catch (e: Exception) {
                                continue // 날짜 변환 실패시 다음 기록으로
                            }

                            // 날짜 범위 내에 있는지 확인
                            if (recordDateObj != null &&
                                !recordDateObj.before(startDateObj) &&
                                !recordDateObj.after(endDateObj)) {

                                // workedHours 가져오기 및 변환
                                val workedHoursStr = recordSnapshot.child("workedHours").getValue(String::class.java) ?: "00:00"
                                val hours = convertTimeStringToHours(workedHoursStr)

                                if (hours > 0) {
                                    totalWorkedHours += hours
                                    workDaysCount++
                                    println("근로자 화면 - 날짜: $recordDate, 시간: $hours, 누적: $totalWorkedHours")
                                }
                            }
                        }

                        // 세부 공제율 정보 가져오기
                        loadDetailedDeductions(uid) { detailedDeductions ->
                            // 급여 계산
                            val grossPay = totalWorkedHours * hourlyRate
                            val deductionsAmount = grossPay * (deductionsPercent / 100)
                            val netPay = grossPay - deductionsAmount

                            // 근무 시간을 소수점 한 자리까지 표시
                            val formattedWorkHours = String.format("%.1f", totalWorkedHours).toDouble()

                            // 개인 근로자 화면에 표시
                            displayIndividualEmployeeDetails(
                                CalculatedSalary(
                                    uid = uid,
                                    userName = username,
                                    date = "$startDate ~ $endDate",
                                    workDays = workDaysCount,
                                    workedHours = formattedWorkHours,
                                    hourlyRate = hourlyRate,
                                    grossPay = grossPay,
                                    deductionsPercent = deductionsPercent,
                                    deductionsAmount = deductionsAmount,
                                    netPay = netPay,
                                    deductions = detailedDeductions
                                )
                            )
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "급여 계산 오류: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    // 화면에 급여 표시
    private fun displayIndividualEmployeeDetails(salaryData: CalculatedSalary) {
        println("DEBUG: displayIndividualEmployeeDetails 호출됨")
        println("DEBUG: 급여 데이터 - 이름: ${salaryData.userName}, 실지급액: ${salaryData.netPay}, 총급여: ${salaryData.grossPay}")
        println("DEBUG: 급여 데이터 - 근무일수: ${salaryData.workDays}, 근무시간: ${salaryData.workedHours}")

        try {
            with(binding) {
                // 실 지급액 표시 (천 단위 콤마 포맷 적용)
                val netPayFormatted = String.format("%,d", salaryData.netPay.toInt())
                salaryAmount.text = "실 지급액: ${netPayFormatted}원"
                println("DEBUG: 실지급액 설정 완료 - ${netPayFormatted}원")

                // 총 급여액 표시 (tvGrossPay 사용)
                val grossPayFormatted = String.format("%,d", salaryData.grossPay.toInt())
                try {
                    val tvGrossPay = root.findViewById<TextView>(R.id.tvGrossPay)
                    if (tvGrossPay != null) {
                        tvGrossPay.text = "총 급여액: ${grossPayFormatted}원"
                        println("DEBUG: 총급여액 설정 완료 - ${grossPayFormatted}원")
                    } else {
                        println("ERROR: tvGrossPay 뷰를 찾을 수 없음")
                    }
                } catch (e: Exception) {
                    println("ERROR: tvGrossPay 설정 중 오류 - ${e.message}")
                }

                // 근무일수 표시
                workDays.text = "근무일수: ${salaryData.workDays}일"
                println("DEBUG: 근무일수 설정 완료 - ${salaryData.workDays}일")

                // 근무시간 표시 - 소수점 한 자리까지
                val formattedWorkHours = String.format("%.1f", salaryData.workedHours)
                totalWorkHours.text = "총 근무시간: ${formattedWorkHours}시간"
                println("DEBUG: 총근무시간 설정 완료 - ${formattedWorkHours}시간")

                // 시급 표시 (천 단위 콤마)
                val hourlyRateFormatted = String.format("%,d", salaryData.hourlyRate.toInt())
                hourlyRate.text = "시급: ${hourlyRateFormatted}원"
                println("DEBUG: 시급 설정 완료 - ${hourlyRateFormatted}원")

                // 공제율 표시
                deductions.text = "공제율: ${salaryData.deductionsPercent}%"
                println("DEBUG: 공제율 설정 완료 - ${salaryData.deductionsPercent}%")

                // 세부 공제 항목 표시 (실제 세부 공제율 사용)
                val deductionsData = salaryData.deductions ?: Deductions()

                // 각 공제 항목별 금액 계산 (실제 비율 적용)
                val nationalPensionAmount = (salaryData.grossPay * (deductionsData.nationalPension / 100)).toInt()
                val healthInsuranceAmount = (salaryData.grossPay * (deductionsData.healthInsurance / 100)).toInt()
                val employmentInsuranceAmount = (salaryData.grossPay * (deductionsData.employmentInsurance / 100)).toInt()
                val industrialAccidentAmount = (salaryData.grossPay * (deductionsData.industrialAccident / 100)).toInt()
                val longTermCareAmount = (salaryData.grossPay * (deductionsData.longTermCare / 100)).toInt()
                val incomeTaxAmount = (salaryData.grossPay * (deductionsData.incomeTax / 100)).toInt()

                // 천 단위 콤마 포맷 적용
                tvrate1.text = "국민연금(${deductionsData.nationalPension}%): ${String.format("%,d", nationalPensionAmount)}원"
                tvrate2.text = "건강보험(${deductionsData.healthInsurance}%): ${String.format("%,d", healthInsuranceAmount)}원"
                tvrate3.text = "고용보험(${deductionsData.employmentInsurance}%): ${String.format("%,d", employmentInsuranceAmount)}원"
                tvrate4.text = "산재보험(${deductionsData.industrialAccident}%): ${String.format("%,d", industrialAccidentAmount)}원"
                tvrate5.text = "장기요양(${deductionsData.longTermCare}%): ${String.format("%,d", longTermCareAmount)}원"
                tvrate6.text = "소득세(${deductionsData.incomeTax}%): ${String.format("%,d", incomeTaxAmount)}원"

                // 총 공제액 표시
                val totalDeductionsFormatted = String.format("%,d", salaryData.deductionsAmount.toInt())
                tvrate7.text = "총 공제액: ${totalDeductionsFormatted}원"
                println("DEBUG: 세부 공제 항목 설정 완료")
            }
        } catch (e: Exception) {
            println("ERROR: 급여 정보 표시 중 오류 발생 - ${e.message}")
            e.printStackTrace()
        }
    }

    // 급여 입력 다이얼로그 표시 (전체 공제 버튼)
    private fun showSalaryInputWholeDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_salary_input_whole)

        val etHourlyRateWhole: EditText = dialog.findViewById(R.id.etHourlyRateWhole)
        val etDeductionsWhole: EditText = dialog.findViewById(R.id.etDeductionsWhole)
        val cbRate: CheckBox = dialog.findViewById(R.id.cbRate)
        val btnSetSalaryWhole: Button = dialog.findViewById(R.id.btnSetSalaryWhole)

        // 세부 공제율 입력 필드들
        val etRate1: EditText = dialog.findViewById(R.id.etRate1)
        val etRate2: EditText = dialog.findViewById(R.id.etRate2)
        val etRate3: EditText = dialog.findViewById(R.id.etRate3)
        val etRate4: EditText = dialog.findViewById(R.id.etRate4)
        val etRate5: EditText = dialog.findViewById(R.id.etRate5)
        val etRate6: EditText = dialog.findViewById(R.id.etRate6)
        val etRate7: EditText = dialog.findViewById(R.id.etRate7)

        // 선택된 근로자의 현재 시급과 공제율 가져오기
        val position = binding.spinnerWorker.selectedItemPosition
        if (position == -1 || position >= workerList.size) {
            Toast.makeText(context, "근로자를 먼저 선택해주세요.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            return
        }

        val selectedWorker = workerList[position]

        // 현재 설정된 값 표시
        etHourlyRateWhole.setText(selectedWorker.hourlyRate.toString())
        etDeductionsWhole.setText(selectedWorker.deductions.toString())

        // 기본값 설정
        etRate1.setText("4.5")  // 국민연금
        etRate2.setText("3.43") // 건강보험
        etRate3.setText("0.8")  // 고용보험
        etRate4.setText("0.65") // 산재보험
        etRate5.setText("1.27") // 장기요양
        etRate6.setText("1.0")  // 소득세
        etRate7.setText("0.0")  // 기타

        // 세부설정 체크박스에 따라 항목 가시성 제어
        val detailFields = listOf(
            etRate1, etRate2, etRate3, etRate4, etRate5, etRate6, etRate7,
            dialog.findViewById<TextView>(R.id.textView),
            dialog.findViewById<TextView>(R.id.textView2),
            dialog.findViewById<TextView>(R.id.textView3),
            dialog.findViewById<TextView>(R.id.textView4),
            dialog.findViewById<TextView>(R.id.textView5),
            dialog.findViewById<TextView>(R.id.textView6),
            dialog.findViewById<TextView>(R.id.textView7)
        )

        // 초기 상태 설정 - 세부항목 숨김
        detailFields.forEach { it.visibility = View.GONE }

        cbRate.setOnCheckedChangeListener { _, isChecked ->
            val visibility = if (isChecked) View.VISIBLE else View.GONE
            detailFields.forEach { it.visibility = visibility }
        }

        btnSetSalaryWhole.setOnClickListener {
            val hourlyRateValue = etHourlyRateWhole.text.toString().toDoubleOrNull() ?: 0.0
            val deductionsValue = if (cbRate.isChecked) {
                // 세부 공제율 항목 합산
                (etRate1.text.toString().toDoubleOrNull() ?: 0.0) +
                        (etRate2.text.toString().toDoubleOrNull() ?: 0.0) +
                        (etRate3.text.toString().toDoubleOrNull() ?: 0.0) +
                        (etRate4.text.toString().toDoubleOrNull() ?: 0.0) +
                        (etRate5.text.toString().toDoubleOrNull() ?: 0.0) +
                        (etRate6.text.toString().toDoubleOrNull() ?: 0.0) +
                        (etRate7.text.toString().toDoubleOrNull() ?: 0.0)
            } else {
                etDeductionsWhole.text.toString().toDoubleOrNull() ?: 0.0
            }
            // 세부 공제율 항목 저장
            val detailedDeductionsData = if (cbRate.isChecked) {
                Deductions(
                    nationalPension = etRate1.text.toString().toDoubleOrNull() ?: 0.0,
                    healthInsurance = etRate2.text.toString().toDoubleOrNull() ?: 0.0,
                    employmentInsurance = etRate3.text.toString().toDoubleOrNull() ?: 0.0,
                    industrialAccident = etRate4.text.toString().toDoubleOrNull() ?: 0.0,
                    longTermCare = etRate5.text.toString().toDoubleOrNull() ?: 0.0,
                    incomeTax = etRate6.text.toString().toDoubleOrNull() ?: 0.0,
                    other = etRate7.text.toString().toDoubleOrNull() ?: 0.0
                )
            } else null

            // 시작일자와 종료일자 가져오기
            val myStartDate = binding.tvStartDate.text.toString()
            val myEndDate = binding.tvEndDate.text.toString()

            // 필요한 급여 데이터 가져오기
            val myWorkDays = binding.workDays.text.toString().replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
            val myWorkedHours = binding.totalWorkHours.text.toString().replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0

            // 총급여와 공제액 계산
            val myGrossPay = hourlyRateValue * myWorkedHours
            val myDeductionsAmount = myGrossPay * (deductionsValue / 100)
            val myNetPay = myGrossPay - myDeductionsAmount

            // 데이터베이스에 시급과 공제율 저장
            val dbUpdates = mutableMapOf<String, Any>()

            // 기본 시급과 공제율 저장
            dbUpdates["hourlyRate"] = hourlyRateValue
            dbUpdates["deductionsPercent"] = deductionsValue

            // 세부 공제율이 있으면 추가
            if (detailedDeductionsData != null) {
                dbUpdates["nationalPension"] = detailedDeductionsData.nationalPension
                dbUpdates["healthInsurance"] = detailedDeductionsData.healthInsurance
                dbUpdates["employmentInsurance"] = detailedDeductionsData.employmentInsurance
                dbUpdates["industrialAccident"] = detailedDeductionsData.industrialAccident
                dbUpdates["longTermCare"] = detailedDeductionsData.longTermCare
                dbUpdates["incomeTax"] = detailedDeductionsData.incomeTax
                dbUpdates["other"] = detailedDeductionsData.other
            }
            // 데이터베이스 업데이트 (worktimes 노드에 저장)
            database.child("worktimes").child(selectedWorker.uid)
                .updateChildren(dbUpdates)
                .addOnSuccessListener {
                    Toast.makeText(context, "시급과 공제율이 업데이트되었습니다.", Toast.LENGTH_SHORT).show()

                    // 업데이트 후 즉시 화면에 반영
                    displayIndividualEmployeeDetails(
                        CalculatedSalary(
                            uid = selectedWorker.uid,
                            userName = selectedWorker.name,
                            date = "$myStartDate ~ $myEndDate",
                            workDays = myWorkDays,
                            workedHours = myWorkedHours,
                            hourlyRate = hourlyRateValue,
                            grossPay = myGrossPay,
                            deductionsPercent = deductionsValue,
                            deductionsAmount = myDeductionsAmount,
                            netPay = myNetPay,
                            deductions = detailedDeductionsData
                        )
                    )

                    // 근로자 목록 데이터 업데이트
                    val index = workerList.indexOfFirst { it.uid == selectedWorker.uid }
                    if (index != -1) {
                        workerList[index] = workerList[index].copy(
                            hourlyRate = hourlyRateValue,
                            deductions = deductionsValue
                        )
                        salaryRecordAdapter.updateData(workerList)
                    }

                    // 저장 후 데이터 새로고침
                    fetchWorkerSalaryDetails(selectedWorker.uid)

                    dialog.dismiss()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(context, "업데이트 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // 다이얼로그 크기 설정
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }

    // 현재 날짜를 가져오는 함수
    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "$year-$month-$day"
    }

    // 일일 근무 시간 데이터 저장
    private fun saveDailyWorkHours(
        employeeUid: String,
        date: String,
        workHours: Double,
        hourlyRate: Double,
        deductions: Double
    ) {
        if (companyCode.isEmpty()) {
            Toast.makeText(context, "회사 코드를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val salaryData = mapOf(
            "workedHours" to workHours
        )

        // 시급과 공제율은 salary 노드의 최상위에 저장
        val hourlyRateData = mapOf(
            "hourlyRate" to hourlyRate,
            "deductionsPercent" to deductions
        )

        // 해당 날짜의 근무 시간 데이터 저장
        database.child("salary").child(employeeUid).child(date).updateChildren(salaryData)
            .addOnSuccessListener {
                // 시급과 공제율은 salary 최상위에 별도로 저장
                database.child("salary").child(employeeUid).updateChildren(hourlyRateData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "근무 데이터가 저장되었습니다.", Toast.LENGTH_SHORT)
                            .show()

                        // UI 업데이트를 위해 데이터 다시 불러오기
                        fetchWorkerSalaryDetails(employeeUid)
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(
                            requireContext(),
                            "시급/공제율 저장 실패: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    requireContext(),
                    "근무 데이터 저장 실패: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // 시급과 공제율만 저장하는 함수
    private fun saveSalaryRateAndDeductions(
        employeeUid: String,
        hourlyRate: Double,
        deductions: Double,
        detailedDeductions: Deductions? = null
    ) {
        if (companyCode.isEmpty()) {
            Toast.makeText(context, "회사 코드를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 저장할 날짜 기간 구하기
        val startDate = binding.tvStartDate.text.toString()
        val endDate = binding.tvEndDate.text.toString()

        // 연-월 구하기 (시작 날짜 기준)
        val yearMonth = getYearMonth(startDate)

        // 저장할 데이터 구성
        val salaryData = mutableMapOf<String, Any>(
            "hourlyRate" to hourlyRate,
            "deductionsPercent" to deductions,
            "date" to "$startDate ~ $endDate",
            "userName" to (workerList.find { it.uid == employeeUid }?.name ?: "Unknown")
        )

        // 세부 공제율 정보가 있으면 추가
        detailedDeductions?.let {
            val deductionsMap = mapOf(
                "nationalPension" to it.nationalPension,
                "healthInsurance" to it.healthInsurance,
                "employmentInsurance" to it.employmentInsurance,
                "industrialAccident" to it.industrialAccident,
                "longTermCare" to it.longTermCare,
                "incomeTax" to it.incomeTax,
                "other" to it.other
            )
            salaryData["deductions"] = deductionsMap
        }

        // 근로자의 salary/[uid]/[연월] 에 시급과 공제율 데이터 저장
        database.child("salary").child(employeeUid).child(yearMonth)
            .updateChildren(salaryData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "$yearMonth 급여 데이터가 업데이트되었습니다.", Toast.LENGTH_SHORT).show()

                // 근로자 목록 데이터 업데이트
                val index = workerList.indexOfFirst { it.uid == employeeUid }
                if (index != -1) {
                    val updatedWorker = workerList[index].copy(
                        hourlyRate = hourlyRate,
                        deductions = deductions
                    )
                    workerList[index] = updatedWorker
                    salaryRecordAdapter.updateData(workerList)
                }

                // 급여 다시 계산 및 표시 - 선택된 근로자의 급여 정보 새로고침
                fetchWorkerSalaryDetails(employeeUid)
            }
            .addOnFailureListener { error ->
                Toast.makeText(requireContext(), "데이터 저장에 실패했습니다: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // 급여 데이터 저장
    private fun saveSalaryData(
        employeeUid: String,
        yearMonth: String,  // "2024-04" 같은 형식
        salaryData: Map<String, Any>
    ) {
        if (companyCode.isEmpty()) {
            Toast.makeText(context, "회사 코드를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // salary/[uid]/[연월] 구조로 저장
        database.child("salary").child(employeeUid).child(yearMonth)
            .setValue(salaryData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "$yearMonth 급여 데이터가 저장되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(requireContext(), "데이터 저장에 실패했습니다: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // "2024-04" 같은 연-월 문자열 생성 함수
    private fun getYearMonth(date: String): String {
        try {
            val parts = date.split("-")
            if (parts.size >= 2) {
                return "${parts[0]}-${parts[1].padStart(2, '0')}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 현재 월 반환 (기본값)
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        return "$year-$month"
    }

    // 급여 출력 다이얼로그
    private fun showSalaryOutputDialog(
        finalSalary: Double,
        workTime: String,
        workHours: Double,
        hourlyRate: Double,
        deductions: Double
    ) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_salary_output_calc)

        val salaryAmountD: TextView = dialog.findViewById(R.id.salaryAmountD)
        val totalWorkHoursD: TextView = dialog.findViewById(R.id.totalWorkHoursD)
        val hourlyRateD: TextView = dialog.findViewById(R.id.hourlyRateD)
        val deductionsD: TextView = dialog.findViewById(R.id.deductionsD)
        val btnCloseOutput: Button = dialog.findViewById(R.id.btnCloseOutput)

        // 급여 계산 후 화면에 표시
        salaryAmountD.text = "실 지급액: ${finalSalary.toInt()}원"
        totalWorkHoursD.text = "총 근무시간: ${workHours}시간"
        hourlyRateD.text = "시급: ${hourlyRate.toInt()}원"
        deductionsD.text = "공제: ${deductions}%"

        btnCloseOutput.setOnClickListener {
            dialog.dismiss()
        }

        // 다이얼로그 크기 설정
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog.show()
    }

    // 급여 계산기 다이얼로그 표시
    private fun showSalaryCalculatorDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_salary_input_calc) // 급여 계산기 레이아웃

        // 필요한 UI 요소들 찾기 및 처리
        val btnSet: Button = dialog.findViewById(R.id.btnSetSalary)
        val etHourlyRate: EditText = dialog.findViewById(R.id.etHourlyRate)
        val etDeductions: EditText = dialog.findViewById(R.id.etDeductions)
        val etWorkHours: EditText = dialog.findViewById(R.id.etWorkHours) // 근무시간 필드 추가

        // 선택된 근로자가 있으면 현재 설정된 시급과 공제율 표시 (읽기 전용)
        val position = binding.spinnerWorker.selectedItemPosition
        if (position != -1 && position < workerList.size) {
            val selectedWorker = workerList[position]
            etHourlyRate.setText(selectedWorker.hourlyRate.toString())
            etDeductions.setText(selectedWorker.deductions.toString())
        }

        // 버튼 클릭 시 계산만 수행
        btnSet.setOnClickListener {
            val hourlyRate = etHourlyRate.text.toString().toDoubleOrNull() ?: 0.0
            val deductions = etDeductions.text.toString().toDoubleOrNull() ?: 0.0
            val workHours = etWorkHours.text.toString().toDoubleOrNull() ?: 0.0

            // 실 지급액 계산
            val grossPay = hourlyRate * workHours
            val deductionAmount = grossPay * (deductions / 100)
            val finalSalary = grossPay - deductionAmount

            // 계산 결과만 표시
            showSalaryOutputDialog(finalSalary, getCurrentDate(), workHours, hourlyRate, deductions)
            dialog.dismiss()
        }

        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }

    // SalaryRecordAdapter
    class SalaryRecordAdapter(
        private val context: Context,
        private var workRecordList: List<Worker>
    ) : RecyclerView.Adapter<SalaryRecordAdapter.SalaryViewHolder>() {

        inner class SalaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNumber: TextView = view.findViewById(R.id.tvNumber)
            val tvWorkerName: TextView = view.findViewById(R.id.tvWorkerName)
            val salaryAmount: TextView = view.findViewById(R.id.salaryAmount)
            val workDays: TextView = view.findViewById(R.id.workDays)
            val totalWorkHours: TextView = view.findViewById(R.id.totalWorkHours)
            val hourlyRate: TextView = view.findViewById(R.id.hourlyRate)
            val deductions: TextView = view.findViewById(R.id.deductions)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalaryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_salary, parent, false)
            return SalaryViewHolder(view)
        }
//사업주화면 전체 근로자 급여 내역
        override fun onBindViewHolder(holder: SalaryViewHolder, position: Int) {
            val workRecord = workRecordList[position]
            holder.tvNumber.text = (position + 1).toString()
            holder.tvWorkerName.text = workRecord.name
            holder.salaryAmount.text = "급여: ${workRecord.salaryAmount.toInt()}원"
            holder.workDays.text = "근무일수: ${workRecord.workDays}일"
            holder.totalWorkHours.text = "총 근무시간: ${workRecord.totalWorkHours}시간"
            holder.hourlyRate.text = "시급: ${workRecord.hourlyRate.toInt()}원"
            holder.deductions.text = "공제: ${workRecord.deductions}%"
        }

        override fun getItemCount(): Int = workRecordList.size

        fun updateData(newData: List<Worker>) {
            workRecordList = newData
            notifyDataSetChanged()
        }
    }
}