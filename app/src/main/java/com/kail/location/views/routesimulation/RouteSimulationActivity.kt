package com.kail.location.views.routesimulation

import com.kail.location.views.base.BaseActivity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.MyLocationData
import com.baidu.mapapi.model.LatLng
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.kail.location.R
import com.kail.location.views.theme.locationTheme
import com.kail.location.viewmodels.RouteSimulationViewModel
import com.kail.location.views.locationsimulation.LocationSimulationActivity
import com.kail.location.views.settings.SettingsActivity

class RouteSimulationActivity : BaseActivity() {
    private val viewModel: RouteSimulationViewModel by viewModels()
    private var mMapView: MapView? = null
    private var mBaiduMap: BaiduMap? = null
    private var mLocClient: LocationClient? = null
    private var mCurrentLat by mutableStateOf(0.0)
    private var mCurrentLon by mutableStateOf(0.0)
    private var isFirstLoc = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = resources.getColor(R.color.colorPrimary, this.theme)

        // Initialize MapView
        mMapView = MapView(this)
        mBaiduMap = mMapView?.map
        mBaiduMap?.isMyLocationEnabled = true
        initMapLocation()
        com.elvishew.xlog.XLog.i("RouteSimulationActivity: Map and location initialized")
        
        var version = "v1.0.0"
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            version = "v${pInfo.versionName}"
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            locationTheme {
                var currentScreen by remember { mutableStateOf(Screen.LIST) }
                val onNavigate: (Int) -> Unit = { id ->
                    when (id) {
                        R.id.nav_location_simulation -> {
                            startActivity(Intent(this@RouteSimulationActivity, LocationSimulationActivity::class.java))
                            finish()
                        }
                        R.id.nav_route_simulation -> {
                            // Already here
                        }
                        R.id.nav_settings -> {
                            startActivity(Intent(this@RouteSimulationActivity, SettingsActivity::class.java))
                        }
                        R.id.nav_dev -> {
                            try {
                                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(this@RouteSimulationActivity, "无法打开开发者选项", Toast.LENGTH_SHORT).show()
                            }
                        }
                        else -> {
                            Toast.makeText(this@RouteSimulationActivity, "功能开发中...", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                when (currentScreen) {
                    Screen.LIST -> {
                        RouteSimulationScreen(
                            viewModel = viewModel,
                            onNavigate = onNavigate,
                            onAddRouteClick = { currentScreen = Screen.PLAN },
                            appVersion = version
                        )
                    }
                    Screen.PLAN -> {
                        RoutePlanScreen(
                            mapView = mMapView,
                            onBackClick = { currentScreen = Screen.LIST },
                            onConfirmClick = { currentScreen = Screen.LIST },
                            onLocateClick = {
                                mLocClient?.requestLocation()
                                val lat = mCurrentLat
                                val lon = mCurrentLon
                                val invalid = (Math.abs(lat) < 0.000001 && Math.abs(lon) < 0.000001) || (lat == 4.9E-324 || lon == 4.9E-324)
                                if (!invalid) {
                                    val ll = LatLng(lat, lon)
                                    mBaiduMap?.animateMapStatus(com.baidu.mapapi.map.MapStatusUpdateFactory.newLatLng(ll))
                                    com.elvishew.xlog.XLog.i("RouteSimulationActivity: Animate to current $ll")
                                } else {
                                    com.elvishew.xlog.XLog.w("RouteSimulationActivity: Current location unavailable")
                                }
                            },
                            currentLatLng = run {
                                val lat = mCurrentLat
                                val lon = mCurrentLon
                                val invalid = (Math.abs(lat) < 0.000001 && Math.abs(lon) < 0.000001) || (lat == 4.9E-324 || lon == 4.9E-324)
                                if (!invalid) LatLng(lat, lon) else null
                            },
                            onNavigate = onNavigate,
                            appVersion = version
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mMapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mMapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMapView?.onDestroy()
        mLocClient?.stop()
        mMapView = null
        mBaiduMap = null
    }

    enum class Screen {
        LIST, PLAN
    }

    private fun initMapLocation() {
        mBaiduMap?.isMyLocationEnabled = true
        // 建议使用 ApplicationContext 初始化 LocationClient，避免内存泄漏并确保 Context 稳定
        mLocClient = LocationClient(applicationContext)


        mLocClient?.registerLocationListener(object : BDAbstractLocationListener() {
            override fun onReceiveLocation(location: BDLocation?) {
                if (location == null || mBaiduMap == null) return

                // 过滤无效坐标
                if (Math.abs(location.latitude) < 0.000001 && Math.abs(location.longitude) < 0.000001) {
                    return
                }
                if (location.latitude == 4.9E-324 || location.longitude == 4.9E-324) {
                    return
                }

                mCurrentLat = location.latitude
                mCurrentLon = location.longitude

                val locData = MyLocationData.Builder()
                    .accuracy(location.radius)
                    .direction(0f)
                    .latitude(location.latitude)
                    .longitude(location.longitude)
                    .build()
                mBaiduMap?.setMyLocationData(locData)

                if (isFirstLoc) {
                    isFirstLoc = false
                    val ll = LatLng(location.latitude, location.longitude)
                    val u = com.baidu.mapapi.map.MapStatusUpdateFactory.newLatLng(ll)
                    mBaiduMap?.animateMapStatus(u)
                }
            }
        })
        val option = LocationClientOption()
        option.setOpenGps(true)
        option.setCoorType("bd09ll")
        option.setScanSpan(1000)
        mLocClient?.locOption = option
        mLocClient?.start()
    }
}
