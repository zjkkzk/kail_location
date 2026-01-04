package com.kail.location.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.kail.location.models.RouteInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.ArrayList

import com.kail.location.models.UpdateInfo
import com.kail.location.utils.UpdateChecker
import android.content.Context
import android.widget.Toast

class RouteSimulationViewModel(application: Application) : AndroidViewModel(application) {
    private val _historyRoutes = MutableStateFlow<List<RouteInfo>>(emptyList())
    val historyRoutes: StateFlow<List<RouteInfo>> = _historyRoutes.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    init {
        loadRoutes()
    }

    fun checkUpdate(context: Context, isAuto: Boolean = false) {
        UpdateChecker.check(context) { info, error ->
            if (info != null) {
                _updateInfo.value = info
            } else {
                if (!isAuto) {
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

    fun loadRoutes() {
        viewModelScope.launch {
            val prefs = PreferenceManager.getDefaultSharedPreferences(getApplication())
            val res = prefs.getString("saved_routes", "[]") ?: "[]"
            val list = parseRoutes(res)
            _historyRoutes.value = list
        }
    }

    private fun parseRoutes(json: String): List<RouteInfo> {
        return try {
            val arr = JSONArray(json)
            val list = ArrayList<Pair<Long, RouteInfo>>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val time = obj.optLong("time", 0L)
                val points = obj.optJSONArray("points") ?: continue
                if (points.length() == 0) continue
                
                val first = points.optJSONObject(0) ?: continue
                val last = points.optJSONObject(points.length() - 1) ?: continue
                
                val s = String.format("%.6f,%.6f", first.optDouble("lat"), first.optDouble("lng"))
                val e = String.format("%.6f,%.6f", last.optDouble("lat"), last.optDouble("lng"))
                
                list.add(time to RouteInfo(time.toString(), s, e, ""))
            }
            list.sortByDescending { it.first }
            list.map { it.second }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
