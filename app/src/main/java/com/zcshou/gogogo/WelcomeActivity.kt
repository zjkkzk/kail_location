package com.zcshou.gogogo

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.zcshou.utils.GoUtils
import java.util.ArrayList

class WelcomeActivity : AppCompatActivity() {
    private lateinit var preferences: SharedPreferences
    private lateinit var checkBox: CheckBox
    private var mAgreement = false
    private var mPrivacy = false

    companion object {
        private const val KEY_ACCEPT_AGREEMENT = "KEY_ACCEPT_AGREEMENT"
        private const val KEY_ACCEPT_PRIVACY = "KEY_ACCEPT_PRIVACY"
        private const val SDK_PERMISSION_REQUEST = 127
    }

    private var isPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // 生成默认参数的值（一定要尽可能早的调用，因为后续有些界面可能需要使用参数）
        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false)

        val startBtn = findViewById<Button>(R.id.startButton)
        startBtn.setOnClickListener { startMainActivity() }

        checkAgreementAndPrivacy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == SDK_PERMISSION_REQUEST) {
            // 只要有定位权限，就视为通过（存储和电话权限为可选）
            val hasFineLocation = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarseLocation = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            
            if (hasFineLocation || hasCoarseLocation) {
                isPermission = true
                startMainActivity()
            } else {
                // 必要的定位权限被拒绝，引导用户去设置
                showPermissionSettingsDialog()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun showPermissionSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage("GoGoGo需要位置权限才能运行。\n请点击“去设置”手动开启位置权限。")
            .setPositiveButton("去设置") { _, _ ->
                try {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    GoUtils.DisplayToast(this, "无法打开设置页面，请手动开启")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun checkDefaultPermissions() {
        val permissionsToRequest = ArrayList<String>()
        
        // 定位精确位置
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        /*
         * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
         */
        // 读写权限
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // 读取电话状态权限
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
        }

        if (permissionsToRequest.isEmpty()) {
            isPermission = true
        } else {
            requestPermissions(permissionsToRequest.toTypedArray(), SDK_PERMISSION_REQUEST)
        }
    }

    private fun startMainActivity() {
        if (!checkBox.isChecked) {
            GoUtils.DisplayToast(this, resources.getString(R.string.app_error_agreement))
            return
        }

        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, resources.getString(R.string.app_error_network))
            return
        }

        if (!GoUtils.isGpsOpened(this)) {
            GoUtils.DisplayToast(this, resources.getString(R.string.app_error_gps))
            return
        }

        if (isPermission) {
            val intent = Intent(this@WelcomeActivity, MainActivity::class.java)
            startActivity(intent)
            this@WelcomeActivity.finish()
        } else {
            checkDefaultPermissions()
        }
    }

    private fun doAcceptation() {
        if (mAgreement && mPrivacy) {
            checkBox.isChecked = true
            checkDefaultPermissions()
        } else {
            checkBox.isChecked = false
        }
        //实例化Editor对象
        val editor = preferences.edit()
        //存入数据
        editor.putBoolean(KEY_ACCEPT_AGREEMENT, mAgreement)
        editor.putBoolean(KEY_ACCEPT_PRIVACY, mPrivacy)
        //提交修改
        editor.apply()
    }

    private fun showAgreementDialog() {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.show()
        alertDialog.setCancelable(false)
        val window = alertDialog.window
        if (window != null) {
            window.setContentView(R.layout.user_agreement)
            window.setGravity(Gravity.CENTER)
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut)

            val tvContent = window.findViewById<TextView>(R.id.tv_content)
            val tvCancel = window.findViewById<Button>(R.id.tv_cancel)
            val tvAgree = window.findViewById<Button>(R.id.tv_agree)
            val ssb = SpannableStringBuilder()
            ssb.append(resources.getString(R.string.app_agreement_content))
            tvContent.movementMethod = LinkMovementMethod.getInstance()
            tvContent.setText(ssb, TextView.BufferType.SPANNABLE)

            tvCancel.setOnClickListener {
                mAgreement = false
                doAcceptation()
                alertDialog.cancel()
            }

            tvAgree.setOnClickListener {
                mAgreement = true
                doAcceptation()
                alertDialog.cancel()
            }
        }
    }

    private fun showPrivacyDialog() {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.show()
        alertDialog.setCancelable(false)
        val window = alertDialog.window
        if (window != null) {
            window.setContentView(R.layout.user_privacy)
            window.setGravity(Gravity.CENTER)
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut)

            val tvContent = window.findViewById<TextView>(R.id.tv_content)
            val tvCancel = window.findViewById<Button>(R.id.tv_cancel)
            val tvAgree = window.findViewById<Button>(R.id.tv_agree)
            val ssb = SpannableStringBuilder()
            ssb.append(resources.getString(R.string.app_privacy_content))
            tvContent.movementMethod = LinkMovementMethod.getInstance()
            tvContent.setText(ssb, TextView.BufferType.SPANNABLE)

            tvCancel.setOnClickListener {
                mPrivacy = false
                doAcceptation()
                alertDialog.cancel()
            }

            tvAgree.setOnClickListener {
                mPrivacy = true
                doAcceptation()
                alertDialog.cancel()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun checkAgreementAndPrivacy() {
        preferences = getSharedPreferences(KEY_ACCEPT_AGREEMENT, MODE_PRIVATE)
        mPrivacy = preferences.getBoolean(KEY_ACCEPT_PRIVACY, false)
        mAgreement = preferences.getBoolean(KEY_ACCEPT_AGREEMENT, false)

        checkBox = findViewById(R.id.check_agreement)
        // 拦截 CheckBox 的点击事件
        checkBox.setOnTouchListener { v, event ->
            if (v is TextView) {
                val method = v.movementMethod
                if (method != null && v.text is Spannable
                    && event.action == MotionEvent.ACTION_UP
                ) {
                    if (method.onTouchEvent(v, v.text as Spannable, event)) {
                        event.action = MotionEvent.ACTION_CANCEL
                    }
                }
            }
            false
        }
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!mPrivacy || !mAgreement) {
                    GoUtils.DisplayToast(this, resources.getString(R.string.app_error_read))
                    checkBox.isChecked = false
                }
            } else {
                mPrivacy = false
                mAgreement = false
            }
        }

        val str = getString(R.string.app_agreement_privacy)
        val builder = getSpannableStringBuilder(str)

        checkBox.text = builder
        checkBox.movementMethod = LinkMovementMethod.getInstance()

        if (mPrivacy && mAgreement) {
            checkBox.isChecked = true
            checkDefaultPermissions()
        } else {
            checkBox.isChecked = false
        }
    }

    private fun getSpannableStringBuilder(str: String): SpannableStringBuilder {
        val builder = SpannableStringBuilder(str)
        val clickSpanAgreement = object : ClickableSpan() {
            override fun onClick(widget: View) {
                showAgreementDialog()
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = resources.getColor(R.color.colorPrimary, this@WelcomeActivity.theme)
                ds.isUnderlineText = false
            }
        }
        val agreementStart = str.indexOf("《")
        val agreementEnd = str.indexOf("》") + 1
        builder.setSpan(clickSpanAgreement, agreementStart, agreementEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val clickSpanPrivacy = object : ClickableSpan() {
            override fun onClick(widget: View) {
                showPrivacyDialog()
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = resources.getColor(R.color.colorPrimary, this@WelcomeActivity.theme)
                ds.isUnderlineText = false
            }
        }
        val privacyStart = str.indexOf("《", agreementEnd)
        val privacyEnd = str.indexOf("》", agreementEnd) + 1
        builder.setSpan(clickSpanPrivacy, privacyStart, privacyEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return builder
    }
}
