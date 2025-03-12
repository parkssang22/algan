package com.capstone.Algan

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import android.widget.ListView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AllemployeeFragment : Fragment() {

    private lateinit var listView: ListView
    private val userList = mutableListOf<Employee>()
    private var userCompanyCode: String? = null // 회사 코드

    // Firebase 관련 변수
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_allemployee, container, false)

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // ListView 초기화
        listView = view.findViewById(R.id.all_list)

        loadUserData() // 회사 코드 가져오기

        return view
    }

    private fun loadUserData() {
        val sharedPreferences =
            requireActivity().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null)

        if (userId == null) {
            Toast.makeText(context, "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val companiesRef = database.getReference("companies")
        companiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var foundUserCompanyCode: String? = null

                for (companySnapshot in snapshot.children) {
                    val ownerSnapshot = companySnapshot.child("owner")
                    if (ownerSnapshot.child("uid").getValue(String::class.java) == userId) {
                        foundUserCompanyCode =
                            ownerSnapshot.child("companyCode").getValue(String::class.java)
                    }

                    // 모든 회사 데이터를 순회한 후 회사 코드를 설정
                    if (foundUserCompanyCode != null) {
                        userCompanyCode = foundUserCompanyCode
                        loadAllUsers() // 모든 사용자 가져오기
                    }


                    val employeesSnapshot = companySnapshot.child("employees")
                    for (employeeSnapshot in employeesSnapshot.children) {
                        if (employeeSnapshot.child("uid").getValue(String::class.java) == userId) {
                            foundUserCompanyCode =
                                employeeSnapshot.child("companyCode").getValue(String::class.java)
                            break
                        }
                    }

                    if (foundUserCompanyCode != null) {
                        userCompanyCode = foundUserCompanyCode
                        loadAllUsers() // 모든 사용자 가져오기 (사업주 + 근로자)
                        return
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAllUsers() {
        if (userCompanyCode != null) {
            val companiesRef = database.getReference("companies")
            companiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userList.clear()

                    // 회사 내 모든 구성원(사업주 + 근로자)을 가져옵니다.
                    for (companySnapshot in snapshot.children) {
                        // 회사 코드 추출 (owner와 employees에 모두 존재)
                        val ownerSnapshot = companySnapshot.child("owner")
                        val ownerCompanyCode =
                            ownerSnapshot.child("companyCode").getValue(String::class.java)

                        // 회사 코드가 로그인한 사용자의 회사 코드와 일치하는 경우 처리
                        if (ownerCompanyCode == userCompanyCode) {
                            // 사업주 정보 추가
                            val ownerUsername =
                                ownerSnapshot.child("username").getValue(String::class.java)
                                    ?: "알 수 없음"
                            val ownerRole =
                                ownerSnapshot.child("role").getValue(String::class.java) ?: "알 수 없음"
                            val ownerEmail =
                                ownerSnapshot.child("email").getValue(String::class.java)
                                    ?: "알 수 없음"
                            val ownerPhone =
                                ownerSnapshot.child("phone").getValue(String::class.java)
                                    ?: "알 수 없음"
                            val ownerCompanyCode =
                                ownerSnapshot.child("companyCode").getValue(String::class.java)
                                    ?: ""

                            val owner = Employee(
                                ownerEmail,
                                ownerUsername,
                                ownerRole,
                                ownerPhone,
                                ownerCompanyCode,
                                ""
                            )
                            userList.add(owner)
                        }

                        // 근로자 정보 추가
                        val employeesSnapshot = companySnapshot.child("employees")
                        for (employeeSnapshot in employeesSnapshot.children) {
                            val employeeCompanyCode =
                                employeeSnapshot.child("companyCode").getValue(String::class.java)

                            // 회사 코드가 로그인한 사용자의 회사 코드와 일치하는 경우 처리
                            if (employeeCompanyCode == userCompanyCode) {
                                val username =
                                    employeeSnapshot.child("username").getValue(String::class.java)
                                        ?: "알 수 없음"
                                val role =
                                    employeeSnapshot.child("role").getValue(String::class.java)
                                        ?: "알 수 없음" // 역할 기본값 처리
                                val email =
                                    employeeSnapshot.child("email").getValue(String::class.java)
                                        ?: "알 수 없음"
                                val phone =
                                    employeeSnapshot.child("phone").getValue(String::class.java)
                                        ?: "알 수 없음"

                                val employee = Employee(
                                    email,
                                    username,
                                    role,
                                    phone,
                                    employeeCompanyCode ?: "",
                                    "",
                                    ""
                                )
                                userList.add(employee)
                            }
                        }
                    }

                    // 로그를 통해 로드된 구성원 확인
                    if (userList.isEmpty()) {
                        Log.d("AllemployeeFragment", "같은 회사에 속한 구성원이 없습니다.")
                    } else {
                        Log.d("AllemployeeFragment", "구성원 목록: ${userList.size}명")
                    }

                    // ListView 어댑터 설정
                    val adapter = EmployeeAdapter(requireContext(), userList)
                    listView.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }


    // EmployeeAdapter 클래스
    private class EmployeeAdapter(
        context: Context,
        private val userList: List<Employee>
    ) : ArrayAdapter<Employee>(context, R.layout.item_allemployee, userList) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_allemployee, parent, false)

            val employee = userList[position]

            val nameTextView: TextView = view.findViewById(R.id.tvUsername)
            val roleTextView: TextView = view.findViewById(R.id.tvRole)
            val profileImageView: ImageView = view.findViewById(R.id.imageView_profile)

            nameTextView.text = employee.username
            roleTextView.text = employee.role
            profileImageView.setImageResource(R.drawable.baseline_person_2_24) // 프로필 이미지 고정

            return view
        }
    }
}
