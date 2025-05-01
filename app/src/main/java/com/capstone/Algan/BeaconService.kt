package com.capstone.Algan

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import com.capstone.Algan.utils.BeaconState
import org.altbeacon.beacon.*

class BeaconService : Service(), BeaconConsumer {

    private lateinit var beaconManager: BeaconManager
    private val region = Region(
        "myBeaconRegion",
        Identifier.parse("0b2b0848-205f-11e9-ab14-820316983006"),
        null,
        null
    )
    private val handler = Handler(Looper.getMainLooper())
    private var lastSeenTime: Long = 0L
    private val beaconTimeout = 10_000L // 비콘 신호가 10초간 없으면 연결 해제
    private val checkRunnable = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            val hasSignal = currentTime - lastSeenTime <= beaconTimeout

            // 비콘 신호 상태 업데이트 (해제 로직 주석 처리)
            /*
            if (BeaconState.isConnected.value != hasSignal) {
                BeaconState.setConnected(hasSignal)
                if (hasSignal) {
                    showToast("비콘 연결됨")
                    Log.d("BeaconService", "비콘 연결됨")
                } else {
                    showToast("비콘 연결 해제됨")
                    Log.d("BeaconService", "비콘 신호가 끊어졌습니다.")
                }
            }
            */
            // 1초마다 상태 확인
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        // BeaconManager 설정
        beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        )
        beaconManager.bind(this)

        // 신호 상태 확인 시작 (주석 처리, 지속적인 확인 필요 없음)
        // handler.post(checkRunnable)
    }

    override fun onBeaconServiceConnect() {
        Log.d("BeaconService", "비콘 서비스 연결됨.")

        beaconManager.addRangeNotifier { beacons, _ ->
            if (beacons.isNotEmpty()) {
                lastSeenTime = System.currentTimeMillis()

                val nearestBeacon = beacons.first()
                val uuid = nearestBeacon.id1.toString()
                val major = nearestBeacon.id2.toInt()
                val minor = nearestBeacon.id3.toInt()
                val rssi = nearestBeacon.rssi // 신호 강도


                Log.d("BeaconService", "비콘 감지됨! UUID: $uuid, Major: $major, Minor: $minor, $rssi")
                showToast("$rssi")
                // 최초로 연결되었을 때만 상태 변경
                if (BeaconState.isConnected.value != true && rssi>=-90) {
                    BeaconState.setConnected(true)
                    showToast("비콘 연결됨! UUID: $uuid, Major: $major, Minor: $minor")
                }
            }
        }
        try {
            beaconManager.startRangingBeacons(region)
        } catch (e: RemoteException) {
            e.printStackTrace()
            Log.e("BeaconService", "비콘 감지 오류 발생")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager.unbind(this)
        // handler.removeCallbacks(checkRunnable) // 주석 처리: 해제 로직 제거
    }

    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }
}