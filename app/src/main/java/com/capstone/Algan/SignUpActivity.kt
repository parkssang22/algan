package com.capstone.Algan

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var companyNameEditText: EditText
    private lateinit var generatedCompanyCodeTextView: TextView
    private lateinit var companyCodeEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // FirebaseAuth 및 Realtime Database 초기화
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val emailEditText: EditText = findViewById(R.id.emailEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val usernameField: EditText = findViewById(R.id.usernameEditText)
        val phoneField: EditText = findViewById(R.id.phoneEditText)
        val roleGroup: RadioGroup = findViewById(R.id.roleGroup)

        companyNameEditText = findViewById(R.id.companyNameEditText)
        companyCodeEditText = findViewById(R.id.companyCodeEditText)
        generatedCompanyCodeTextView = findViewById(R.id.generatedCompanyCodeTextView)

        val signupButton: Button = findViewById(R.id.signupButton)

        // 역할 선택에 따라 입력 필드 가시성 조정
        roleGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.employerRadioButton -> {
                    companyNameEditText.visibility = EditText.VISIBLE
                    companyCodeEditText.visibility = EditText.GONE
                    generatedCompanyCodeTextView.visibility = TextView.VISIBLE
                }
                R.id.employeeRadioButton -> {
                    companyNameEditText.visibility = EditText.GONE
                    companyCodeEditText.visibility = EditText.VISIBLE
                    generatedCompanyCodeTextView.visibility = TextView.GONE
                }
            }
        }

        // 회사 이름 입력 필드에 텍스트 변경 리스너 추가
        companyNameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length >= 3) {
                    val companyCode = generateCompanyCode(s.toString())
                    generatedCompanyCodeTextView.text = "$companyCode"
                } else {
                    generatedCompanyCodeTextView.text = ""
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 뒤로가기 버튼 클릭 리스너 추가
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        signupButton.setOnClickListener {
            val username = usernameField.text.toString()
            val password = passwordEditText.text.toString()
            val email = emailEditText.text.toString()
            val phone = phoneField.text.toString()
            val companyName = companyNameEditText.text.toString()
            val selectedRoleId = roleGroup.checkedRadioButtonId
            val role = findViewById<RadioButton>(selectedRoleId)?.text.toString()
            val companyCode = companyCodeEditText.text.toString()

            // 필드 체크
            if (username.isEmpty() || password.isEmpty() || email.isEmpty() || phone.isEmpty() || role.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 역할에 따라 회원가입 처리
            if (role == "사업주") {
                if (companyName.isEmpty()) {
                    Toast.makeText(this, "회사 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val companyCodeWithSuffix = generatedCompanyCodeTextView.text.toString().replace("회사 코드: ", "")
                signUpAsBusinessOwner(username, password, email, phone, companyName, companyCodeWithSuffix)
            } else if (role == "근로자") {
                if (companyCode.isEmpty()) {
                    Toast.makeText(this, "회사 코드를 입력하세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                signUpAsEmployee(username, password, email, phone, companyCode, companyName)
            }
        }
    }

    private fun generateCompanyCode(companyName: String): String {
        val nameSuffix = companyName.takeLast(3)
        val randomSuffix = (10000..99999).random().toString()
        return "$nameSuffix$randomSuffix"
    }

    private fun signUpAsBusinessOwner(username: String, password: String, email: String, phone: String, companyName: String, companyCode: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser!!.uid
                    val user = BusinessOwner(uid = userId, username = username, phone = phone, email = email, companyName = companyName, companyCode = companyCode)

                    database.reference.child("companies").child(companyCode).child("owner")
                        .setValue(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "$companyCode", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Realtime Database 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signUpAsEmployee(username: String, password: String, email: String, phone: String, companyCode: String, companyName: String) {
        isCompanyCodeValid(companyCode) { isValid ->
            if (isValid) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser!!.uid
                            val user = Employee(uid = userId, username = username, phone = phone, email = email, companyCode = companyCode, salary = null, companyName = companyName, workingHours = null)

                            database.reference.child("companies").child(companyCode).child("employees").child(userId).setValue(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "근로자 회원가입 성공!", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Realtime Database 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "유효하지 않은 회사 코드입니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isCompanyCodeValid(companyCode: String, callback: (Boolean) -> Unit) {
        database.reference.child("companies").child(companyCode).get()
            .addOnSuccessListener { snapshot ->
                callback(snapshot.exists())
            }.addOnFailureListener {
                Toast.makeText(this, "회사 코드 확인 실패", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }
}