package com.capstone.Algan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView

class EmployeeAdapter(private val user: User, private val employeeList: List<User>) :
    RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {

    private val filteredEmployeeList = if (user.role == "사업주" && user.companyName != null) {
        employeeList.filter { it.companyName == user.companyName && it.role == "고용인" }
    } else {
        employeeList
    }

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
        val employee = filteredEmployeeList[position]
        holder.nameTextView.text = "이름: ${employee.username}"
        holder.salaryTextView.text = "급여: 예시 급여"
        holder.workingHoursTextView.text = "근무시간: 예시 근무시간"
    }

    // getItemCount: 아이템의 총 개수를 반환하는 메서드
    override fun getItemCount(): Int = filteredEmployeeList.size
}
