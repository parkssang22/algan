package com.capstone.Algan

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.WindowInsets
import android.window.SplashScreen
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.capstone.Algan.fragments.NoticeBoardFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp


class MainActivity : AppCompatActivity() {

    private var backPressedTime: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

// 툴바 설정
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)


        // 툴바 메뉴 클릭 이벤트
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                // 전체 근로자 화면으로 이동
                R.id.menu_Allemployee -> {
                    replaceFragment(AllemployeeFragment())
                    true
                }
                //알림 화면으로 이동
                R.id.menu_alam ->{
                    replaceFragment(AlamFragment())
                    true
                }

                R.id.menu_mypage -> {
                    // 마이페이지 화면으로 이동
                    val intent = Intent(this, MyPageActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)

        // 기본 화면으로 급여 화면을 설정
        replaceFragment(SalaryFragment())

        // 하단 내비게이션 아이템 클릭 리스너 설정
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.fragment_salary -> {
                    replaceFragment(SalaryFragment()) // 급여 화면으로 이동
                    true
                }
                R.id.fragment_noticeboard -> {
                    replaceFragment(NoticeBoardFragment()) // 게시판 화면으로 이동
                    true
                }
                R.id.fragment_checklist -> {
                    replaceFragment(ChecklistFragment()) // 체크리스트 화면으로 이동
                    true
                }
                R.id.fragment_workrecord -> {
                    replaceFragment(WorkRecordFragment()) // 출퇴근 기록 화면으로 이동
                    true
                }
                else -> false
            }
        }
    }

    // Fragment 교체 함수
    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.main_container, fragment)
        transaction.commit()
    }

    // 뒤로가기 버튼 동작
    override fun onBackPressed() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            super.onBackPressed() // 2초 안에 뒤로가기를 다시 누르면 앱 종료
            return
        }

        AlertDialog.Builder(this).apply {
            setTitle("앱 종료")
            setMessage("앱을 종료하시겠습니까?")
            setPositiveButton("예") { _, _ -> finish() }
            setNegativeButton("아니요", null)
            show()
        }
        backPressedTime = currentTime
    }
}
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}