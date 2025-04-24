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
        database.child("worktime").child(uid)
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

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(context, "날짜 범위를 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        database.child("worktime").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalWorkedHours = 0.0
                    var workDaysCount = 0

                    val hourlyRate =
                        snapshot.child("hourlyRate").getValue(Double::class.java) ?: 0.0
                    val deductionsPercent =
                        snapshot.child("deductionsPercent").getValue(Double::class.java) ?: 0.0
                    val username =
                        snapshot.child("username").getValue(String::class.java) ?: "Unknown"

                    for (dateSnapshot in snapshot.children) {
                        val dateKey = dateSnapshot.key
                        if (dateKey != null && dateKey.matches(Regex("\\d{4}-\\d{1,2}-\\d{1,2}"))) {
                            if (isDateInRange(dateKey, startDate, endDate)) {
                                val workedHoursValue = dateSnapshot.child("workedHours").getValue()
                                val dailyHours = when (workedHoursValue) {
                                    is String -> convertWorkedHoursToDouble(workedHoursValue)
                                    is Double -> workedHoursValue
                                    is Long -> workedHoursValue.toDouble()
                                    else -> 0.0
                                }
                                if (dailyHours > 0) {
                                    totalWorkedHours += dailyHours
                                    workDaysCount++
                                }
                            }
                        }
                    }

                    val grossPay = totalWorkedHours * hourlyRate
                    val deductionsAmount = grossPay * (deductionsPercent / 100)
                    val netPay = grossPay - deductionsAmount

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
                            netPay = netPay
                        )
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "상세 데이터 로딩 오류: ${error.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun convertWorkedHoursToDouble(time: String): Double {
        val parts = time.split(":")
        if (parts.size == 2 || parts.size == 3) {
            val hours = parts[0].toDoubleOrNull() ?: 0.0
            val minutes = parts[1].toDoubleOrNull() ?: 0.0
            return hours + (minutes / 60)
        }
        return 0.0
    }

    // 날짜가 범위 내에 있는지 확인하는 함수
    private fun isDateInRange(dateStr: String, startDateStr: String, endDateStr: String): Boolean {
        try {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val date = dateFormat.parse(normalizeDateString(dateStr))
            val startDate = dateFormat.parse(normalizeDateString(startDateStr))
            val endDate = dateFormat.parse(normalizeDateString(endDateStr))

            return date != null && startDate != null && endDate != null &&
                    (date.after(startDate) || date == startDate) &&
                    (date.before(endDate) || date == endDate)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // 날짜 문자열을 YYYY-MM-DD 형식으로 정규화하는 함수
    private fun normalizeDateString(dateStr: String): String {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val month = parts[1].padStart(2, '0')
            val day = parts[2].padStart(2, '0')
            return "$year-$month-$day"
        }
        return dateStr
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
        // 시작 날짜와 종료 날짜 가져오기
        val startDate = binding.tvStartDate.text.toString()
        val endDate = binding.tvEndDate.text.toString()

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(context, "날짜 범위를 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        database.child("worktime").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalWorkedHours = 0.0
                    var workDaysCount = 0  // 근무일수 카운터 추가
                    val hourlyRate =
                        snapshot.child("hourlyRate").getValue(Double::class.java) ?: 0.0
                    val deductionsPercent =
                        snapshot.child("deductionsPercent").getValue(Double::class.java) ?: 0.0
                    val username =
                        snapshot.child("username").getValue(String::class.java) ?: "Unknown"

                    // worktime 하위의 날짜별 데이터를 조회
                    for (dateSnapshot in snapshot.children) {
                        val dateKey = dateSnapshot.key

                        // 날짜 형식의 키만 처리 (다른 필드는 건너뜀)
                        if (dateKey != null && dateKey.matches(Regex("\\d{4}-\\d{1,2}-\\d{1,2}"))) {
                            // 날짜가 범위 내에 있는지 확인
                            if (isDateInRange(dateKey, startDate, endDate)) {
                                // 해당 날짜의 근무 시간 합산
                                val dailyHours =
                                    dateSnapshot.child("workedHours").getValue(Double::class.java)
                                        ?: 0.0
                                totalWorkedHours += dailyHours
                                workDaysCount++  // 근무일수 증가
                            }
                        }
                    }

                    val grossPay = totalWorkedHours * hourlyRate
                    val deductionsAmount = grossPay * (deductionsPercent / 100)
                    val netPay = grossPay - deductionsAmount

                    // 개인 근로자 화면에 표시
                    displayIndividualEmployeeDetails(
                        CalculatedSalary(
                            uid = uid,
                            userName = username,
                            date = "$startDate ~ $endDate", // 선택한 날짜 범위 표시
                            workDays = workDaysCount,  // 근무일수 추가
                            workedHours = totalWorkedHours,
                            hourlyRate = hourlyRate,
                            grossPay = grossPay,
                            deductionsPercent = deductionsPercent,
                            deductionsAmount = deductionsAmount,
                            netPay = netPay
                        )
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "급여 계산 오류: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // 개별 직원 상세정보 표시
    private fun displayIndividualEmployeeDetails(salaryData: CalculatedSalary) {
        // UI에 급여 정보 표시
        with(binding) {
            salaryAmount.text = "급여: ${salaryData.netPay.toInt()}원"
            // worktime.text 대신 workDays.text 사용
            workDays.text = "근무일수: ${salaryData.workDays}일"  // 이 부분을 layout에 추가해야 함
            totalWorkHours.text = "총 근무시간: ${salaryData.workedHours}시간"
            hourlyRate.text = "시급: ${salaryData.hourlyRate.toInt()}원"
            deductions.text = "공제율: ${salaryData.deductionsPercent}%"

            // 추가 정보 표시 (국민연금 등)
            tvrate1.text = "국민연금: ${(salaryData.grossPay * 0.045).toInt()}원"
            tvrate2.text = "건강보험: ${(salaryData.grossPay * 0.0343).toInt()}원"
            tvrate3.text = "고용보험: ${(salaryData.grossPay * 0.008).toInt()}원"
            tvrate4.text = "산재보험: ${(salaryData.grossPay * 0.0065).toInt()}원"
            tvrate5.text = "장기요양: ${(salaryData.grossPay * 0.0127).toInt()}원"
            tvrate6.text = "소득세: ${(salaryData.grossPay * 0.01).toInt()}원"
            tvrate7.text = "총 공제액: ${salaryData.deductionsAmount.toInt()}원"
        }
    }

    // 급여 입력 다이얼로그 표시
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
            dialog.findViewById<TextView>(R.id.etDeductionsWhole).rootView.findViewById<TextView>(R.id.textView),
            dialog.findViewById<TextView>(R.id.etDeductionsWhole).rootView.findViewById<TextView>(R.id.textView2),
            dialog.findViewById<TextView>(R.id.etDeductionsWhole).rootView.findViewById<TextView>(R.id.textView3),
            dialog.findViewById<TextView>(R.id.etDeductionsWhole).rootView.findViewById<TextView>(R.id.textView4),
            dialog.findViewById<TextView>(R.id.etDeductionsWhole).rootView.findViewById<TextView>(R.id.textView5),
            dialog.findViewById<TextView>(R.id.etDeductionsWhole).rootView.findViewById<TextView>(R.id.textView6),
            dialog.findViewById<TextView>(R.id.etDeductionsWhole).rootView.findViewById<TextView>(R.id.textView7)
        )

        // 초기 상태 설정 - 세부항목 숨김
        detailFields.forEach { it.visibility = View.GONE }

        cbRate.setOnCheckedChangeListener { _, isChecked ->
            val visibility = if (isChecked) View.VISIBLE else View.GONE
            detailFields.forEach { it.visibility = visibility }
        }

        // 선택된 근로자 가져오기
        val position = binding.spinnerWorker.selectedItemPosition
        if (position == -1 || position >= workerList.size) {
            Toast.makeText(context, "근로자를 먼저 선택해주세요.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            return
        }

        val selectedWorker = workerList[position]

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
            val deductions = if (cbRate.isChecked) {
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

            // 데이터베이스에 시급과 공제율 저장
            saveSalaryRateAndDeductions(
                selectedWorker.uid,
                hourlyRateValue,
                deductionsValue,
                deductions
            )

            dialog.dismiss()

            // 저장 후 데이터 새로고침
            fetchWorkerSalaryDetails(selectedWorker.uid)
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

        // 시급과 공제율은 worktime 노드의 최상위에 저장
        val hourlyRateData = mapOf(
            "hourlyRate" to hourlyRate,
            "deductionsPercent" to deductions
        )

        // 해당 날짜의 근무 시간 데이터 저장
        database.child("worktime").child(employeeUid).child(date).updateChildren(salaryData)
            .addOnSuccessListener {
                // 시급과 공제율은 worktime 최상위에 별도로 저장
                database.child("worktime").child(employeeUid).updateChildren(hourlyRateData)
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

        val salaryData = mutableMapOf<String, Any>(
            "hourlyRate" to hourlyRate,
            "deductionsPercent" to deductions
        )

        // 세부 공제율 정보가 있으면 추가
        detailedDeductions?.let {
            salaryData["nationalPension"] = it.nationalPension
            salaryData["healthInsurance"] = it.healthInsurance
            salaryData["employmentInsurance"] = it.employmentInsurance
            salaryData["industrialAccident"] = it.industrialAccident
            salaryData["longTermCare"] = it.longTermCare
            salaryData["incomeTax"] = it.incomeTax
            salaryData["other"] = it.other
        }

        // 근로자의 worktime에 시급과 공제율 데이터 저장
        database.child("worktime").child(employeeUid).updateChildren(salaryData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "시급과 공제율이 저장되었습니다.", Toast.LENGTH_SHORT).show()

                // 급여 다시 계산 및 표시 - 선택된 근로자의 급여 정보 새로고침
                fetchWorkerSalaryDetails(employeeUid)
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    requireContext(),
                    "데이터 저장에 실패했습니다: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // 급여 데이터 저장
    private fun saveSalaryData(
        employeeUid: String,
        hourlyRate: Double,
        deductions: Double,
        workHours: Double,
        finalSalary: Double
    ) {
        if (companyCode.isEmpty()) {
            Toast.makeText(context, "회사 코드를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val salaryData = mapOf(
            "hourlyRate" to hourlyRate,
            "deductionsPercent" to deductions,
            "workedHours" to workHours,
            "finalSalary" to finalSalary
        )

        // 근로자의 worktime에 급여 데이터 저장
        database.child("worktime").child(employeeUid).updateChildren(salaryData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "급여 데이터가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                // 데이터 갱신
                fetchWorkerSalaryDetails(employeeUid)
                fetchWorkerSalaryData(
                    employeeUid,
                    workerList.find { it.uid == employeeUid }?.name ?: "Unknown"
                )
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    requireContext(),
                    "급여 데이터 저장에 실패했습니다: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
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

        override fun onBindViewHolder(holder: SalaryViewHolder, position: Int) {
            val workRecord = workRecordList[position]
            holder.tvNumber.text = (position + 1).toString()
            holder.tvWorkerName.text = workRecord.name
            holder.salaryAmount.text = "급여: ${workRecord.salaryAmount.toInt()}원"
            // worktime.text를 workDays.text로 변경
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