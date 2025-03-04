package com.capstone.Algan

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.Algan.databinding.FragmentWorkrecordBinding
import java.text.SimpleDateFormat
import java.util.*

class WorkRecordFragment : Fragment(R.layout.fragment_workrecord) {

    private lateinit var btnClockInOut: Button
    private lateinit var tvDate: TextView
    private lateinit var tvClockIn: TextView
    private lateinit var tvClockOut: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var spinnerWorker: Spinner // 근로자 선택 스피너
    private lateinit var workerSelectionLayout: LinearLayout // 근로자 선택 레이아웃
    private lateinit var lntoprecord: LinearLayout // 출퇴근 버튼 레이아웃

    private var isClockedIn = false
    private var clockInTime: String? = null
    private var clockOutTime: String? = null

    private lateinit var recordAdapter: RecordAdapter
    private val records = mutableListOf<AttendanceRecordWithTime>()

    // 로그인된 근로자 이름
    private var loggedInWorkerName: String = "근로자1"

    private var isEmployer = false // 근로자 화면 테스트
    //private var isEmployer = true // 사업주 화면 테스트

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_workrecord, container, false)

        // 뷰 초기화
        btnClockInOut = view.findViewById(R.id.btnClockInOut)
        tvDate = view.findViewById(R.id.tvDate)
        tvClockIn = view.findViewById(R.id.tvClockIn)
        tvClockOut = view.findViewById(R.id.tvClockOut)
        tvStartDate = view.findViewById(R.id.tvStartDate)
        tvEndDate = view.findViewById(R.id.tvEndDate)
        recyclerView = view.findViewById(R.id.recyclerViewRecords)
        spinnerWorker = view.findViewById(R.id.spinnerWorker)
        lntoprecord = view.findViewById(R.id.lntoprecord)
        workerSelectionLayout = view.findViewById(R.id.workerSelectionLayout)

        // RecyclerView 설정
        recordAdapter = RecordAdapter(records)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = recordAdapter

        // 현재 날짜 표시
        val currentDate = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date())
        tvDate.text = currentDate

        // 하드코딩된 기록 추가
        records.add(AttendanceRecordWithTime(1, "2025년 02월 23일", "09:00:00", "18:00:00", "09:00", 1, "근로자1"))
        records.add(AttendanceRecordWithTime(2, "2025년 02월 23일", "08:30:00", "17:30:00", "09:00", 2, "근로자2"))
        records.add(AttendanceRecordWithTime(3, "2025년 02월 22일", "09:00:00", "18:00:00", "09:00", 3, "근로자3"))

        val workerList = listOf("전체", "근로자1", "근로자2", "근로자3") // 데이터 예시
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, workerList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWorker.adapter = adapter

        // 출근/퇴근 버튼 클릭 이벤트 설정
        btnClockInOut.setOnClickListener {
            val selectedDate = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date())
            if (isClockedIn) {
                clockOutTime = getCurrentTime()
                tvClockOut.text = "퇴근 시간: $clockOutTime"
                saveAttendance(selectedDate, clockInTime ?: "미등록", clockOutTime ?: "미등록")
                btnClockInOut.text = "출근"
                btnClockInOut.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_light)
                isClockedIn = false
            } else {
                clockInTime = getCurrentTime()
                tvClockIn.text = "출근 시간: $clockInTime"
                clockOutTime = null
                tvClockOut.text = "퇴근 시간: 미등록"
                btnClockInOut.text = "퇴근"
                btnClockInOut.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_light)
                isClockedIn = true
            }
        }

        // 날짜 선택을 위한 DatePickerDialog 설정
        tvStartDate.setOnClickListener {
            showDatePicker { date ->
                tvStartDate.text = date
                filterRecordsByDate()
            }
        }

        tvEndDate.setOnClickListener {
            showDatePicker { date ->
                tvEndDate.text = date
                filterRecordsByDate()
            }
        }

        // 사업주일 때만 근로자 선택 스피너 보이기
        if (isEmployer) {
            spinnerWorker.visibility = View.VISIBLE
            lntoprecord.visibility = View.GONE // 출퇴근 버튼 안보이게
            workerSelectionLayout.visibility = View.VISIBLE
        } else {
            spinnerWorker.visibility = View.GONE
            workerSelectionLayout.visibility = View.GONE
            lntoprecord.visibility = View.VISIBLE
        }

        // 근로자 선택 변경 시 필터링
        spinnerWorker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                filterRecordsByWorker(position)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }

        return view
    }

    // 사업주일 때 근로자 선택에 맞는 출퇴근 기록 필터링
    private fun filterRecordsByWorker(workerIndex: Int) {
        val filteredRecords = if (isEmployer) {
            // 사업주라면 "전체"를 포함한 모든 근로자 기록을 보여주기
            records.filter { it.workerIndex == workerIndex || workerIndex == 0 }
        } else {
            // 근로자라면 본인만 보이도록 필터링
            records.filter { it.workerName == loggedInWorkerName }
        }

        recordAdapter.updateRecords(filteredRecords)
    }

    // 날짜 범위에 맞춰 기록 필터링
    private fun filterRecordsByDate() {
        val startDate = tvStartDate.text.toString()
        val endDate = tvEndDate.text.toString()

        val filteredRecords = records.filter {
            val recordDate = it.date
            (startDate.isEmpty() || recordDate >= startDate) && (endDate.isEmpty() || recordDate <= endDate)
        }

        recordAdapter.updateRecords(filteredRecords)
    }

    // 출퇴근 기록 저장
    private fun saveAttendance(date: String, clockIn: String, clockOut: String) {
        val workerName = if (isEmployer) {
            // 사업주일 때는 스피너에서 선택된 근로자의 이름을 사용
            spinnerWorker.selectedItem.toString()
        } else {
            // 근로자일 때는 로그인된 사용자의 이름을 사용
            loggedInWorkerName // 로그인된 근로자의 이름 사용
        }

        val workedHours = calculateWorkedHours(clockIn, clockOut)
        val newRecord = AttendanceRecordWithTime(
            id = records.size + 1,
            date = date,
            clockIn = clockIn,
            clockOut = clockOut,
            workedHours = workedHours,
            workerIndex = null, // 필요 없다면 null
            workerName = workerName // 선택된 근로자 이름 또는 로그인된 사용자 이름
        )

        records.add(newRecord)
        recordAdapter.notifyItemInserted(records.size - 1)
    }

    // 현재 시간 얻기
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
        return sdf.format(Date())
    }

    // 날짜 선택을 위한 DatePickerDialog 표시
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%04d년 %02d월 %02d일", selectedYear, selectedMonth + 1, selectedDay)
            onDateSelected(formattedDate)
        }, year, month, day)

        datePickerDialog.show()
    }

    // 근무 시간을 계산하는 함수
    private fun calculateWorkedHours(clockIn: String, clockOut: String): String {
        val format = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
        val inTime = format.parse(clockIn)
        val outTime = format.parse(clockOut)
        val diffInMillis = outTime.time - inTime.time
        val hours = (diffInMillis / (1000 * 60 * 60)).toInt()
        val minutes = ((diffInMillis / (1000 * 60)) % 60).toInt()
        return String.format("%02d:%02d", hours, minutes)
    }

    // 데이터 클래스 정의
    data class AttendanceRecordWithTime(
        val id: Int,
        val date: String,
        val clockIn: String, // 출근 시간
        val clockOut: String, // 퇴근 시간
        val workedHours: String, // 근무 시간
        val workerIndex: Int? = null, // 근로자 구분 (사업주용)
        val workerName: String? // 근로자 이름 (사업주, 근로자 모두 사용)
    )

    // 어댑터 정의
    class RecordAdapter(private val records: MutableList<AttendanceRecordWithTime>) :
        RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_work_record, parent, false)
            return RecordViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
            val record = records[position]
            holder.tvNumber.text = (position + 1).toString() // 번호 1부터 시작
            holder.tvDate.text = record.date
            holder.tvClockIn.text = formatTimeForTable(record.clockIn)
            holder.tvClockOut.text = formatTimeForTable(record.clockOut)
            holder.tvWorkedHours.text = record.workedHours
            holder.tvWorkerName.text = record.workerName // 이름 추가
        }

        override fun getItemCount(): Int {
            return records.size
        }

        // RecyclerView의 기록 갱신
        fun updateRecords(newRecords: List<AttendanceRecordWithTime>) {
            records.clear()
            records.addAll(newRecords)
            notifyDataSetChanged()
        }

        inner class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvNumber: TextView = itemView.findViewById(R.id.tvNumber)
            val tvDate: TextView = itemView.findViewById(R.id.tvDate)
            val tvClockIn: TextView = itemView.findViewById(R.id.tvClockIn)
            val tvClockOut: TextView = itemView.findViewById(R.id.tvClockOut)
            val tvWorkedHours: TextView = itemView.findViewById(R.id.tvWorkedHours)
            val tvWorkerName: TextView = itemView.findViewById(R.id.tvWorkerName) // 이름 표시
        }

        // 표에 시간 형식 변경: 시:분만 표시
        private fun formatTimeForTable(time: String): String {
            val timeParts = time.split(":")
            return if (timeParts.size >= 2) {
                "${timeParts[0]}:${timeParts[1]}"
            } else {
                time
            }
        }
    }
}
