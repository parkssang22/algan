package com.capstone.Algan

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class InfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)
        // ActionBar 숨기기
        supportActionBar?.hide()
        // User 객체 생성
        val user = User(
            id = 1,
            username = "테스트이름",
            password = "password123",
            role = "사업주",
            phone = "010-1234-5678",
            email = "test@example.com",
            companyName = "테스트 회사"
        )

        // 사용자 정보 화면에 표시
        val userName = findViewById<TextView>(R.id.userName)
        val userEmail = findViewById<TextView>(R.id.userEmail)
        val userPhone = findViewById<TextView>(R.id.userPhone)
        val userRole = findViewById<TextView>(R.id.userRole)
        val companyName = findViewById<TextView>(R.id.companyName)

        userName.text = "이름: ${user.username}"
        userEmail.text = "이메일: ${user.email}"
        userPhone.text = "전화번호: ${user.phone}"
        userRole.text = "역할: ${user.role}"

        // 사업주만 회사명 표시
        if (user.role == "사업주") {
            companyName.text = "회사명: ${user.companyName}"
        }
    }
}
