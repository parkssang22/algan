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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*
import com.capstone.Algan.R

class ChecklistFragment : Fragment() {

    private var _binding: FragmentChecklistBinding? = null
    private val binding get() = _binding!!

    // Firebase Database 참조
    private val database = FirebaseDatabase.getInstance().reference

    // 사업주 여부 확인 변수
    private val isBusinessOwner = true

    // 근로자 목록
    private val employeeList = mutableListOf<String>()
    private val employeesByGroup = mutableMapOf<String, List<String>>()

    // 그룹 리스트
    private val groupList = listOf("전체", "오전", "오후", "야간")

    // 로그인한 근로자 정보 (동적으로 설정)
    private var loggedInUser: Employee? = null

    private val checklistItems = mutableListOf<ChecklistItem>()
    private lateinit var listViewAdapter: ChecklistAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChecklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Firebase에서 근로자 목록 불러오기
        loadEmployeesFromFirebase()

        // 로그인한 근로자 정보가 있다면 Spinner 초기화
        loggedInUser?.let { employee ->
            binding.spinnerGroup.setSelection(getGroupPosition(employee.group))
            binding.spinnerEmployees.setSelection(employeeList.indexOf(employee.name))
        }

        // 사업주일 경우에만 체크리스트 추가 뷰가 보이게
        if (isBusinessOwner) {
            binding.tvgroup.visibility = View.VISIBLE
            binding.spinnerGroup.visibility = View.VISIBLE
            binding.tvselectEmployees.visibility = View.VISIBLE
            binding.spinnerEmployees.visibility = View.VISIBLE
            binding.textViewItemContent.visibility = View.VISIBLE
            binding.editTextItemContent.visibility = View.VISIBLE
            binding.buttonAddItem.visibility = View.VISIBLE
        } else {
            binding.tvgroup.visibility = View.GONE
            binding.spinnerGroup.visibility = View.GONE
            binding.tvselectEmployees.visibility = View.GONE
            binding.spinnerEmployees.visibility = View.GONE
            binding.textViewItemContent.visibility = View.GONE
            binding.editTextItemContent.visibility = View.GONE
            binding.buttonAddItem.visibility = View.GONE
        }

        // ListView 설정
        setupListView()
    }

    private fun loadEmployeesFromFirebase() {
        val companiesRef = database.child("companies").child("11348407").child("employees")

        companiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                employeeList.clear()
                employeesByGroup.clear()

                // 그룹별 근로자 목록 초기화
                for (group in groupList) {
                    employeesByGroup[group] = mutableListOf()
                }

                for (employeeSnapshot in snapshot.children) {
                    val username = employeeSnapshot.child("username").getValue(String::class.java)
                    val workingHours = employeeSnapshot.child("workingHours").getValue(String::class.java)

                    if (username != null) {
                        employeeList.add(username)

                        // 그룹별 근로자 목록에 추가 (하드코딩된 예시)
                        when (workingHours) {
                            "오전" -> (employeesByGroup["오전"] as MutableList).add(username)
                            "오후" -> (employeesByGroup["오후"] as MutableList).add(username)
                            "야간" -> (employeesByGroup["야간"] as MutableList).add(username)
                            else -> (employeesByGroup["전체"] as MutableList).add(username)
                        }
                    }
                }

                // "전체" 그룹 추가
                employeesByGroup["전체"] = employeeList.toList()

                // Spinner 설정
                setupGroupSpinner()
                setupEmployeeSpinner()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "근로자 데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupGroupSpinner() {
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, groupList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGroup.adapter = spinnerAdapter

        binding.spinnerGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedGroup = binding.spinnerGroup.selectedItem.toString()
                updateEmployeeSpinner(selectedGroup)
                filterChecklistItems()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupEmployeeSpinner() {
        if (isBusinessOwner) {
            binding.spinnerEmployees.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedEmployee = binding.spinnerEmployees.selectedItem?.toString()
                    loadChecklistsForEmployee(selectedEmployee ?: "")
                    filterChecklistItems()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } else {
            loggedInUser?.let { employee ->
                binding.spinnerEmployees.setSelection(employeeList.indexOf(employee.name))
                loadChecklistsForEmployee(employee.name)
            }
        }
    }

    private fun setupListView() {
        listViewAdapter = ChecklistAdapter(requireContext(), checklistItems)
        binding.listViewItems.adapter = listViewAdapter
    }

    private fun updateEmployeeSpinner(selectedGroup: String) {
        val employeesInGroup = employeesByGroup[selectedGroup]?.toMutableList() ?: mutableListOf()

        if (employeesInGroup.isNotEmpty() && !employeesInGroup.contains("전체")) {
            employeesInGroup.add(0, "전체")
        }

        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, employeesInGroup)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEmployees.adapter = spinnerAdapter
    }

    private fun filterChecklistItems() {
        val selectedEmployee = binding.spinnerEmployees.selectedItem?.toString()
        val selectedGroup = binding.spinnerGroup.selectedItem.toString()

        val employeesInGroup = employeesByGroup[selectedGroup] ?: listOf()
        Log.d(selectedGroup,"1")

        checklistItems.forEach { item ->
            item.isVisible = when {
                selectedEmployee == "전체" && item.employeeName in employeesInGroup -> true
                selectedEmployee == item.employeeName -> true
                selectedEmployee == "전체" -> true
                loggedInUser?.let { it.name == item.employeeName && it.group == selectedGroup } == true -> true
                else -> false
            }
        }

        listViewAdapter.notifyDataSetChanged()
    }

    private fun addChecklistItem() {
        // 선택된 근로자와 입력한 내용 가져오기
        val selectedEmployee = binding.spinnerEmployees.selectedItem?.toString()
        val itemContent = binding.editTextItemContent.text.toString()

        // 선택된 근로자와 입력한 내용이 유효한지 확인
        if (selectedEmployee.isNullOrEmpty() || itemContent.isEmpty()) {
            Toast.makeText(requireContext(), "근로자를 선택하고 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 체크리스트 항목 생성
        val checklistItem = ChecklistItem(
            date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
            employeeName = selectedEmployee,
            content = itemContent,
            isCompleted = false
        )

        // Firebase에 체크리스트 항목 저장
        val checklistRef = database.child("companies").child("11348407").child("checklists").child(selectedEmployee)
        checklistRef.push().setValue(checklistItem).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                checklistItems.add(checklistItem)
                listViewAdapter.notifyDataSetChanged()
                binding.editTextItemContent.text.clear()
            } else {
                Toast.makeText(requireContext(), "체크리스트 등록에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadChecklistsForEmployee(employeeName: String) {
        val checklistRef = database.child("companies").child("11348407").child("checklists").child(employeeName)

        checklistRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                checklistItems.clear()

                for (checklistSnapshot in snapshot.children) {
                    val item = checklistSnapshot.getValue(ChecklistItem::class.java)
                    if (item != null) {
                        checklistItems.add(item)
                    }
                }

                listViewAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "체크리스트를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        binding.spinnerGroup.setSelection(0)
        binding.spinnerEmployees.setSelection(0)
        filterChecklistItems()
        // 체크리스트 갱신
        listViewAdapter.notifyDataSetChanged()
    }

    // 로그인한 근로자 정보 설정
    fun setLoggedInUser(name: String, group: String) {
        loggedInUser = Employee(name, group)
    }

    // 그룹 이름에 해당하는 Spinner 위치 반환
    private fun getGroupPosition(group: String): Int {
        return groupList.indexOf(group)
    }

    // 근로자 정보 클래스
    data class Employee(val name: String, val group: String)

    data class ChecklistItem(
        val date: String,
        val employeeName: String,
        val content: String,
        var isCompleted: Boolean,
        var isVisible: Boolean = true
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class ChecklistAdapter(
        private val context: android.content.Context,
        private val items: MutableList<ChecklistItem>
    ) : BaseAdapter() {

        override fun getCount(): Int = items.count { it.isVisible }

        override fun getItem(position: Int): Any {
            val visibleItems = items.filter { it.isVisible }
            return visibleItems[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val visibleItems = items.filter { it.isVisible }
            val item = visibleItems[position]

            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_checklist, parent, false)
            val tvDate = view.findViewById<TextView>(R.id.tvDate)
            val tvEmployeeName = view.findViewById<TextView>(R.id.tvEmployeeName)
            val tvContent = view.findViewById<TextView>(R.id.tvContent)
            val buttonStatus = view.findViewById<Button>(R.id.buttonStatus)
            val buttonExpand = view.findViewById<Button>(R.id.buttonExpand)

            tvDate.text = item.date
            tvEmployeeName.text = item.employeeName
            tvContent.text = item.content

            view.post {
                if (tvContent.lineCount > 1) {
                    tvContent.maxLines = 2
                    tvContent.ellipsize = TextUtils.TruncateAt.END
                }
            }

            buttonExpand.setOnClickListener {
                if (tvContent.maxLines == 2) {
                    tvContent.text = item.content + "\n"
                    tvContent.maxLines = Int.MAX_VALUE
                    buttonExpand.text = "-"
                } else {
                    tvContent.text = item.content
                    tvContent.maxLines = 2
                    buttonExpand.text = "+"
                }
            }

            updateButtonStatus(buttonStatus, item.isCompleted)

            buttonStatus.setOnClickListener {
                item.isCompleted = !item.isCompleted
                updateButtonStatus(buttonStatus, item.isCompleted)
            }

            return view
        }
    }

    // 완료 => 미완료 버튼 바꾸는 함수
    private fun updateButtonStatus(button: Button, isCompleted: Boolean) {
        if (!isAdded || context == null) return // 프래그먼트가 유효하지 않으면 리턴

        val color = if (isCompleted) android.R.color.holo_green_light else android.R.color.holo_orange_light
        val text = if (isCompleted) "완료" else "미완료"

        button.text = text

        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setColor(resources.getColor(color, null))
        }

        button.background = drawable
    }
}

