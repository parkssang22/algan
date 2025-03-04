package com.capstone.Algan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AllemployeeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val userList = mutableListOf<Employee>()

    // 모든 근로자 데이터 (실제 데이터는 Firebase에서 가져오도록 변경해야함)
    private val allUsers = listOf(
        Employee("user1@example.com", "user1", "password1", "근로자", "010-1234-5678", "ABC", "1"),
        Employee("user2@example.com", "user2", "password2", "근로자", "010-2345-6789", "XYZ", "2"),
        Employee("owner@example.com", "owner", "password3", "사업주", "010-3456-7890", "ABC", "3"),
        Employee("user3@example.com", "user3", "password4", "근로자", "010-4567-8901", "ABC", "4")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_allemployee, container, false)

        // RecyclerView 초기화
        recyclerView = view.findViewById(R.id.recyclerView_employees)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // 모든 근로자 데이터를 userList에 설정
        userList.addAll(allUsers.filter { it.role == "근로자" })

        // RecyclerView에 데이터 설정
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            // ViewHolder 클래스 정의
            inner class EmployeeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                val profileImageView: ImageView = itemView.findViewById(R.id.imageView_profile)
                val nameTextView: TextView = itemView.findViewById(R.id.tvUsername)
                val roleTextView: TextView = itemView.findViewById(R.id.tvRole)
            }

            // onCreateViewHolder: 뷰를 생성하여 ViewHolder로 반환
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_allemployee, parent, false)
                return EmployeeViewHolder(itemView)
            }

            // onBindViewHolder: 데이터 바인딩
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val employee = userList[position]
                val employeeViewHolder = holder as EmployeeViewHolder

                // 뷰에 데이터 설정
                employeeViewHolder.nameTextView.text = employee.username
                employeeViewHolder.roleTextView.text = employee.role
                employeeViewHolder.profileImageView.setImageResource(R.drawable.baseline_person_2_24) // 프로필 이미지
            }

            // getItemCount: 아이템 수 반환
            override fun getItemCount(): Int = userList.size
        }

        recyclerView.adapter = adapter

        return view
    }
}
