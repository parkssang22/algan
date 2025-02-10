package com.capstone.Algan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capstone.Algan.databinding.FragmentSalaryBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

class SalaryFragment : Fragment() {

    private var _binding: FragmentSalaryBinding? = null
    private val binding get() = _binding!!

    private lateinit var user: Employee // 수정된 부분
    private lateinit var employeeAdapter: EmployeeAdapter  // Adapter 선언
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        database = FirebaseDatabase.getInstance().reference

        _binding = FragmentSalaryBinding.inflate(inflater, container, false)

        // 근로자 정보 가져오기
        getUserInfo()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getUserInfo() {
        // 로그인된 사용자 정보 가져오기
        val uid = "1" // 예시로 UID를 지정, 실제로는 로그인된 사용자의 UID를 사용해야 함
        database.child("employees").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    user = snapshot.getValue(Employee::class.java)!!
                    binding.salaryAmount.text = "실 지급액: ${user.salary ?: "정보 없음"}"
                    binding.totalWorkHours.text = "총 근무시간: ${user.workingHours ?: "정보 없음"}"

                    // 근로자가 속한 회사의 근로자 목록 가져오기
                    getEmployees(user.companyCode)
                } else {
                    // 데이터가 존재하지 않을 경우
                    binding.salaryAmount.text = "급여 정보 없음"
                    binding.totalWorkHours.text = "근무시간 정보 없음"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // 데이터 가져오기 실패
                binding.salaryAmount.text = "급여 정보 로드 실패"
                binding.totalWorkHours.text = "근무시간 정보 로드 실패"
            }
        })
    }


    private fun getEmployees(companyCode: String) {
        database.child("companies").child(companyCode).child("employees").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val employeeList = mutableListOf<Employee>()
                for (childSnapshot in snapshot.children) {
                    val employee = childSnapshot.getValue(Employee::class.java)
                    if (employee != null) {
                        employeeList.add(employee)
                    }
                }
                // 어댑터 설정
                employeeAdapter = EmployeeAdapter(user, employeeList)
                binding.recyclerView.adapter = employeeAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                // 근로자 목록 가져오기 실패
                // 처리 로직 추가 가능
            }
        })
    }
}
