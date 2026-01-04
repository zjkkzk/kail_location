package com.kail.location.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.kail.location.models.UpdateInfo
import com.kail.location.utils.UpdateChecker
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.flow.update

class LocationSimulationViewModel(application: Application) : AndroidViewModel(application) {

    data class LocationInfo(
        val name: String = "NONE",
        val address: String = "NONE • NONE",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    )

    private val _locationInfo = MutableStateFlow(LocationInfo())
    val locationInfo: StateFlow<LocationInfo> = _locationInfo.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _isJoystickEnabled = MutableStateFlow(false)
    val isJoystickEnabled: StateFlow<Boolean> = _isJoystickEnabled.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    fun toggleSimulation() {
        _isSimulating.value = !_isSimulating.value
        // Logic to start/stop service would go here or be observed by Activity
    }

    fun setJoystickEnabled(enabled: Boolean) {
        _isJoystickEnabled.value = enabled
    }

    fun checkUpdate(context: Context, isAuto: Boolean = false) {
        UpdateChecker.check(context) { info, error ->
            if (info != null) {
                _updateInfo.value = info
            } else {
                if (!isAuto) {
                    // Use MainExecutor to show toast
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        if (error != null) {
                            Toast.makeText(context, "检查更新失败: $error", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
    }
}
