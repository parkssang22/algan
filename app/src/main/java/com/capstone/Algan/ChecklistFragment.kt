package com.capstone.Algan

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.capstone.Algan.databinding.FragmentChecklistBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ChecklistFragment : Fragment() {
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentChecklistBinding? = null
    private val binding get() = _binding!!
    private var lastLoadedKey: String? = null


    // 상태 변수들
    private var isBusinessOwner = false
    private var loggedInUserUid: String = ""
    private var companyCode: String = ""

    // 리스트들 초기화
    private val employeeList = mutableListOf<Employee>()
    private val checklistItems = mutableListOf<CheckList>()

    // 어댑터 선언
    private lateinit var listViewAdapter: ChecklistAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        setupAddButton()

        auth.currentUser?.let { user ->
            fetchUserData(user.uid) {
                requireActivity().runOnUiThread {
                    setupListView() // 데이터가 로드된 후 실행
                }
            }
        } ?: run {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
        }



        // ListView 스크롤 활성화 및 설정
        binding.listViewItems.apply {
            isVerticalScrollBarEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }


        // ListView 스크롤 리스너 설정
        binding.listViewItems.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}

            override fun onScroll(
                view: AbsListView,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                if (firstVisibleItem + visibleItemCount >= totalItemCount) {
                    loadMoreChecklists()
                }
            }
        })
    }

    // 🔹 근로자 목록 가져오기 (사업주용)
    private fun fetchUserData(userId: String, callback: () -> Unit) {
        // 먼저 사업주인지 확인
        database.child("companies")
            .orderByChild("owner/uid")
            .equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (companySnapshot in snapshot.children) {
                            companyCode = companySnapshot.key ?: ""
                            loggedInUserUid = userId
                            isBusinessOwner = true

                            requireActivity().runOnUiThread {
                                setupBusinessOwnerUI()
                                fetchWorkerList { // 여기서 근로자 목록을 가져온 후 체크리스트를 로드
                                    loadChecklistsForAllEmployees() // 모든 근로자의 체크리스트 로드
                                }
                                callback()
                            }
                            return
                        }
                    } else {
                        // 사업주가 아니면 근로자로 확인
                        checkIfEmployee(userId, callback)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChecklistFragment", "회사 데이터 로드 실패", error.toException())
                }
            })
    }

    private fun checkIfEmployee(userId: String, callback: () -> Unit) {
        database.child("companies").orderByChild("employees/$userId/uid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (companySnapshot in snapshot.children) {
                            companyCode = companySnapshot.key ?: ""
                            loggedInUserUid = userId
                            isBusinessOwner = false

                            requireActivity().runOnUiThread {
                                setupEmployeeUI()
                                loadChecklistsForEmployee(userId)
                                callback()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "사용자를 찾을 수 없습니다.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChecklistFragment", "근로자 데이터 로드 실패", error.toException())
                }
            })
    }

    // 🔹 근로자 목록 가져오기 및 콜백 추가
    private fun fetchWorkerList(callback: () -> Unit) {
        database.child("companies").child(companyCode).child("employees")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.w("ChecklistFragment", "근로자 목록이 없습니다.")
                        Toast.makeText(requireContext(), "근로자가 없습니다.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    employeeList.clear()
                    for (employeeSnapshot in snapshot.children) {
                        val employee = employeeSnapshot.getValue(Employee::class.java)
                        if (employee != null) {
                            employeeList.add(employee)
                        }
                    }

                    if (employeeList.isNotEmpty()) {
                        requireActivity().runOnUiThread {
                            setupEmployeeSpinner() // UI 업데이트
                            callback() // ✅ 콜백 호출하여 체크리스트 로드
                        }
                    } else {
                        Log.w("ChecklistFragment", "근로자 목록이 비어 있습니다.")
                        Toast.makeText(requireContext(), "근로자가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChecklistFragment", "근로자 목록을 불러오는 데 실패했습니다.", error.toException())
                    Toast.makeText(requireContext(), "근로자 목록을 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    // 🔹 모든 근로자의 체크리스트 불러오기
    private fun loadChecklistsForAllEmployees() {
        Log.d("ChecklistFragment", "모든 근로자의 체크리스트 데이터 불러오기 시작")

        // 기존 체크리스트 초기화
        checklistItems.clear() // 모든 체크리스트를 불러오기 전에 초기화
        employeeList.forEach { employee ->
            loadChecklistsForEmployee(employee.uid) // 각 근로자의 체크리스트를 로드합니다.
        }
    }

    // 🔹 사업주 UI 설정
    private fun setupBusinessOwnerUI() {
        binding.tvselectEmployees.visibility = View.VISIBLE
        binding.spinnerEmployees.visibility = View.VISIBLE
        binding.textViewItemContent.visibility = View.VISIBLE
        binding.editTextItemContent.visibility = View.VISIBLE
        binding.buttonAddItem.visibility = View.VISIBLE
    }

    // 🔹 근로자 UI 설정
    private fun setupEmployeeUI() {
        binding.tvselectEmployees.visibility = View.GONE
        binding.spinnerEmployees.visibility = View.GONE
        binding.textViewItemContent.visibility = View.GONE
        binding.editTextItemContent.visibility = View.GONE
        binding.buttonAddItem.visibility = View.GONE

        // ChecklistAdapter 초기화
        listViewAdapter = ChecklistAdapter(this@ChecklistFragment.requireContext(), checklistItems)
        binding.listViewItems.adapter = listViewAdapter

        // 자신의 체크리스트 로드
        loadChecklistsForEmployee(loggedInUserUid) // 자신의 체크리스트 로드
    }


    private fun setupListView() {
        Log.d("ChecklistFragment", "리스트뷰 초기화 시작")

        // 리스트뷰 어댑터 초기화
        listViewAdapter = ChecklistAdapter(requireContext(), checklistItems)
        binding.listViewItems.adapter = listViewAdapter

        // ListView 스크롤 활성화
        binding.listViewItems.isVerticalScrollBarEnabled = true
        binding.listViewItems.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

        // 근로자 목록 로드
        fetchWorkerList()

        lastLoadedKey = null

        // 초기에 모든 근로자에 대한 체크리스트 로드
        loadChecklistsForAllEmployees() // 수정된 부분
    }


    private fun scrollToBottom() {
        binding.listViewItems.post {
            binding.listViewItems.setSelection(listViewAdapter.count - 1)
        }
    }


    // 🔹 근로자 체크리스트 불러오기 (여기서 adapter.notifyDataSetChanged () 호출)
    private fun loadChecklistsForEmployee(employeeUid: String) {
        Log.d("ChecklistFragment", "체크리스트 데이터 불러오기 시작: UID=$employeeUid")

        database.child("checklist")
            .child(companyCode)
            .child(employeeUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("ChecklistFragment", "체크리스트 데이터 로드 시작")

                    if (!snapshot.exists()) {
                        Log.w("ChecklistFragment", "체크리스트 데이터 없음.")
                        Toast.makeText(requireContext(), "등록된 체크리스트가 없습니다.", Toast.LENGTH_SHORT)
                            .show()
                        return
                    }

                    // 체크리스트 항목 추가
                    for (checklistSnapshot in snapshot.children) {
                        val checklistItem = checklistSnapshot.getValue(CheckList::class.java)
                        checklistItem?.let { item ->
                            if (!checklistItems.contains(item)) { // 중복 확인 후 추가
                                checklistItems.add(item)
                                Log.d(
                                    "ChecklistFragment",
                                    "불러온 체크리스트: ${item.contents}, 사용자: ${item.username}"
                                )
                            } else {
                                Log.d(
                                    "ChecklistFragment",
                                    "중복된 체크리스트 항목: ${item.contents}, 사용자: ${item.username}"
                                )
                            }
                        }
                    }

                    listViewAdapter.notifyDataSetChanged() // UI 업데이트
                    setListViewHeightBasedOnItems(binding.listViewItems)
                    scrollToBottom()
                    Log.d("ChecklistFragment", "총 체크리스트 항목 수: ${checklistItems.size}")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChecklistFragment", "체크리스트 불러오기 실패", error.toException())
                    Toast.makeText(requireContext(), "체크리스트를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun getEmployeeNameByUid(uid: String): String {
        return employeeList.find { it.uid == uid }?.username ?: uid // uid 대신 사용자 이름을 반환하도록 수정
    }


    private fun setListViewHeightBasedOnItems(listView: ListView) {
        val listAdapter = listView.adapter ?: return
        if (listAdapter.count == 0) return
        var totalHeight = 0
        for (i in 0 until listAdapter.count) {
            val listItem = listAdapter.getView(i, null, listView)
            listItem.measure(0, 0)
            totalHeight += listItem.measuredHeight
        }

        val params = listView.layoutParams
        params.height = totalHeight + (listView.dividerHeight * (listAdapter.count - 1))
        listView.layoutParams = params
        listView.requestLayout()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChecklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    // 🔹 근무자 선택 Spinner 설정
    private fun setupEmployeeSpinner() {
        if (employeeList.isEmpty()) {
            Log.w("ChecklistFragment", "근로자 목록이 비어 있음. Spinner에 추가할 데이터 없음.")
            Toast.makeText(requireContext(), "등록된 근로자가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val employeeNames = mutableListOf("전체")
        employeeNames.addAll(employeeList.map { it.username })

        Log.d("ChecklistFragment", "스피너에 추가할 근로자 목록: $employeeNames")

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            employeeNames
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerEmployees.adapter = spinnerAdapter
        binding.spinnerEmployees.visibility = View.VISIBLE
        spinnerAdapter.notifyDataSetChanged()

        // Spinner 항목 선택 리스너 추가
        binding.spinnerEmployees.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedEmployeeName = parent.getItemAtPosition(position).toString()
                    Log.d("ChecklistFragment", "선택된 근로자: $selectedEmployeeName")

                    // 선택된 근로자의 UID를 가져오기
                    val selectedEmployee = employeeList.find { it.username == selectedEmployeeName }
                    if (selectedEmployee != null) {
                        // 선택된 근로자에 대한 UID로 체크리스트 로드
                        loadChecklistsForEmployee(selectedEmployee.uid)
                    } else {
                        Log.w("ChecklistFragment", "선택된 근로자에 대한 UID를 찾을 수 없습니다.")
                    }

                    Toast.makeText(
                        requireContext(),
                        "선택된 근로자: $selectedEmployeeName",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    Log.d("ChecklistFragment", "근로자 선택 안됨")
                }
            }
    }

    private fun loadMoreChecklists() {
        Log.d("ChecklistFragment", "추가 데이터 로드 시작")

        if (lastLoadedKey == null) {
            Log.w("ChecklistFragment", "lastLoadedKey가 null입니다.")
            return
        }

        database.child("checklist")
            .child(companyCode)
            .child(loggedInUserUid)
            .orderByKey()
            .startAfter(lastLoadedKey!!)
            .limitToFirst(10)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.w("ChecklistFragment", "추가 데이터 없음.")
                        return
                    }

                    for (checklistSnapshot in snapshot.children) {
                        val checklistItem = checklistSnapshot.getValue(CheckList::class.java)
                        checklistItem?.let {
                            checklistItems.add(it)
                            Log.d("ChecklistFragment", "추가된 체크리스트: ${it.contents}")
                        }
                    }

                    lastLoadedKey = snapshot.children.lastOrNull()?.key ?: lastLoadedKey
                    listViewAdapter.notifyDataSetChanged()
                    setListViewHeightBasedOnItems(binding.listViewItems)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChecklistFragment", "추가 데이터 로드 실패", error.toException())
                    Toast.makeText(
                        requireContext(),
                        "추가 데이터를 불러오는 데 실패했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }


    private fun fetchWorkerList() {
        database.child("companies").child(companyCode).child("employees")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.w("ChecklistFragment", "근로자 목록이 없습니다.")
                        Toast.makeText(requireContext(), "근로자가 없습니다.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    employeeList.clear()
                    for (employeeSnapshot in snapshot.children) {
                        val employee = employeeSnapshot.getValue(Employee::class.java)
                        if (employee != null) {
                            employeeList.add(employee)
                        }
                    }

                    if (employeeList.isNotEmpty()) {
                        requireActivity().runOnUiThread {
                            setupEmployeeSpinner() // ✅ UI 업데이트를 안전하게 실행
                        }
                    } else {
                        Log.w("ChecklistFragment", "근로자 목록이 비어 있습니다.")
                        Toast.makeText(requireContext(), "근로자가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChecklistFragment", "근로자 목록을 불러오는 데 실패했습니다.", error.toException())
                    Toast.makeText(
                        requireContext(),
                        "근로자 목록을 불러오는 데 실패했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // 🔹 체크리스트 추가 버튼 설정(사업주만 사용 가능).
    private fun setupAddButton() {
        Log.d("ChecklistFragment", "buttonAddItem 클릭 이벤트 설정됨")

        binding.buttonAddItem.setOnClickListener {
            val content = binding.editTextItemContent.text.toString().trim()
            val selectedEmployeeName = binding.spinnerEmployees.selectedItem?.toString()

            if (selectedEmployeeName == null) {
                Toast.makeText(requireContext(), "근로자를 선택하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "내용을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedEmployeeName == "전체") {
                employeeList.forEach { employee ->
                    addChecklistItem(employee, content)
                }
                Toast.makeText(requireContext(), "모든 근로자에게 체크리스트가 전송되었습니다.", Toast.LENGTH_SHORT)
                    .show()
            } else {
                val selectedEmployee = employeeList.find { it.username == selectedEmployeeName }
                selectedEmployee?.let {
                    addChecklistItem(it, content)
                    Toast.makeText(
                        requireContext(),
                        "${it.username} 에게 체크리스트가 전송되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            binding.editTextItemContent.text.clear()
        }
    }


    // 🔹 체크리스트 추가 (checklists/{로그인UID}/{체크리스트UID} 경로에 저장)
    private fun addChecklistItem(employee: Employee, content: String) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = getCurrentTime()
        val newChecklist = CheckList(
            uid = UUID.randomUUID().toString(),
            username = employee.username,
            contents = content,
            date = currentDate,
            time = currentTime,
            status = "미완료"
        )

        // 데이터베이스 경로를 companyCode/employeeUid로 변경
        database.child("checklist")
            .child(companyCode)
            .child(employee.uid)
            .child(newChecklist.uid)
            .setValue(newChecklist)
            .addOnSuccessListener {
                Log.d("ChecklistFragment", "Checklist successfully added for ${employee.username}")
                Toast.makeText(
                    requireContext(),
                    "${employee.username}에게 체크리스트가 전송되었습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Log.e("ChecklistFragment", "Failed to add checklist", e)
                Toast.makeText(
                    requireContext(),
                    "체크리스트 전송 실패!",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getCurrentTime(): String {
        val currentTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        currentTimeFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul") // 한국 표준시로 설정
        val currentTime = currentTimeFormat.format(Date())
        Log.d("WorkRecordFragment", "Current Time: $currentTime")
        return currentTime
    }


    inner class ChecklistAdapter(
        private val context: android.content.Context,
        private val items: MutableList<CheckList>
    ) : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val item = items[position]
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_checklist, parent, false)

            // 각 뷰를 찾고 데이터를 설정합니다.
            val tvDate = view.findViewById<TextView>(R.id.tvDate)
            val tvUsername = view.findViewById<TextView>(R.id.tvUsername)
            val tvContent = view.findViewById<TextView>(R.id.tvContent)
            val buttonStatus = view.findViewById<Button>(R.id.buttonStatus)
            val buttonExpand = view.findViewById<Button>(R.id.buttonExpand)

            // 체크리스트 항목의 데이터 바인딩
            tvDate.text = item.date // 체크리스트의 날짜
            tvUsername.text = item.username // 체크리스트의 사용자 이름
            tvContent.text = item.contents // 체크리스트의 내용
            updateButtonStatus(buttonStatus, item.status) // 버튼 상태 업데이트

            // 내용이 길 경우 펼치기 기능 추가
            view.post {
                if (tvContent.lineCount > 1) {
                    tvContent.maxLines = 2
                    tvContent.ellipsize = TextUtils.TruncateAt.END
                }
            }

            buttonExpand.setOnClickListener {
                if (tvContent.maxLines == 2) {
                    tvContent.maxLines = Int.MAX_VALUE
                    buttonExpand.text = "-"
                } else {
                    tvContent.maxLines = 2
                    buttonExpand.text = "+"
                }
            }

            buttonStatus.setOnClickListener {
                val newStatus = if (item.status == "완료") "미완료" else "완료"
                item.status = newStatus
                updateButtonStatus(buttonStatus, newStatus)

                val checklistRef = database.child("checklist").child(companyCode).child(item.uid)
                val updates = mapOf<String, Any>(
                    "status" to newStatus
                )
                checklistRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Log.d("ChecklistFragment", "체크리스트 상태 업데이트 성공: ${item.uid} -> $newStatus")
                        updateBusinessOwnerChecklist(item, newStatus, companyCode) // 🔹 companyCode 넘겨줌
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChecklistFragment", "체크리스트 상태 업데이트 실패", e)
                    }
            }


            return view
        }


        private fun updateButtonStatus(button: Button, status: String) {
            val colorRes =
                if (status == "완료") android.R.color.holo_green_light else android.R.color.holo_orange_light
            button.text = status
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f
                setColor(resources.getColor(colorRes, null))
            }
            button.background = drawable
        }
    }
}
// 사업주 체크리스트 업데이트 함수
private fun updateBusinessOwnerChecklist(checklistItem: CheckList, newStatus: String, companyCode: String) {
    val database = FirebaseDatabase.getInstance().reference
    val ownerChecklistRef = database.child("checklist").child(companyCode).child(checklistItem.uid)
    val updates = mapOf<String, Any>(
        "status" to newStatus
    )

    ownerChecklistRef.updateChildren(updates)
        .addOnSuccessListener {
            Log.d("ChecklistFragment", "사업주 체크리스트 상태 업데이트 성공: ${checklistItem.uid} -> $newStatus")
        }
        .addOnFailureListener { e ->
            Log.e("ChecklistFragment", "사업주 체크리스트 상태 업데이트 실패", e)
        }
}