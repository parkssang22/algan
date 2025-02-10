package com.capstone.Algan

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class InfoActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        // ActionBar 숨기기
        supportActionBar?.hide()

        // Firebase 초기화
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // 로그인한 사용자 UID 가져오기
        val uid = auth.currentUser?.uid // 현재 로그인한 사용자의 UID

        // UID가 null이 아닐 경우 사용자 정보 가져오기
        uid?.let {
            fetchUserInfo(it)
        } ?: run {
            // UID가 null일 경우 처리 (예: 로그인되지 않은 경우)
        }
    }

    private fun fetchUserInfo(uid: String) {
        database.child("employees").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val employee = snapshot.getValue(Employee::class.java)

                    // 사용자 정보를 화면에 표시
                    employee?.let {
                        val userName = findViewById<TextView>(R.id.userName)
                        val userEmail = findViewById<TextView>(R.id.userEmail)
                        val userPhone = findViewById<TextView>(R.id.userPhone)
                        val userRole = findViewById<TextView>(R.id.userRole)
                        val userCompanyCode = findViewById<TextView>(R.id.companyCodeEditText)

                        userName.text = "이름: ${it.username}"
                        userEmail.text = "이메일: ${it.email}"
                        userPhone.text = "전화번호: ${it.phone}"
                        userRole.text = "역할: ${it.role}"
                        userCompanyCode.text = "회사 코드: ${it.companyCode}"
                    }
                } else {
                    // 데이터가 존재하지 않을 경우 처리 (예: Toast 메시지)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // 데이터 가져오기 실패 처리 (예: Toast 메시지)
            }
        })
    }
}
