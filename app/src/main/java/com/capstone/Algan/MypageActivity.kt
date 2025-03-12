package com.capstone.Algan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyPageActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var companyNameTextView: TextView
    private lateinit var companyCodeTextView: TextView
    private lateinit var userRoleTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var editButton: Button

    private lateinit var usernameEditText: EditText
    private lateinit var phoneEditText: EditText

    private var isEditing = false // 수정 모드 여부 체크

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // UI 요소 초기화
        userRoleTextView = findViewById(R.id.userrollTextView)
        usernameTextView = findViewById(R.id.usernameTextView)
        emailTextView = findViewById(R.id.emailTextView)
        phoneTextView = findViewById(R.id.phoneTextView)
        companyNameTextView = findViewById(R.id.companyNameTextView)
        companyCodeTextView = findViewById(R.id.companyCodeTextView)
        logoutButton = findViewById(R.id.logoutButton)
        editButton = findViewById(R.id.editButton)

        usernameEditText = findViewById(R.id.usernameEditText)
        phoneEditText = findViewById(R.id.phoneEditText)

        loadUserData()

        logoutButton.setOnClickListener {
            logout()
        }

        editButton.setOnClickListener {
            toggleEditMode()
        }
    }

    private fun loadUserData() {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null)

        if (userId == null) {
            Toast.makeText(this, "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val companiesRef = database.getReference("companies")
        companiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var userCompanyCode: String? = null
                var userSnapshot: DataSnapshot? = null
                var role: String? = null

                for (companySnapshot in snapshot.children) {
                    val ownerSnapshot = companySnapshot.child("owner")
                    if (ownerSnapshot.child("uid").getValue(String::class.java) == userId) {
                        userCompanyCode = ownerSnapshot.child("companyCode").getValue(String::class.java)
                        userSnapshot = ownerSnapshot
                        role = "사업주"
                        break
                    }

                    val employeesSnapshot = companySnapshot.child("employees")
                    for (employeeSnapshot in employeesSnapshot.children) {
                        if (employeeSnapshot.child("uid").getValue(String::class.java) == userId) {
                            userCompanyCode = employeeSnapshot.child("companyCode").getValue(String::class.java)
                            userSnapshot = employeeSnapshot
                            role = "근로자"
                            break
                        }
                    }
                }

                if (userSnapshot != null && role != null) {
                    loadUserInfo(userSnapshot, role, userCompanyCode, snapshot)
                } else {
                    Toast.makeText(this@MyPageActivity, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MyPageActivity, "데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserInfo(userSnapshot: DataSnapshot, role: String, userCompanyCode: String?, snapshot: DataSnapshot) {
        val username = userSnapshot.child("username").getValue(String::class.java) ?: "알 수 없음"
        val email = userSnapshot.child("email").getValue(String::class.java) ?: "알 수 없음"
        val phone = userSnapshot.child("phone").getValue(String::class.java) ?: "알 수 없음"
        val companyCode = userCompanyCode ?: "알 수 없음"
        var companyName = "알 수 없음"

        for (companySnapshot in snapshot.children) {
            val ownerSnapshot = companySnapshot.child("owner")
            if (ownerSnapshot.child("companyCode").getValue(String::class.java) == companyCode) {
                companyName = ownerSnapshot.child("companyName").getValue(String::class.java) ?: "알 수 없음"
                break
            }
        }

        usernameTextView.text = "이름: $username"
        emailTextView.text = "이메일: $email"
        phoneTextView.text = "전화번호: $phone"
        userRoleTextView.text = "$role"
        companyNameTextView.text = "회사 이름: $companyName"
        companyCodeTextView.text = "회사 코드: $companyCode"
    }

    private fun toggleEditMode() {
        if (isEditing) {
            saveUserData()
        } else {
            enableEditing(true)
        }
        isEditing = !isEditing
    }

    private fun enableEditing(enable: Boolean) {
        if (enable) {
            usernameTextView.visibility = View.GONE
            phoneTextView.visibility = View.GONE

            usernameEditText.visibility = View.VISIBLE
            phoneEditText.visibility = View.VISIBLE

            usernameEditText.setText(usernameTextView.text.toString().replace("이름: ", ""))
            phoneEditText.setText(phoneTextView.text.toString().replace("전화번호: ", ""))

            editButton.text = "저장하기"
        } else {
            usernameTextView.visibility = View.VISIBLE
            emailTextView.visibility = View.VISIBLE
            phoneTextView.visibility = View.VISIBLE

            usernameEditText.visibility = View.GONE
            phoneEditText.visibility = View.GONE

            editButton.text = "수정하기"
        }
    }

    private fun saveUserData() {
        val newUsername = usernameEditText.text.toString().trim()
        val newPhone = phoneEditText.text.toString().trim()

        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null) ?: return
        val role = userRoleTextView.text.toString().replace("역할: ", "")
        val companyCode = companyCodeTextView.text.toString().replace("회사 코드: ", "")

        // 🔹 Firebase 경로: companies/{회사코드}/owner OR employees/{userId}
        val rolePath = if (role == "사업주") "owner" else "employees"
        // 🔹 Firebase 경로 설정: 사업주일 때는 owner 아래에, 근로자일 때는 employees/{userId} 아래에 저장
        val userRef = if (role == "사업주") {
            // 사업주일 경우: owner 아래 바로 저장
            database.getReference("companies")
                .child(companyCode)
                .child("owner") // 사업주는 owner 아래에 저장
        } else {
            // 근로자일 경우: employees/{userId} 아래 저장
            database.getReference("companies")
                .child(companyCode)
                .child("employees")
                .child(userId)
        }

        val updatedData = mapOf(
            "username" to newUsername,
            "phone" to newPhone
        )

        userRef.updateChildren(updatedData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                usernameTextView.text = "이름: $newUsername"
                phoneTextView.text = "전화번호: $newPhone"

                enableEditing(false)
                isEditing = false
                Toast.makeText(this, "정보가 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "저장 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun logout() {
        auth.signOut()
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
