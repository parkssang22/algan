package com.capstone.Algan
class UserService {
    private val users = mutableListOf<User>()
    private val invitationCodes = mutableListOf<InvitationCode>()

    fun validateInvitationCode(code: String): Boolean {
        val invitation = invitationCodes.find { it.code == code }
        return if (invitation != null && !invitation.isUsed && System.currentTimeMillis() <= invitation.expiryDate) {
            true
        } else {
            throw IllegalArgumentException("Invalid or expired invitation code")
        }
    }

    fun registerUser(
        username: String,
        password: String,
        role: String,
        phone: String,
        email: String,
        companyName : String,
        invitationCode: String
    ) {
        try {
            if (validateInvitationCode(invitationCode)) {
                val newUser = User(username, password, role, phone, email, companyName)
                users.add(newUser)
                invitationCodes.find { it.code == invitationCode }?.isUsed = true
            }
        } catch (e: Exception) {
            // 초대 코드가 유효하지 않은 경우 처리
            throw IllegalArgumentException("Failed to register user: ${e.message}")
        }
    }
}

