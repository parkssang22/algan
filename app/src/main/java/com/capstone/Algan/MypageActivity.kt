package com.capstone.Algan

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MyPageActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var companyNameTextView: TextView
    private lateinit var companyCodeTextView: TextView
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        // FirebaseAuth 및 Realtime Database 초기화
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        usernameTextView = findViewById(R.id.usernameTextView)
        emailTextView = findViewById(R.id.emailTextView)
        phoneTextView = findViewById(R.id.phoneTextView)
        companyNameTextView = findViewById(R.id.companyNameTextView)
        companyCodeTextView = findViewById(R.id.companyCodeTextView)
        logoutButton = findViewById(R.id.logoutButton)

        // 사용자 정보 가져오기
        loadUserData()

        // 로그아웃 버튼 클릭 리스너
        logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
            finish() // 현재 Activity 종료하고 이전 화면으로 돌아감
        }
    }

    private fun loadUserData() {
        val sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE)

        val username = sharedPreferences.getString("username", "알 수 없음")
        val email = sharedPreferences.getString("email", "알 수 없음")
        val phone = sharedPreferences.getString("phone", "알 수 없음")
        val companyName = sharedPreferences.getString("companyName", "알 수 없음")
        val companyCode = sharedPreferences.getString("companyCode", "알 수 없음")

        // 텍스트뷰에 데이터 설정
        usernameTextView.text = "이름: $username"
        emailTextView.text = "이메일: $email"
        phoneTextView.text = "전화번호: $phone"
        companyNameTextView.text = "회사 이름: $companyName"
        companyCodeTextView.text = "회사 코드: $companyCode"
    }
}