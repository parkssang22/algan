package com.capstone.Algan

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
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

        val loginButton = findViewById<Button>(R.id.login_button)
        loginButton.setOnClickListener {
            val EmailField = findViewById<EditText>(R.id.Email)
            val passwordField = findViewById<EditText>(R.id.password)
            auth.signInWithEmailAndPassword(EmailField.text.toString(), passwordField.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Toast.makeText(this, "성공", Toast.LENGTH_LONG).show()

                        // MainActivity로 이동
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish() // 현재 액티비티 종료

                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(this, "실패", Toast.LENGTH_LONG).show()
                    }
                }
        }

        val savebtn = findViewById<Button>(R.id.savebtn)
        savebtn.setOnClickListener {
            val database = Firebase.database
            val myRef = database.getReference("message")

            myRef.setValue("Hello, World!")
        }
    }
}





