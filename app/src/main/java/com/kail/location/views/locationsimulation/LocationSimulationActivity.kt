package com.kail.location.views.locationsimulation

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.kail.location.R
import com.kail.location.views.base.BaseActivity
import com.kail.location.viewmodels.LocationSimulationViewModel
import com.kail.location.views.theme.locationTheme
import com.kail.location.views.routesimulation.RouteSimulationActivity
import com.kail.location.views.settings.SettingsActivity
import android.widget.Toast

import com.kail.location.views.main.MainActivity

class LocationSimulationActivity : BaseActivity() {

    private val viewModel: LocationSimulationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var version = "v1.0.0"
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            version = "v${pInfo.versionName}"
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            locationTheme {
                LocationSimulationScreen(
                    viewModel = viewModel,
                    onNavigate = { id ->
                        when (id) {
                            R.id.nav_location_simulation -> {
                                // Already here
                            }
                            R.id.nav_route_simulation -> {
                                startActivity(Intent(this, RouteSimulationActivity::class.java))
                            }
                            R.id.nav_settings -> {
                                startActivity(Intent(this, SettingsActivity::class.java))
                            }
                            R.id.nav_dev -> {
                                try {
                                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(this, "无法打开开发者选项", Toast.LENGTH_SHORT).show()
                                }
                            }
                            // Add other navigation cases as needed
                            else -> {
                                Toast.makeText(this, "功能开发中...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onAddLocation = {
                        startActivity(Intent(this, MainActivity::class.java))
                    },
                    appVersion = version
                )
            }
        }
    }
}
