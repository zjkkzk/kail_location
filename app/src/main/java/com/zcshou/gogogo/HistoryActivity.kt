package com.zcshou.gogogo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zcshou.gogogo.ui.HistoryScreen
import com.zcshou.gogogo.ui.theme.GoGoGoTheme
import com.zcshou.utils.GoUtils

class HistoryActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* 为了启动欢迎页全屏，状态栏被设置了透明，但是会导致其他页面状态栏空白
         * 这里设计如下：
         * 1. 除了 WelcomeActivity 之外的所有 Activity 均继承 BaseActivity
         * 2. WelcomeActivity 单独处理，其他 Activity 手动填充 StatusBar
         * */
        window.statusBarColor = resources.getColor(R.color.colorPrimary, this.theme)

        setContent {
            GoGoGoTheme {
                val viewModel: HistoryViewModel = viewModel()
                HistoryScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    onLocationSelect = { name, lon, lat ->
                        if (!MainActivity.showLocation(name, lon, lat)) {
                            GoUtils.DisplayToast(this, getString(R.string.history_error_location))
                        }
                        finish()
                    }
                )
            }
        }
    }
}
