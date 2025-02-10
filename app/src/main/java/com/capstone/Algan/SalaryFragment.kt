package com.capstone.Algan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capstone.Algan.databinding.FragmentSalaryBinding

class SalaryFragment : Fragment() {

    private var _binding: FragmentSalaryBinding? = null
    private val binding get() = _binding!!

    private lateinit var user: User
    private lateinit var employeeAdapter: EmployeeAdapter  // Adapter 선언

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        user = getUser()

        _binding = FragmentSalaryBinding.inflate(inflater, container, false)

        // 근로자 화면 설정
        if (user.role == "근로자") {
            binding.employeeSalaryInfo.visibility = View.VISIBLE
            binding.employerSalaryInfo.visibility = View.GONE

            // 급여 정보 설정 (근로자용)
            binding.salaryAmount.text = "실 지급액: 90,000원"
            binding.totalWorkHours.text = "총 근무시간: 10일"
            binding.hourlyRate.text = "시급: 10,000원"
            binding.deductions.text = "공제: 10%"

        } else if (user.role == "사업주") {
            binding.employeeSalaryInfo.visibility = View.GONE
            binding.employerSalaryInfo.visibility = View.VISIBLE

            // 사업주용 급여 관리 기능
            binding.employeeSalaryList.text = "근로자 급여 정보"

            // 사업주가 속한 회사의 근로자들만 표시
            val employeeList = getEmployees()  // 실제로 DB나 API에서 가져오도록 수정 필요
            employeeAdapter = EmployeeAdapter(user, employeeList)
            binding.recyclerView.adapter = employeeAdapter  // 어댑터 설정
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getUser(): User {
        // 로그인된 사용자 정보를 가져오도록 수정
        return User(id = "1", username = "근로자1", password = "password123", role = "근로자", phone = "011-1234-5678", email = "a2@example.com")
    }

    // 예시로 근로자 목록을 가져오는 함수 ++ DB연동으로 수정
    private fun getEmployees(): List<User> {
        return listOf(
            User(id = "2", username = "근로자1", password = "password", role = "근로자", phone = "010-1234-5678", email = "worker1@example.com"),
            User(id = "3", username = "근로자2", password = "password", role = "근로자", phone = "010-2345-6789", email = "worker2@example.com")
        )
    }
}

