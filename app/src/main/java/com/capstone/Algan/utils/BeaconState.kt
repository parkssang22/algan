// BeaconState.kt
package com.capstone.Algan.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object BeaconState {
    private val _isConnected = MutableLiveData(false)  // 초기값 false
    val isConnected: LiveData<Boolean> get() = _isConnected

    fun setConnected(value: Boolean) {
        _isConnected.value = value
    }
}
