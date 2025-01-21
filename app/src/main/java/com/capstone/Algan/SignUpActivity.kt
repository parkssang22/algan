package com.capstone.Algan

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // FirebaseAuth 및 Realtime Database 초기화
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val emailEditText: EditText = findViewById(R.id.emailEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val idField: EditText = findViewById(R.id.idEditText)
        val usernameField: EditText = findViewById(R.id.usernameEditText)
        val phoneField: EditText = findViewById(R.id.phoneEditText)
        val roleGroup: RadioGroup = findViewById(R.id.roleGroup)
        val signupButton: Button = findViewById(R.id.signupButton)

        // 뒤로가기 버튼 클릭 리스너 추가
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish() // 현재 액티비티 종료
        }

        signupButton.setOnClickListener {
            val id = idField.text.toString()
            val username = usernameField.text.toString()
            val password = passwordEditText.text.toString()
            val email = emailEditText.text.toString()
            val phone = phoneField.text.toString()
            val selectedRoleId = roleGroup.checkedRadioButtonId
            val role = findViewById<RadioButton>(selectedRoleId)?.text.toString()

            // 필드 체크 (빈 값이면 에러 메시지)
            if (id.isEmpty() || username.isEmpty() || password.isEmpty() || email.isEmpty() || phone.isEmpty() || role.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 회원가입 처리
            signUp(id, username, password, role, email, phone)
        }
    }

    private fun signUp(id: String, username: String, password: String, role: String, email: String, phone: String) {
        // Firebase Authentication을 사용하여 사용자 생성
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // 회원가입 성공 시 Realtime Database에 사용자 정보 저장
                    val userId = auth.currentUser!!.uid
                    val user = User(id, username, password, email, phone, role)

                    // Realtime Database에 사용자 정보 저장
                    database.reference.child("users").child(userId).setValue(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                            finish()  // 회원가입 후 현재 화면 종료
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Realtime Database 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}


