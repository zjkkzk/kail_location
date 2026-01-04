package com.kail.location.views.routesimulation

import com.kail.location.views.base.BaseActivity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.baidu.mapapi.map.MapView
import com.kail.location.R
import com.kail.location.views.theme.locationTheme

class RouteSimulationActivity : BaseActivity() {
    private var mMapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = resources.getColor(R.color.colorPrimary, this.theme)

        // Initialize MapView
        mMapView = MapView(this)
        
        setContent {
            locationTheme {
                var currentScreen by remember { mutableStateOf(Screen.LIST) }

                when (currentScreen) {
                    Screen.LIST -> {
                        RouteSimulationScreen(
                            onBackClick = { finish() },
                            onAddRouteClick = { currentScreen = Screen.PLAN }
                        )
                    }
                    Screen.PLAN -> {
                        RoutePlanScreen(
                            mapView = mMapView,
                            onBackClick = { currentScreen = Screen.LIST }
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
        mMapView = null
    }

    enum class Screen {
        LIST, PLAN
    }
}
