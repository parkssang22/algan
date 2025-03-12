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
        val uid = "1" // 실제 로그인된 사용자의 UID 사용
        database.child("employees").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    user = snapshot.getValue(Employee::class.java)!!

                    // 사용자 정보가 있으면 근무 시간 정보 가져오기
                    getWorkTime(uid)

                    // 회사의 직원 목록 가져오기
                    getEmployees(user.companyCode)
                } else {
                    binding.salaryAmount.text = "급여 정보 없음"
                    binding.totalWorkHours.text = "근무시간 정보 없음"
                }
            }

            override fun onCancelled(error: DatabaseError) {
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

    private fun getWorkTime(uid: String) {
        database.child("worktimes").orderByChild("uid").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalPay = 0.0
                    var totalWorkHours = 0.0

                    for (childSnapshot in snapshot.children) {
                        val workTime = childSnapshot.getValue(WorkTime::class.java)
                        if (workTime != null) {
                            // recordTime (총 근무시간)이 "5.5" 같은 문자열 형태라면 변환 필요
                            val workHours = workTime.workedHours.toDoubleOrNull() ?: 0.0
                            val hourlyRate = workTime.hourlyRate.toDoubleOrNull() ?: 0.0

                            totalWorkHours += workHours
                            totalPay += workHours * hourlyRate
                        }
                    }

                    // UI 업데이트
                    binding.totalWorkHours.text = "총 근무시간: $totalWorkHours 시간"
                    binding.salaryAmount.text = "실 지급액: ${totalPay.toInt()} 원"
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.salaryAmount.text = "급여 정보 로드 실패"
                    binding.totalWorkHours.text = "근무시간 정보 로드 실패"
                }
            })
    }

}
