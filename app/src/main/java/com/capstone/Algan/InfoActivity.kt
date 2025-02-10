package com.capstone.Algan

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyPageActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage) // XML 연결

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // UI 요소 연결
        val usernameText = findViewById<TextView>(R.id.username)
        val emailText = findViewById<TextView>(R.id.email)
        val phoneText = findViewById<TextView>(R.id.phone)
        val roleText = findViewById<TextView>(R.id.role)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            userRef = database.reference.child("users").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        usernameText.text = user.username
                        emailText.text = user.email
                        phoneText.text = user.phone
                        roleText.text = user.role
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MyPageActivity, "데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
