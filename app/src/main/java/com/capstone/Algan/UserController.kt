package com.capstone.Algan

class UserController(private val userService: UserService) {

    fun handleUserRegistration(
        uid: String,
        password: String, // 비밀번호 추가
        role: String,
        phone: String,
        email: String,
        companyName : String,
        companyCode: String,
        invitationCode: String
    ) {
        try {
            // 사용자 등록 처리
            userService.registerUser(uid, password, role, phone, email, companyName, companyCode, invitationCode)
            println("User registered successfully!")
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }

    fun handleLogin(username: String, password: String): Boolean {
        // TODO: 로그인 검증 로직 추가
        println("Login attempted with username: $username")
        return true // 임시로 로그인 성공 반환
    }
}
