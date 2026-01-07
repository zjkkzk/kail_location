package com.kail.location.views.sponsor

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.kail.location.views.base.BaseActivity
import com.kail.location.views.theme.locationTheme
import com.kail.location.viewmodels.SponsorViewModel

class SponsorActivity : BaseActivity() {
    private val viewModel: SponsorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            locationTheme {
                SponsorScreen(viewModel = viewModel, onBackClick = { finish() })
            }
        }
    }
}
