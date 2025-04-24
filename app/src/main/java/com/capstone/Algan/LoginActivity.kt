package com.capstone.Algan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        // ActionBar 숨기기
        supportActionBar?.hide()

        val signupButton = findViewById<Button>(R.id.signup_button)
        signupButton.setOnClickListener {
            // SignUpActivity로 이동
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
        val savebtn = findViewById<Button>(R.id.savebtn)
        val emailField = findViewById<EditText>(R.id.Email)
        // 비밀번호 찾기 버튼 클릭 리스너
        savebtn.setOnClickListener {
            val email = emailField.text.toString().trim() // 이메일 입력 필드에서 이메일 가져오기

            // 이메일이 비어 있는지 확인
            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 비밀번호 재설정 이메일 전송
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 이메일 전송 성공
                        Toast.makeText(
                            this,
                            "비밀번호 재설정 이메일이 전송되었습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // 이메일 전송 실패
                        Toast.makeText(
                            this,
                            "이메일 전송에 실패했습니다. 이메일을 확인해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        val loginButton = findViewById<Button>(R.id.login_button)
        loginButton.setOnClickListener {
            val emailField = findViewById<EditText>(R.id.Email)
            val passwordField = findViewById<EditText>(R.id.password)

            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // 로그인 성공
                        Toast.makeText(this, "로그인 성공!", Toast.LENGTH_LONG).show()

                        // 현재 로그인한 사용자 ID 가져오기
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            fetchAndSaveUserInfo(userId, email) // 사용자 정보 가져와서 저장
                        }


                    } else {
                        // 로그인 실패
                        Toast.makeText(this, "로그인 실패. 다시 시도하세요.", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }



    /**
     * Firebase에서 사용자 정보를 가져와 SharedPreferences에 저장하는 함수
     */
    private fun fetchAndSaveUserInfo(userId: String, email: String) {
        val database = Firebase.database
        val companiesRef = database.getReference("companies")

        companiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (companySnapshot in snapshot.children) {
                    val companyCode = companySnapshot.key // 회사 코드 가져오기

                    if (companyCode != null) {
                        Log.d("LoginActivity", "찾은 회사 코드: $companyCode")

                        // 🔹 employee인지 확인
                        val employeeSnapshot = companySnapshot.child("employees").child(userId)
                        if (employeeSnapshot.exists()) {
                            val username =
                                employeeSnapshot.child("username").getValue(String::class.java)
                                    ?: "이름 없음"
                            saveUserInfoToPreferences(
                                userId,
                                username,
                                email,
                                companyCode
                            ) // companyCode 추가

                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                            return
                        }

                        // 🔹 owner인지 확인 (이메일 기준)
                        val ownerSnapshot = companySnapshot.child("owner")
                        val ownerEmail = ownerSnapshot.child("email").getValue(String::class.java)

                        if (ownerEmail == email) { // 이메일 일치 시 사업주로 판단
                            val username =
                                ownerSnapshot.child("username").getValue(String::class.java)
                                    ?: "이름 없음"
                            saveUserInfoToPreferences(
                                userId,
                                username,
                                email,
                                companyCode
                            ) // companyCode 추가

                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                            return
                        }
                    }
                }
                // 회사 또는 사용자 정보 찾을 수 없음
                Toast.makeText(this@LoginActivity, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LoginActivity", "사용자 정보 불러오기 실패: ${error.message}")
            }
        })
    }


    /**
     * SharedPreferences에 사용자 정보 저장하는 함수
     */
    private fun saveUserInfoToPreferences(
        userId: String,
        username: String,
        email: String,
        companyCode: String
    ) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("userId", userId)
        editor.putString("username", username)
        editor.putString("userEmail", email)
        editor.putString("companyCode", companyCode) // 🔹 companyCode 저장
        editor.apply()

        Log.d(
            "LoginActivity",
            "사용자 정보 저장 완료: ID=$userId, Name=$username, Email=$email, CompanyCode=$companyCode"
        )
    }
}