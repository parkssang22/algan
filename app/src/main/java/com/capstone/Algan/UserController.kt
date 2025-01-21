package com.capstone.Algan

class UserController(private val userService: UserService) {

    fun handleUserRegistration(
        id: String,
        username: String,
        password: String,
        role: String,
        phone: String,
        email: String,
        companyName: String? = null, // 사업주일 경우에만 필요
        invitationCode: String
    ) {
        try {
            // 사용자 등록 처리
            userService.registerUser(id, username, password, role, phone, email, companyName, invitationCode)
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
