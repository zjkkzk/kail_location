package com.zcshou.gogogo

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBar

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* 为了启动欢迎页全屏，状态栏被设置了透明，但是会导致其他页面状态栏空白
         * 这里设计如下：
         * 1. 除了 WelcomeActivity 之外的所有 Activity 均继承 BaseActivity
         * 2. WelcomeActivity 单独处理，其他 Activity 手动填充 StatusBar
         * */
        window.statusBarColor = resources.getColor(R.color.colorPrimary, this.theme)

        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, FragmentSettings())
                .commit()
        }

        /* 获取默认的顶部的标题栏（安卓称为 ActionBar）*/
        val actionBar: ActionBar? = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            this.finish() // back button
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
