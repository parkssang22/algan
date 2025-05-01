package com.capstone.Algan

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log // 로그를 위해 추가
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.Algan.utils.BeaconState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import org.altbeacon.beacon.* // 비콘
import com.capstone.Algan.utils.pdfWordRecord

class WorkRecordFragment : Fragment() {
    private lateinit var beaconManager: BeaconManager// 비콘
    // UI 요소 선언
    private lateinit var btnClockInOut: Button
    private lateinit var tvClockIn: TextView
    private lateinit var tvClockOut: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var spinnerWorker: Spinner
    private lateinit var workerSelectionLayout: LinearLayout
    private lateinit var lntoprecord: LinearLayout
    private lateinit var tvDate: TextView // 오늘 날짜 표시를 위한 텍스트뷰

    // Firebase 관련 변수
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // 출퇴근 관련 상태 변수
    private var isClockedIn = false
    private var clockInTime: String? = null
    private var clockOutTime: String? = null
    private var uid: String? = null
    private var username: String? = null
    private var hourlyRate: String? = null
    private var companyCode: String? = null
    private var companyName: String? = null
    private var isEmployer: Boolean = false
    private var worktype:String?=null

    // RecyclerView 관련 변수
    private lateinit var recordAdapter: RecordAdapter
    private val records = mutableListOf<WorkTime>()

    // 근로자 리스트 (사업주용)
    private val workerList = mutableListOf<Employee>() // Employee 데이터 클래스 사용
    private lateinit var selectedWorkerUid: String

    // 현재 로그인한 사용자 데이터 클래스
    private var currentUserData: Any? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        val view = inflater.inflate(R.layout.fragment_workrecord, container, false)

        initView(view)
        setupRecyclerView()
        fetchUserData() // 사용자 데이터 가져오기
        // 비콘 연결 상태 관찰
        BeaconState.isConnected.observe(viewLifecycleOwner, androidx.lifecycle.Observer { isConnected ->
            if (isConnected) {
                handleClockInOut()
                worktype ="비콘"
                Toast.makeText(requireContext(), "비콘 연결됨.", Toast.LENGTH_SHORT)
                    .show()
            } else {
                // 비콘 연결 해제됨
                Toast.makeText(requireContext(), "비콘 연결 안됨.", Toast.LENGTH_SHORT)
                    .show()
            }
        })
        val btnPdf = view.findViewById<Button>(R.id.btnPdf)  // 뷰에서 직접 버튼 찾아오기

        btnPdf.setOnClickListener {
            fetchRecords { workTimeList ->
                if (workTimeList.isNotEmpty()) {
                    val pdfPath = pdfWordRecord(requireContext(), workTimeList)
                    Toast.makeText(requireContext(), "PDF 저장 완료: $pdfPath", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "출퇴근 기록이 없습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnClockInOut.setOnClickListener {

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // 매번 클릭할 때마다 최신 시간 확보
            val latestClockInOrOutTime = getCurrentTime()

            handleClockInOut() // handleClockInOut 메서드 수정 필요
        }
        return view
    }


    // UI 요소 초기화
    private fun initView(view: View) {
        btnClockInOut = view.findViewById(R.id.btnClockInOut)
        tvClockIn = view.findViewById(R.id.tvClockIn)
        tvClockOut = view.findViewById(R.id.tvClockOut)
        tvStartDate = view.findViewById(R.id.tvStartDate)
        tvEndDate = view.findViewById(R.id.tvEndDate)
        recyclerView = view.findViewById(R.id.recyclerViewRecords)
        spinnerWorker = view.findViewById(R.id.spinnerWorker)
        lntoprecord = view.findViewById(R.id.lntoprecord)
        workerSelectionLayout = view.findViewById(R.id.workerSelectionLayout)
        tvDate = view.findViewById(R.id.tvDate) // 오늘 날짜 텍스트뷰 초기화
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
    }

    // 사용자 데이터 가져오기
    private fun fetchUserData() {
        val currentUser = auth.currentUser ?: return // 로그인한 유저 확인
        uid = currentUser.uid // UID 가져오기

        // companies 노드에서 현재 유저의 UID를 기반으로 사업주 또는 근로자 정보 검색
        database.child("companies").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var userFound = false
                for (companySnapshot in snapshot.children) {
                    // 사업주 확인
                    val ownerSnapshot = companySnapshot.child("owner")
                    val owner = ownerSnapshot.getValue(BusinessOwner::class.java)
                    if (owner != null && owner.uid == uid) {
                        // 현재 로그인한 사용자가 사업주인 경우
                        username = owner.username
                        companyCode = owner.companyCode
                        companyName = owner.companyName
                        isEmployer = true
                        userFound = true
                        setupUIForRole()
                        fetchWorkerList() // 사업주인 경우 근로자 리스트 가져오기
                        break
                    }

                    // 근로자 확인
                    val employeesSnapshot = companySnapshot.child("employees")
                    for (employeeSnapshot in employeesSnapshot.children) {
                        val employee = employeeSnapshot.getValue(Employee::class.java)
                        if (employee != null && employee.uid == uid) {
                            // 현재 로그인한 사용자가 근로자인 경우
                            username = employee.username
                            companyCode = employee.companyCode
                            companyName = employee.companyName
                            isEmployer = false
                            userFound = true
                            setupUIForRole()
                            // 근로자 본인의 기록을 가져오도록 fetchRecords 호출
                            fetchRecords { workTimeList ->
                                // 콜백에서 반환된 workTimeList를 처리
                                if (workTimeList.isNotEmpty()) {
                                    // 작업이 성공적으로 완료된 경우
                                    // 예: 리스트 표시 등
                                } else {
                                    // 기록이 없을 경우 처리
                                }
                            }
                            break
                        }
                    }

                    if (userFound) break
                }

                if (!userFound) {
                    Toast.makeText(requireContext(), "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "데이터베이스 오류: ${error.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    // 근로자 리스트 가져오기 (사업주인 경우)
    private fun fetchWorkerList() {
        if (companyCode == null) return

        database.child("companies").child(companyCode!!)
            .child("employees") // companies/{companyCode}/employees 경로 접근
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    workerList.clear()
                    for (workerSnapshot in snapshot.children) {
                        val workerRole = workerSnapshot.child("role").getValue(String::class.java)
                        if (workerRole == "근로자") { // 역할이 '근로자'인 경우만 추가합니다.
                            val worker = workerSnapshot.getValue(Employee::class.java)
                            if (worker != null) {
                                workerList.add(worker) // 리스트에 추가합니다.
                            }
                        }
                    }
                    setupWorkerSpinner() // 스피너 설정 호출.
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "근로자 목록을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    // 근로자 선택 스피너 설정
    private fun setupWorkerSpinner() {
        val workerNames = workerList.map { it.username }
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, workerNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWorker.adapter = adapter

        spinnerWorker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                selectedWorkerUid = workerList[position].uid
                // 근로자 본인의 기록을 가져오도록 fetchRecords 호출
                fetchRecords { workTimeList ->
                    // 콜백에서 반환된 workTimeList를 처리
                    if (workTimeList.isNotEmpty()) {
                        // 작업이 성공적으로 완료된 경우
                        // 예: 리스트 표시 등
                    } else {
                        // 기록이 없을 경우 처리
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // 기본 선택 설정
        if (workerList.isNotEmpty()) {
            selectedWorkerUid = workerList[0].uid
            // 근로자 본인의 기록을 가져오도록 fetchRecords 호출
            fetchRecords { workTimeList ->
                // 콜백에서 반환된 workTimeList를 처리
                if (workTimeList.isNotEmpty()) {
                    // 작업이 성공적으로 완료된 경우
                    // 예: 리스트 표시 등
                } else {
                    // 기록이 없을 경우 처리
                }
            }
        }
    }
    // 역할에 따른 UI 변경
    private fun setupUIForRole() {
        if (isEmployer) {
            // 사업주인 경우
            spinnerWorker.visibility = View.VISIBLE
            lntoprecord.visibility = View.GONE
            workerSelectionLayout.visibility = View.VISIBLE
            btnClockInOut.visibility = View.GONE
            tvClockIn.visibility = View.GONE
            tvClockOut.visibility = View.GONE
        } else {
            // 근로자인 경우
            spinnerWorker.visibility = View.GONE
            workerSelectionLayout.visibility = View.GONE
            lntoprecord.visibility = View.VISIBLE
            btnClockInOut.visibility = View.VISIBLE
            tvClockIn.visibility = View.VISIBLE
            tvClockOut.visibility = View.VISIBLE
        }

        setupDatePickers()
    }

    // 날짜 선택기 설정
    private fun setupDatePickers() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        tvStartDate.text = currentDate
        tvEndDate.text = currentDate
        tvDate.text = "오늘 날짜: $currentDate" // 오늘 날짜 표시

        tvStartDate.setOnClickListener {
            showDatePicker { selectedDate ->
                tvStartDate.text = selectedDate
                // 근로자 본인의 기록을 가져오도록 fetchRecords 호출
                fetchRecords { workTimeList ->
                    // 콜백에서 반환된 workTimeList를 처리
                    if (workTimeList.isNotEmpty()) {
                        // 작업이 성공적으로 완료된 경우
                        // 예: 리스트 표시 등
                    } else {
                        // 기록이 없을 경우 처리
                    }
                }
            }
        }

        tvEndDate.setOnClickListener {
            showDatePicker { selectedDate ->
                tvEndDate.text = selectedDate
                // 근로자 본인의 기록을 가져오도록 fetchRecords 호출
                fetchRecords { workTimeList ->
                    // 콜백에서 반환된 workTimeList를 처리
                    if (workTimeList.isNotEmpty()) {
                        // 작업이 성공적으로 완료된 경우
                        // 예: 리스트 표시 등
                    } else {
                        // 기록이 없을 경우 처리
                    }
                }
            }
        }
    }
    // 날짜 선택 다이얼로그 함수 추가
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate =
                    String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                onDateSelected(selectedDate)
            }, year, month, day)

        datePickerDialog.show()
    }

    // RecyclerView 설정
    private fun setupRecyclerView() {
        recordAdapter = RecordAdapter(records)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = recordAdapter
    }

    // 출퇴근 버튼 처리 (근로자인 경우)
    private fun handleClockInOut() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = getCurrentTime()

        if (isClockedIn) {
            clockOutTime = currentTime
            tvClockOut.text = "퇴근 시간: $clockOutTime"
            // saveWorkTime 호출 시, clockIn이 null일 가능성을 고려해 안전하게 처리합니다.
            if (clockInTime != null) {
                saveWorkTime(currentDate, clockInTime!!, clockOutTime!!)
            } else {
                Toast.makeText(requireContext(), "출근 시간이 등록되지 않았습니다.", Toast.LENGTH_SHORT).show()
                return // 출근 시간이 없으면 저장하지 않음
            }
            btnClockInOut.text = "출근"
            btnClockInOut.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_light)
            isClockedIn = false
        } else {
            clockInTime = currentTime
            tvClockIn.text = "출근 시간: $clockInTime"
            clockOutTime = null
            tvClockOut.text = "퇴근 시간: 미등록"
            btnClockInOut.text = "퇴근"
            btnClockInOut.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_light)
            isClockedIn = true
        }

    }

    // 출퇴근 기록 저장
    private fun saveWorkTime(date: String, openTime: String, closeTime: String) {
        val workTime = WorkTime(
            id = null, // Firebase에서 자동 생성되는 키를 사용하므로 null로 설정
            uid = uid ?: "",
            date = date,
            clockIn = openTime,
            clockOut = closeTime,
            workedHours = calculateWorkedHours(openTime, closeTime),
            //hourlyRate = hourlyRate ?: "10000",
            userName = username ?: "알 수 없는 사용자",
            worktype=worktype?:"수동"
        )

        // 데이터를 회사 코드 아래에 저장
        val workTimeRef = database.child("worktimes").child(companyCode ?: "unknown_company")
        workTimeRef.push().setValue(workTime)
            .addOnSuccessListener {
                // 저장 후 기록을 다시 가져와 RecyclerView를 갱신합니다.
                // 근로자 본인의 기록을 가져오도록 fetchRecords 호출
                fetchRecords { workTimeList ->
                    // 콜백에서 반환된 workTimeList를 처리
                    if (workTimeList.isNotEmpty()) {
                        // 작업이 성공적으로 완료된 경우
                        // 예: 리스트 표시 등
                    } else {
                        // 기록이 없을 경우 처리
                    }
                }
                Toast.makeText(requireContext(), "출퇴근 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchRecords(onResult: (List<WorkTime>) -> Unit) {
        records.clear()
        val startDateStr = tvStartDate.text.toString()
        val endDateStr = tvEndDate.text.toString()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = dateFormat.parse(startDateStr)
        val endDate = dateFormat.parse(endDateStr)

        val queryUid = if (isEmployer) selectedWorkerUid else uid
        val workTimeRef = database.child("worktimes").child(companyCode ?: "unknown_company")

        workTimeRef.orderByChild("uid").equalTo(queryUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (recordSnapshot in snapshot.children) {
                        val workRecord = recordSnapshot.getValue(WorkTime::class.java)
                        if (workRecord != null) {
                            val recordDate = dateFormat.parse(workRecord.date)
                            if (recordDate != null && startDate != null && endDate != null) {
                                if (!recordDate.before(startDate) && !recordDate.after(endDate)) {
                                    records.add(workRecord)
                                }
                            }
                        }
                    }
                    records.sortByDescending { it.date }
                    recordAdapter.notifyDataSetChanged()

                    if (records.isEmpty()) {
                        Toast.makeText(requireContext(), "해당 기간의 기록이 없습니다.", Toast.LENGTH_SHORT).show()
                    }

                    // ✅ 콜백 호출
                    onResult(records)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "기록을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    onResult(emptyList())  // 에러 발생 시도 콜백
                }
            })
    }

    // 현재 시간 가져오기
    private fun getCurrentTime(): String {
        val currentTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        currentTimeFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul") // 한국 표준시로 설정
        val currentTime = currentTimeFormat.format(Date())
        Log.d("WorkRecordFragment", "Current Time: $currentTime")
        return currentTime
    }
    // 근무 시간 계산
    private fun calculateWorkedHours(clockIn: String, clockOut: String): String {
        return try {
            val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val dateIn = format.parse(clockIn)
            val dateOut = format.parse(clockOut)
            val diff = dateOut.time - dateIn.time
            val hours = diff / (1000 * 60 * 60)
            val minutes = (diff / (1000 * 60)) % 60
            val seconds = (diff / 1000) % 60
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } catch (e: Exception) {
            "00:00"
        }
    }
}