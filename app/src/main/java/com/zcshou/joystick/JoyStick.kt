package com.zcshou.joystick

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.PixelFormat
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.preference.PreferenceManager
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.sug.SuggestionSearch
import com.baidu.mapapi.search.sug.SuggestionSearchOption
import com.elvishew.xlog.XLog
import com.zcshou.database.DataBaseHistoryLocation
import com.zcshou.gogogo.HistoryActivity
import com.zcshou.gogogo.MainActivity
import com.zcshou.gogogo.R
import com.zcshou.utils.GoUtils
import com.zcshou.utils.MapUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class JoyStick @JvmOverloads constructor(
    private val mContext: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(mContext, attrs, defStyleAttr) {

    private var mWindowParamCurrent: WindowManager.LayoutParams = WindowManager.LayoutParams()
    private lateinit var mWindowManager: WindowManager
    private var mCurWin = WINDOW_TYPE_JOYSTICK
    private val inflater: LayoutInflater = LayoutInflater.from(mContext)
    private var isWalk = false
    private lateinit var btnWalk: ImageButton
    private var isRun = false
    private lateinit var btnRun: ImageButton
    private var isBike = false
    private lateinit var btnBike: ImageButton
    private var mListener: JoyStickClickListener? = null

    // 移动
    private lateinit var mJoystickLayout: View
    private lateinit var mTimer: GoUtils.TimeCount
    private var isMove = false
    private var mSpeed = 1.2        /* 默认的速度，单位 m/s */
    private var mAltitude = 55.0
    private var mAngle = 0.0
    private var mR = 0.0
    private var disLng = 0.0
    private var disLat = 0.0
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
    /* 历史记录悬浮窗相关 */
    private lateinit var mHistoryLayout: FrameLayout
    private val mAllRecord: MutableList<Map<String, Any>> = ArrayList()
    private lateinit var noRecordText: TextView
    private lateinit var mRecordListView: ListView
    /* 地图悬浮窗相关 */
    private lateinit var mMapLayout: FrameLayout
    private lateinit var mMapView: MapView
    private lateinit var mBaiduMap: BaiduMap
    private lateinit var mCurMapLngLat: LatLng
    private var mMarkMapLngLat: LatLng? = null
    private lateinit var mSuggestionSearch: SuggestionSearch
    private lateinit var mSearchList: ListView
    private lateinit var mSearchLayout: LinearLayout

    companion object {
        private const val DivGo = 1000L    /* 移动的时间间隔，单位 ms */
        private const val WINDOW_TYPE_JOYSTICK = 0
        private const val WINDOW_TYPE_MAP = 1
        private const val WINDOW_TYPE_HISTORY = 2
    }

    init {
        initWindowManager()

        if (true) { // inflater is always not null from from(context)
            initJoyStickView()
            try {
                initJoyStickMapView()
            } catch (e: Exception) {
                XLog.e("JoyStick: Error initializing MapView", e)
            }
            initHistoryView()
        }
    }

    fun setCurrentPosition(lng: Double, lat: Double, alt: Double) {
        val lngLat = MapUtils.wgs2bd09(lng, lat)
        mCurMapLngLat = LatLng(lngLat[1], lngLat[0])
        mAltitude = alt

        resetBaiduMap()
    }

    fun show() {
        try {
            when (mCurWin) {
                WINDOW_TYPE_MAP -> {
                    if (this::mJoystickLayout.isInitialized && mJoystickLayout.parent != null) {
                        mWindowManager.removeView(mJoystickLayout)
                    }
                    if (this::mHistoryLayout.isInitialized && mHistoryLayout.parent != null) {
                        mWindowManager.removeView(mHistoryLayout)
                    }
                    if (this::mMapLayout.isInitialized && mMapLayout.parent == null) {
                        resetBaiduMap()
                        mWindowManager.addView(mMapLayout, mWindowParamCurrent)
                    }
                }
                WINDOW_TYPE_HISTORY -> {
                    if (this::mMapLayout.isInitialized && mMapLayout.parent != null) {
                        mWindowManager.removeView(mMapLayout)
                    }
                    if (this::mJoystickLayout.isInitialized && mJoystickLayout.parent != null) {
                        mWindowManager.removeView(mJoystickLayout)
                    }
                    if (this::mHistoryLayout.isInitialized && mHistoryLayout.parent == null) {
                        mWindowManager.addView(mHistoryLayout, mWindowParamCurrent)
                    }
                }
                WINDOW_TYPE_JOYSTICK -> {
                    if (this::mMapLayout.isInitialized && mMapLayout.parent != null) {
                        mWindowManager.removeView(mMapLayout)
                    }
                    if (this::mHistoryLayout.isInitialized && mHistoryLayout.parent != null) {
                        mWindowManager.removeView(mHistoryLayout)
                    }
                    if (this::mJoystickLayout.isInitialized && mJoystickLayout.parent == null) {
                        mWindowManager.addView(mJoystickLayout, mWindowParamCurrent)
                    }
                }
            }
        } catch (e: Exception) {
            XLog.e("JoyStick: Error in show()", e)
        }
    }

    fun hide() {
        if (this::mMapLayout.isInitialized && mMapLayout.parent != null) {
            mWindowManager.removeViewImmediate(mMapLayout)
        }

        if (this::mJoystickLayout.isInitialized && mJoystickLayout.parent != null) {
            mWindowManager.removeViewImmediate(mJoystickLayout)
        }

        if (this::mHistoryLayout.isInitialized && mHistoryLayout.parent != null) {
            mWindowManager.removeViewImmediate(mHistoryLayout)
        }
    }

    fun destroy() {
        try {
            if (this::mTimer.isInitialized) {
                mTimer.cancel()
            }

            if (this::mMapLayout.isInitialized && mMapLayout.parent != null) {
                mWindowManager.removeViewImmediate(mMapLayout)
            }

            if (this::mJoystickLayout.isInitialized && mJoystickLayout.parent != null) {
                mWindowManager.removeViewImmediate(mJoystickLayout)
            }

            if (this::mHistoryLayout.isInitialized && mHistoryLayout.parent != null) {
                mWindowManager.removeViewImmediate(mHistoryLayout)
            }

            if (this::mBaiduMap.isInitialized) {
                mBaiduMap.isMyLocationEnabled = false
            }
            if (this::mMapView.isInitialized) {
                mMapView.onDestroy()
            }
        } catch (e: Exception) {
            XLog.e("JoyStick: Error in destroy()", e)
        }
    }

    fun setListener(mListener: JoyStickClickListener) {
        this.mListener = mListener
    }

    private fun initWindowManager() {
        mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mWindowParamCurrent = WindowManager.LayoutParams()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mWindowParamCurrent.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            mWindowParamCurrent.type = WindowManager.LayoutParams.TYPE_PHONE
        }
        mWindowParamCurrent.format = PixelFormat.RGBA_8888
        mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or      // 不添加这个将导致游戏无法启动（MIUI12）,添加之后导致键盘无法显示
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        mWindowParamCurrent.gravity = Gravity.START or Gravity.TOP
        mWindowParamCurrent.width = WindowManager.LayoutParams.WRAP_CONTENT
        mWindowParamCurrent.height = WindowManager.LayoutParams.WRAP_CONTENT
        mWindowParamCurrent.x = 300
        mWindowParamCurrent.y = 300
    }

    @SuppressLint("InflateParams")
    private fun initJoyStickView() {
        /* 移动计时器 */
        mTimer = GoUtils.TimeCount(DivGo, DivGo)
        mTimer.setListener(object : GoUtils.TimeCount.TimeCountListener {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                // 注意：这里的 x y 与 圆中角度的对应问题（以 X 轴正向为 0 度）且转换为 km
                disLng = mSpeed * (DivGo / 1000.0) * mR * cos(mAngle * 2.0 * Math.PI / 360) / 1000 // 注意安卓中的三角函数使用的是弧度
                disLat = mSpeed * (DivGo / 1000.0) * mR * sin(mAngle * 2.0 * Math.PI / 360) / 1000 // 注意安卓中的三角函数使用的是弧度
                mListener?.onMoveInfo(mSpeed, disLng, disLat, 90.0 - mAngle)
                mTimer.start()
            }
        })
        // 获取参数区设置的速度
        try {
            mSpeed = sharedPreferences.getString("setting_walk", resources.getString(R.string.setting_walk_default))?.toDouble() ?: 1.2
        } catch (e: NumberFormatException) {  // GOOD: The exception is caught.
            mSpeed = 1.2
        }
        mJoystickLayout = inflater.inflate(R.layout.joystick, null)

        /* 整个摇杆拖动事件处理 */
        mJoystickLayout.setOnTouchListener(JoyStickOnTouchListener())

        /* 位置按钮点击事件处理 */
        val btnPosition = mJoystickLayout.findViewById<ImageButton>(R.id.joystick_position)
        btnPosition.setOnClickListener {
            if (mMapLayout.parent == null) {
                mCurWin = WINDOW_TYPE_MAP
                show()
            }
        }

        /* 历史按钮点击事件处理 */
        val btnHistory = mJoystickLayout.findViewById<ImageButton>(R.id.joystick_history)
        btnHistory.setOnClickListener {
            if (mHistoryLayout.parent == null) {
                mCurWin = WINDOW_TYPE_HISTORY
                show()
            }
        }

        /* 步行按键的点击处理 */
        btnWalk = mJoystickLayout.findViewById(R.id.joystick_walk)
        btnWalk.setOnClickListener {
            if (!isWalk) {
                btnWalk.setColorFilter(resources.getColor(R.color.colorAccent, mContext.theme))
                isWalk = true
                btnRun.setColorFilter(resources.getColor(R.color.black, mContext.theme))
                isRun = false
                btnBike.setColorFilter(resources.getColor(R.color.black, mContext.theme))
                isBike = false
                try {
                    mSpeed = sharedPreferences.getString("setting_walk", resources.getString(R.string.setting_walk_default))?.toDouble() ?: 1.2
                } catch (e: NumberFormatException) {  // GOOD: The exception is caught.
                    mSpeed = 1.2
                }
            }
        }
        /* 默认为步行 */
        isWalk = true
        btnWalk.setColorFilter(resources.getColor(R.color.colorAccent, mContext.theme))
        /* 跑步按键的点击处理 */
        isRun = false
        btnRun = mJoystickLayout.findViewById(R.id.joystick_run)
        btnRun.setOnClickListener {
            if (!isRun) {
                btnRun.setColorFilter(resources.getColor(R.color.colorAccent, mContext.theme))
                isRun = true
                btnWalk.setColorFilter(resources.getColor(R.color.black, mContext.theme))
                isWalk = false
                btnBike.setColorFilter(resources.getColor(R.color.black, mContext.theme))
                isBike = false
                try {
                    mSpeed = sharedPreferences.getString("setting_run", resources.getString(R.string.setting_run_default))?.toDouble() ?: 3.6
                } catch (e: NumberFormatException) {  // GOOD: The exception is caught.
                    mSpeed = 3.6
                }
            }
        }
        /* 自行车按键的点击处理 */
        isBike = false
        btnBike = mJoystickLayout.findViewById(R.id.joystick_bike)
        btnBike.setOnClickListener {
            if (!isBike) {
                btnBike.setColorFilter(resources.getColor(R.color.colorAccent, mContext.theme))
                isBike = true
                btnWalk.setColorFilter(resources.getColor(R.color.black, mContext.theme))
                isWalk = false
                btnRun.setColorFilter(resources.getColor(R.color.black, mContext.theme))
                isRun = false
                try {
                    mSpeed = sharedPreferences.getString("setting_bike", resources.getString(R.string.setting_bike_default))?.toDouble() ?: 10.0
                } catch (e: NumberFormatException) {  // GOOD: The exception is caught.
                    mSpeed = 10.0
                }
            }
        }
        /* 方向键点击处理 */
        val rckView = mJoystickLayout.findViewById<RockerView>(R.id.joystick_rocker)
        rckView.setListener(object : RockerView.RockerViewClickListener {
            override fun clickAngleInfo(auto: Boolean, angle: Double, r: Double) {
                processDirection(auto, angle, r)
            }
        })

        /* 方向键点击处理 */
        val btnView = mJoystickLayout.findViewById<ButtonView>(R.id.joystick_button)
        btnView.setListener(object : ButtonView.ButtonViewClickListener {
            override fun clickAngleInfo(auto: Boolean, angle: Double, r: Double) {
                processDirection(auto, angle, r)
            }
        })

        /* 这里用来决定摇杆类型 */
        if (sharedPreferences.getString("setting_joystick_type", "0") == "0") {
            rckView.visibility = View.VISIBLE
            btnView.visibility = View.GONE
        } else {
            rckView.visibility = View.GONE
            btnView.visibility = View.VISIBLE
        }
    }

    private fun processDirection(auto: Boolean, angle: Double, r: Double) {
        if (r <= 0) {
            mTimer.cancel()
            isMove = false
        } else {
            mAngle = angle
            mR = r
            if (auto) {
                if (!isMove) {
                    mTimer.start()
                    isMove = true
                }
            } else {
                mTimer.cancel()
                isMove = false
                // 注意：这里的 x y 与 圆中角度的对应问题（以 X 轴正向为 0 度）且转换为 km
                disLng = mSpeed * (DivGo / 1000.0) * mR * cos(mAngle * 2.0 * Math.PI / 360) / 1000 // 注意安卓中的三角函数使用的是弧度
                disLat = mSpeed * (DivGo / 1000.0) * mR * sin(mAngle * 2.0 * Math.PI / 360) / 1000 // 注意安卓中的三角函数使用的是弧度
                mListener?.onMoveInfo(mSpeed, disLng, disLat, 90.0 - mAngle)
            }
        }
    }

    private inner class JoyStickOnTouchListener : OnTouchListener {
        private var x = 0
        private var y = 0

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = event.rawX.toInt()
                    y = event.rawY.toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    val nowX = event.rawX.toInt()
                    val nowY = event.rawY.toInt()
                    val movedX = nowX - x
                    val movedY = nowY - y
                    x = nowX
                    y = nowY

                    mWindowParamCurrent.x += movedX
                    mWindowParamCurrent.y += movedY
                    mWindowManager.updateViewLayout(view, mWindowParamCurrent)
                }
                MotionEvent.ACTION_UP -> view.performClick()
                else -> {
                }
            }
            return false
        }
    }

    interface JoyStickClickListener {
        fun onMoveInfo(speed: Double, disLng: Double, disLat: Double, angle: Double)
        fun onPositionInfo(lng: Double, lat: Double, alt: Double)
    }


    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun initJoyStickMapView() {
        mMapLayout = inflater.inflate(R.layout.joystick_map, null) as FrameLayout
        mMapLayout.setOnTouchListener(JoyStickOnTouchListener())

        mSearchList = mMapLayout.findViewById(R.id.map_search_list_view)
        mSearchLayout = mMapLayout.findViewById(R.id.map_search_linear)
        mSuggestionSearch = SuggestionSearch.newInstance()
        mSuggestionSearch.setOnGetSuggestionResultListener { suggestionResult ->
            if (suggestionResult?.allSuggestions == null) {
                GoUtils.DisplayToast(mContext, resources.getString(R.string.app_search_null))
            } else {
                val data: MutableList<Map<String, Any>> = ArrayList()
                val retCnt = suggestionResult.allSuggestions.size

                for (i in 0 until retCnt) {
                    if (suggestionResult.allSuggestions[i].pt == null) {
                        continue
                    }

                    val poiItem: MutableMap<String, Any> = HashMap()
                    poiItem[MainActivity.POI_NAME] = suggestionResult.allSuggestions[i].key
                    poiItem[MainActivity.POI_ADDRESS] = suggestionResult.allSuggestions[i].city + " " + suggestionResult.allSuggestions[i].district
                    poiItem[MainActivity.POI_LONGITUDE] = "" + suggestionResult.allSuggestions[i].pt.longitude
                    poiItem[MainActivity.POI_LATITUDE] = "" + suggestionResult.allSuggestions[i].pt.latitude
                    data.add(poiItem)
                }

                val simAdapt = SimpleAdapter(
                    mContext,
                    data,
                    R.layout.search_poi_item,
                    arrayOf(MainActivity.POI_NAME, MainActivity.POI_ADDRESS, MainActivity.POI_LONGITUDE, MainActivity.POI_LATITUDE), // 与下面数组元素要一一对应
                    intArrayOf(R.id.poi_name, R.id.poi_address, R.id.poi_longitude, R.id.poi_latitude)
                )
                mSearchList.adapter = simAdapt
                mSearchLayout.visibility = View.VISIBLE
            }
        }
        mSearchList.setOnItemClickListener { parent, view, position, id ->
            mSearchLayout.visibility = View.GONE

            val lng = (view.findViewById<View>(R.id.poi_longitude) as TextView).text.toString()
            val lat = (view.findViewById<View>(R.id.poi_latitude) as TextView).text.toString()
            markBaiduMap(LatLng(lat.toDouble(), lng.toDouble()))
        }

        val tips = mMapLayout.findViewById<TextView>(R.id.joystick_map_tips)
        val mSearchView = mMapLayout.findViewById<SearchView>(R.id.joystick_map_searchView)
        mSearchView.setOnSearchClickListener {
            tips.visibility = View.GONE

            // 特殊处理：这里让搜索框获取焦点，以显示输入法
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            mWindowManager.updateViewLayout(mMapLayout, mWindowParamCurrent)
        }
        mSearchView.setOnCloseListener {
            tips.visibility = View.VISIBLE
            mSearchLayout.visibility = View.GONE

            // 关闭时清除焦点
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            mWindowManager.updateViewLayout(mMapLayout, mWindowParamCurrent)

            false       /* 这里必须返回false，否则需要自行处理搜索框的折叠 */
        }
        mSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty()) {
                    try {
                        mSuggestionSearch.requestSuggestion(
                            SuggestionSearchOption()
                                .keyword(newText)
                                .city(MainActivity.mCurrentCity)
                        )
                    } catch (e: Exception) {
                        GoUtils.DisplayToast(mContext, resources.getString(R.string.app_error_search))
                        e.printStackTrace()
                    }
                } else {
                    mSearchLayout.visibility = View.GONE
                }

                return true
            }
        })

        val btnGo = mMapLayout.findViewById<ImageButton>(R.id.btnGo)
        btnGo.setOnClickListener {
            // 关闭时清除焦点
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            mWindowManager.updateViewLayout(mMapLayout, mWindowParamCurrent)

            tips.visibility = View.VISIBLE
            mSearchView.clearFocus()
            mSearchView.onActionViewCollapsed()

            if (mMarkMapLngLat == null) {
                GoUtils.DisplayToast(mContext, resources.getString(R.string.app_error_location))
            } else {
                if (mCurMapLngLat != mMarkMapLngLat) {
                    mCurMapLngLat = mMarkMapLngLat!!
                    mMarkMapLngLat = null

                    val lngLat = MapUtils.bd2wgs(mCurMapLngLat.longitude, mCurMapLngLat.latitude)
                    mListener?.onPositionInfo(lngLat[0], lngLat[1], mAltitude)

                    resetBaiduMap()

                    GoUtils.DisplayToast(mContext, resources.getString(R.string.app_location_ok))
                }
            }
        }
        btnGo.setColorFilter(resources.getColor(R.color.colorAccent, mContext.theme))

        val btnClose = mMapLayout.findViewById<ImageButton>(R.id.map_close)
        btnClose.setOnClickListener {
            // 关闭时清除焦点
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

            tips.visibility = View.VISIBLE
            mSearchLayout.visibility = View.GONE
            mSearchView.clearFocus()
            mSearchView.onActionViewCollapsed()

            mCurWin = WINDOW_TYPE_JOYSTICK
            show()
        }

        val btnBack = mMapLayout.findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { resetBaiduMap() }
        btnBack.setColorFilter(resources.getColor(R.color.colorAccent, mContext.theme))

        initBaiduMap()
    }

    private fun initBaiduMap() {
        mMapView = mMapLayout.findViewById(R.id.map_joystick)
        mMapView.showZoomControls(false)
        mBaiduMap = mMapView.map
        mBaiduMap.mapType = BaiduMap.MAP_TYPE_NORMAL
        mBaiduMap.isMyLocationEnabled = true

        mBaiduMap.setOnMapTouchListener { }

        mBaiduMap.setOnMapClickListener(object : BaiduMap.OnMapClickListener {
            /**
             * 单击地图
             */
            override fun onMapClick(point: LatLng) {
                markBaiduMap(point)
            }

            /**
             * 单击地图中的POI点
             */
            override fun onMapPoiClick(poi: MapPoi) {
                markBaiduMap(poi.position)
            }
        })

        mBaiduMap.setOnMapLongClickListener { point ->
            /**
             * 长按地图
             */
            markBaiduMap(point)
        }

        mBaiduMap.setOnMapDoubleClickListener { point ->
            /**
             * 双击地图
             */
            markBaiduMap(point)
        }
    }

    private fun resetBaiduMap() {
        if (!this::mBaiduMap.isInitialized) {
            XLog.e("JoyStick: mBaiduMap not initialized in resetBaiduMap")
            return
        }
        try {
            mBaiduMap.clear()

            val locData = MyLocationData.Builder()
                .latitude(mCurMapLngLat.latitude)
                .longitude(mCurMapLngLat.longitude)
                .build()
            mBaiduMap.setMyLocationData(locData)

            val builder = MapStatus.Builder()
            builder.target(mCurMapLngLat).zoom(18.0f)
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
        } catch (e: Exception) {
            XLog.e("JoyStick: Error in resetBaiduMap", e)
        }
    }

    private fun markBaiduMap(latLng: LatLng) {
        if (!this::mBaiduMap.isInitialized) {
            XLog.e("JoyStick: mBaiduMap not initialized in markBaiduMap")
            return
        }
        try {
            mMarkMapLngLat = latLng

            val ooA = MarkerOptions().position(latLng).icon(MainActivity.mMapIndicator)
            mBaiduMap.clear()
            mBaiduMap.addOverlay(ooA)

            val builder = MapStatus.Builder()
            builder.target(latLng).zoom(18.0f)
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
        } catch (e: Exception) {
             XLog.e("JoyStick: Error in markBaiduMap", e)
        }
    }


    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun initHistoryView() {
        mHistoryLayout = inflater.inflate(R.layout.joystick_history, null) as FrameLayout
        mHistoryLayout.setOnTouchListener(JoyStickOnTouchListener())

        val tips = mHistoryLayout.findViewById<TextView>(R.id.joystick_his_tips)
        val mSearchView = mHistoryLayout.findViewById<SearchView>(R.id.joystick_his_searchView)
        mSearchView.setOnSearchClickListener {
            tips.visibility = View.GONE

            // 特殊处理：这里让搜索框获取焦点，以显示输入法
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            mWindowManager.updateViewLayout(mHistoryLayout, mWindowParamCurrent)
        }
        mSearchView.setOnCloseListener {
            tips.visibility = View.VISIBLE

            // 关闭时清除焦点
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            mWindowManager.updateViewLayout(mHistoryLayout, mWindowParamCurrent)

            false       /* 这里必须返回false，否则需要自行处理搜索框的折叠 */
        }
        mSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {// 当点击搜索按钮时触发该方法
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {// 当搜索内容改变时触发该方法
                if (TextUtils.isEmpty(newText)) {
                    showHistory(mAllRecord)
                } else {
                    val searchRet: MutableList<Map<String, Any>> = ArrayList()
                    for (i in mAllRecord.indices) {
                        if (mAllRecord[i].toString().contains(newText!!)) {
                            searchRet.add(mAllRecord[i])
                        }
                    }

                    if (searchRet.size > 0) {
                        showHistory(searchRet)
                    } else {
                        GoUtils.DisplayToast(mContext, resources.getString(R.string.app_search_null))
                        showHistory(mAllRecord)
                    }
                }

                return false
            }
        })

        noRecordText = mHistoryLayout.findViewById(R.id.joystick_his_record_no_textview)
        mRecordListView = mHistoryLayout.findViewById(R.id.joystick_his_record_list_view)
        mRecordListView.setOnItemClickListener { adapterView, view, i, l ->
            // 关闭时清除焦点
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            mWindowManager.updateViewLayout(mHistoryLayout, mWindowParamCurrent)

            mSearchView.clearFocus()
            mSearchView.onActionViewCollapsed()
            tips.visibility = View.VISIBLE

            // wgs84坐标
            var wgs84LatLng = (view.findViewById<View>(R.id.WGSLatLngText) as TextView).text.toString()
            wgs84LatLng = wgs84LatLng.substring(wgs84LatLng.indexOf('[') + 1, wgs84LatLng.indexOf(']'))
            val wgs84latLngStr = wgs84LatLng.split(" ".toRegex()).toTypedArray()
            val wgs84Longitude = wgs84latLngStr[0].substring(wgs84latLngStr[0].indexOf(':') + 1)
            val wgs84Latitude = wgs84latLngStr[1].substring(wgs84latLngStr[1].indexOf(':') + 1)

            mListener?.onPositionInfo(wgs84Longitude.toDouble(), wgs84Latitude.toDouble(), mAltitude)

            // 注意这里在选择位置之后需要刷新地图
            var bdLatLng = (view.findViewById<View>(R.id.BDLatLngText) as TextView).text.toString()
            bdLatLng = bdLatLng.substring(bdLatLng.indexOf('[') + 1, bdLatLng.indexOf(']'))
            val bdLatLngStr = bdLatLng.split(" ".toRegex()).toTypedArray()
            val bdLongitude = bdLatLngStr[0].substring(bdLatLngStr[0].indexOf(':') + 1)
            val bdLatitude = bdLatLngStr[1].substring(bdLatLngStr[1].indexOf(':') + 1)
            mCurMapLngLat = LatLng(bdLatitude.toDouble(), bdLongitude.toDouble())

            GoUtils.DisplayToast(mContext, resources.getString(R.string.app_location_ok))
        }

        fetchAllRecord()

        showHistory(mAllRecord)

        val btnClose = mHistoryLayout.findViewById<ImageButton>(R.id.joystick_his_close)
        btnClose.setOnClickListener {
            // 关闭时清除焦点
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

            mSearchView.clearFocus()
            mSearchView.onActionViewCollapsed()
            tips.visibility = View.VISIBLE

            mCurWin = WINDOW_TYPE_JOYSTICK
            show()
        }
    }

    private fun fetchAllRecord() {
        val mHistoryLocationDB: SQLiteDatabase

        try {
            val hisLocDBHelper = DataBaseHistoryLocation(mContext.applicationContext)
            mHistoryLocationDB = hisLocDBHelper.writableDatabase

            val cursor = mHistoryLocationDB.query(
                DataBaseHistoryLocation.TABLE_NAME, null,
                DataBaseHistoryLocation.DB_COLUMN_ID + " > ?", arrayOf("0"),
                null, null, DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP + " DESC", null
            )

            while (cursor.moveToNext()) {
                val item: MutableMap<String, Any> = HashMap()
                val ID = cursor.getInt(0)
                val Location = cursor.getString(1)
                val Longitude = cursor.getString(2)
                val Latitude = cursor.getString(3)
                val TimeStamp = cursor.getInt(4).toLong()
                val BD09Longitude = cursor.getString(5)
                val BD09Latitude = cursor.getString(6)
                Log.d(
                    "TB",
                    ID.toString() + "\t" + Location + "\t" + Longitude + "\t" + Latitude + "\t" + TimeStamp + "\t" + BD09Longitude + "\t" + BD09Latitude
                )
                val bigDecimalLongitude = BigDecimal.valueOf(Longitude.toDouble())
                val bigDecimalLatitude = BigDecimal.valueOf(Latitude.toDouble())
                val bigDecimalBDLongitude = BigDecimal.valueOf(BD09Longitude.toDouble())
                val bigDecimalBDLatitude = BigDecimal.valueOf(BD09Latitude.toDouble())
                val doubleLongitude = bigDecimalLongitude.setScale(11, RoundingMode.HALF_UP).toDouble()
                val doubleLatitude = bigDecimalLatitude.setScale(11, RoundingMode.HALF_UP).toDouble()
                val doubleBDLongitude = bigDecimalBDLongitude.setScale(11, RoundingMode.HALF_UP).toDouble()
                val doubleBDLatitude = bigDecimalBDLatitude.setScale(11, RoundingMode.HALF_UP).toDouble()
                item[HistoryActivity.KEY_ID] = ID.toString()
                item[HistoryActivity.KEY_LOCATION] = Location
                item[HistoryActivity.KEY_TIME] = GoUtils.timeStamp2Date(TimeStamp.toString())
                item[HistoryActivity.KEY_LNG_LAT_WGS] = "[经度:$doubleLongitude 纬度:$doubleLatitude]"
                item[HistoryActivity.KEY_LNG_LAT_CUSTOM] = "[经度:$doubleBDLongitude 纬度:$doubleBDLatitude]"
                mAllRecord.add(item)
            }
            cursor.close()
            mHistoryLocationDB.close()
        } catch (e: Exception) {
            Log.e("JOYSTICK", "ERROR - fetchAllRecord")
        }
    }

    private fun showHistory(list: List<Map<String, Any>>) {
        if (list.isEmpty()) {
            mRecordListView.visibility = View.GONE
            noRecordText.visibility = View.VISIBLE
        } else {
            noRecordText.visibility = View.GONE
            mRecordListView.visibility = View.VISIBLE

            try {
                val simAdapt = SimpleAdapter(
                    mContext,
                    list,
                    R.layout.history_item,
                    arrayOf(HistoryActivity.KEY_ID, HistoryActivity.KEY_LOCATION, HistoryActivity.KEY_TIME, HistoryActivity.KEY_LNG_LAT_WGS, HistoryActivity.KEY_LNG_LAT_CUSTOM), // 与下面数组元素要一一对应
                    intArrayOf(R.id.LocationID, R.id.LocationText, R.id.TimeText, R.id.WGSLatLngText, R.id.BDLatLngText)
                )
                mRecordListView.adapter = simAdapt
            } catch (e: Exception) {
                Log.e("JOYSTICK", "ERROR - showHistory")
            }
        }
    }
}
