package com.capstone.Algan  // 패키지명은 본인 앱에 맞게 수정

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "새 토큰: $token")
        // TODO: 토큰 서버 전송 처리 등 추가 가능
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_MESSAGE", "수신된 메시지: ${remoteMessage.data}")
        // TODO: 알림 처리 등 추가
    }
}
