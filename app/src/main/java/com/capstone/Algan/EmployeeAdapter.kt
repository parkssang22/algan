package com.capstone.Algan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView

class EmployeeAdapter(private val user: Employee, private val employeeList: List<Employee>) :
    RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {

    // ViewHolder: RecyclerView 항목의 뷰를 재사용하는 객체
    class EmployeeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: MaterialTextView = itemView.findViewById(R.id.employeeName)
        val salaryTextView: MaterialTextView = itemView.findViewById(R.id.employeeSalary)
        val workingHoursTextView: MaterialTextView = itemView.findViewById(R.id.employeeWorkingHours)
    }

    // onCreateViewHolder: ViewHolder를 생성하는 메서드
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_employee, parent, false)
        return EmployeeViewHolder(itemView)
    }

    // onBindViewHolder: ViewHolder에 데이터를 설정하는 메서드
    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        val employee = employeeList[position]
        holder.nameTextView.text = "이름: ${employee.username}"
        holder.salaryTextView.text = "급여: ${employee.salary ?: "정보 없음"}"
        holder.workingHoursTextView.text = "근무시간: ${employee.workingHours ?: "정보 없음"}"
    }

    // getItemCount: 아이템의 총 개수를 반환하는 메서드
    override fun getItemCount(): Int = employeeList.size
}
