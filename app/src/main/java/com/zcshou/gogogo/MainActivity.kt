package com.zcshou.gogogo

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.core.SearchResult
import com.baidu.mapapi.search.geocode.*
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener
import com.baidu.mapapi.search.sug.SuggestionResult
import com.baidu.mapapi.search.sug.SuggestionSearch
import com.baidu.mapapi.search.sug.SuggestionSearchOption
import com.elvishew.xlog.XLog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.zcshou.database.DataBaseHistoryLocation
import com.zcshou.database.DataBaseHistorySearch
import com.zcshou.service.ServiceGo
import com.zcshou.utils.GoUtils
import com.zcshou.utils.MapUtils
import com.zcshou.utils.ShareUtils
import io.noties.markwon.Markwon
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.math.abs

class MainActivity : BaseActivity(), SensorEventListener, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mOkHttpClient: OkHttpClient
    private lateinit var sharedPreferences: SharedPreferences

    /*============================== 主界面地图 相关 ==============================*/
    /************** 地图 *****************/
    private var mMapView: MapView? = null
    private var mGeoCoder: GeoCoder? = null
    private var mSensorManager: SensorManager? = null
    private var mSensorAccelerometer: Sensor? = null
    private var mSensorMagnetic: Sensor? = null
    private val mAccValues = FloatArray(3) //加速度传感器数据
    private val mMagValues = FloatArray(3) //地磁传感器数据
    private val mR = FloatArray(9) //旋转矩阵，用来保存磁场和加速度的数据
    private val mDirectionValues = FloatArray(3) //模拟方向传感器的数据（原始数据为弧度）

    /************** 定位 *****************/
    private var mLocClient: LocationClient? = null
    private var mCurrentLat = 0.0       // 当前位置的百度纬度
    private var mCurrentLon = 0.0       // 当前位置的百度经度
    private var mCurrentDirection = 0.0f
    private var isFirstLoc = true // 是否首次定位
    private var isMockServStart = false
    private var mServiceBinder: ServiceGo.ServiceGoBinder? = null
    private var mConnection: ServiceConnection? = null
    private var mButtonStart: FloatingActionButton? = null

    /*============================== 历史记录 相关 ==============================*/
    private var mLocationHistoryDB: SQLiteDatabase? = null
    private var mSearchHistoryDB: SQLiteDatabase? = null

    /*============================== SearchView 相关 ==============================*/
    private var searchView: SearchView? = null
    private var mSearchList: ListView? = null
    private var mSearchLayout: LinearLayout? = null
    private var mSearchHistoryList: ListView? = null
    private var mHistoryLayout: LinearLayout? = null
    private var searchItem: MenuItem? = null
    private var mSuggestionSearch: SuggestionSearch? = null

    /*============================== 更新 相关 ==============================*/
    private var mDownloadManager: DownloadManager? = null
    private var mDownloadId: Long = 0
    private var mDownloadBdRcv: BroadcastReceiver? = null
    private var mUpdateFilename: String? = null

    companion object {
        /* 对外 */
        const val LAT_MSG_ID = "LAT_VALUE"
        const val LNG_MSG_ID = "LNG_VALUE"
        const val ALT_MSG_ID = "ALT_VALUE"

        const val POI_NAME = "POI_NAME"
        const val POI_ADDRESS = "POI_ADDRESS"
        const val POI_LONGITUDE = "POI_LONGITUDE"
        const val POI_LATITUDE = "POI_LATITUDE"

        // 使用 lazy 加载或者在使用前判空，防止静态初始化崩溃
        val mMapIndicator: BitmapDescriptor by lazy { BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding) }
        var mCurrentCity: String? = null
        var mBaiduMap: BaiduMap? = null
        var mMarkLatLngMap = LatLng(36.547743718042415, 117.07018449827267) // 当前标记的地图点
        var mMarkName: String? = null

        fun showLocation(name: String, longitude: String, latitude: String): Boolean {
            try {
                if (mBaiduMap == null) return false

                mMarkName = name
                mMarkLatLngMap = LatLng(latitude.toDouble(), longitude.toDouble())

                // 定义Maker坐标点
                // 构建MarkerOption，用于在地图上添加Marker
                val option = MarkerOptions()
                    .position(mMarkLatLngMap)
                    .icon(mMapIndicator)
                    .zIndex(9)
                    .draggable(true)
                // 在地图上添加Marker，并显示
                mBaiduMap?.clear()
                mBaiduMap?.addOverlay(option)

                // 移动地图
                val u = MapStatusUpdateFactory.newLatLng(mMarkLatLngMap)
                mBaiduMap?.animateMapStatus(u)

                return true
            } catch (e: Exception) {
                return false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START)
                } else {
                    if (isMockServStart) {
                        val intent = Intent(Intent.ACTION_MAIN)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        intent.addCategory(Intent.CATEGORY_HOME)
                        startActivity(intent)
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })

        XLog.i("MainActivity: onCreate")

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mOkHttpClient = OkHttpClient()

        initNavigationView()

        initMap()

        initMapLocation()

        initMapButton()

        initGoBtn()

        mConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mServiceBinder = service as ServiceGo.ServiceGoBinder
            }

            override fun onServiceDisconnected(name: ComponentName) {
                mServiceBinder = null
            }
        }

        /* 传感器 */
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensorAccelerometer = mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensorMagnetic = mSensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        mSensorManager?.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_UI)
        mSensorManager?.registerListener(this, mSensorMagnetic, SensorManager.SENSOR_DELAY_UI)

        initLocationDataBase()
        initSearchDataBase()
        initSearchView()

        /* 检查更新 */
        if (sharedPreferences.getBoolean("setting_check_update", true)) {
            checkUpdate(true)
        }
    }

    override fun onResume() {
        super.onResume()
        mMapView?.onResume()

        mSensorManager?.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_UI)
        mSensorManager?.registerListener(this, mSensorMagnetic, SensorManager.SENSOR_DELAY_UI)

        /* 检查是否在模拟中 */
        isMockServStart = GoUtils.isServiceRunning(this, ServiceGo::class.java.name)
        if (isMockServStart) {
            mButtonStart?.setImageResource(R.drawable.ic_stop_black_24dp)
            // Bind service if running
            val serviceIntent = Intent(this, ServiceGo::class.java)
            bindService(serviceIntent, mConnection!!, Context.BIND_AUTO_CREATE)
        } else {
            mButtonStart?.setImageResource(R.drawable.ic_play_arrow_black_24dp)
        }

        /* 检查是否开启了位置模拟 */
        if (!GoUtils.isAllowMockLocation(this)) {
            GoUtils.DisplayToast(this, "请在开发者选项中开启模拟位置权限！")
        }
    }

    override fun onPause() {
        super.onPause()
        mMapView?.onPause()
        mSensorManager?.unregisterListener(this)
        if (mConnection != null && isMockServStart) {
            try {
                unbindService(mConnection!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        mLocClient?.stop()
        mBaiduMap?.isMyLocationEnabled = false
        mMapView?.onDestroy()
        mMapView = null
        mBaiduMap = null
        mGeoCoder?.destroy()
        mSuggestionSearch?.destroy()
        mLocationHistoryDB?.close()
        mSearchHistoryDB?.close()
        if (mDownloadBdRcv != null) {
            unregisterReceiver(mDownloadBdRcv)
        }
        super.onDestroy()
    }

    /*
    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            if (isMockServStart) {
                // moveTaskToBack(true); // Original comment
                val intent = Intent(Intent.ACTION_MAIN)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.addCategory(Intent.CATEGORY_HOME)
                startActivity(intent)
            } else {
                super.onBackPressed()
            }
        }
    }
    */

    /*============================== 传感器 Listener ==============================*/
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccValues, 0, 3)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagValues, 0, 3)
        }

        // 只有当坐标有效时才更新 MyLocationData，防止跳到 (0,0)
        // 4.9E-324 是百度地图的无效值常量
        if (Math.abs(mCurrentLat) < 0.000001 && Math.abs(mCurrentLon) < 0.000001) {
            return
        }
        if (mCurrentLat == 4.9E-324 || mCurrentLon == 4.9E-324) {
            return
        }

        SensorManager.getRotationMatrix(mR, null, mAccValues, mMagValues)
        SensorManager.getOrientation(mR, mDirectionValues)

        // 弧度转角度
        val mValue = ((360 + Math.toDegrees(mDirectionValues[0].toDouble())) % 360).toFloat()
        val locData = MyLocationData.Builder()
            .accuracy(0f)
            .direction(mValue) // 此处设置开发者获取到的方向信息，顺时针0-360
            .latitude(mCurrentLat)
            .longitude(mCurrentLon)
            .build()
        mBaiduMap?.setMyLocationData(locData)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /*============================== UI初始化 ==============================*/
    private fun initNavigationView() {
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val headerView = navigationView.getHeaderView(0)
        val imageButton = headerView.findViewById<View>(R.id.app_icon)
        imageButton.setOnClickListener {
            val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START)
            }
        }
    }

    private fun initMap() {
        mMapView = findViewById(R.id.bdMapView)
        mBaiduMap = mMapView?.map

        // 开启定位图层
        mBaiduMap?.isMyLocationEnabled = true

        /* 设置定位图层配置信息，只有先允许定位图层后设置定位图层配置信息才会生效
         * customMarker 用户自定义定位图标
         * enableDirection 是否允许显示方向信息
         * locationMode 定位图层显示方式
         * */
        mBaiduMap?.setMyLocationConfiguration(
            MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.NORMAL, true, null
            )
        )

        /* 地图状态改变监听 */
        mBaiduMap?.setOnMapStatusChangeListener(object : BaiduMap.OnMapStatusChangeListener {
            override fun onMapStatusChangeStart(mapStatus: MapStatus) {}
            override fun onMapStatusChangeStart(mapStatus: MapStatus, i: Int) {}
            override fun onMapStatusChange(mapStatus: MapStatus) {}
            override fun onMapStatusChangeFinish(mapStatus: MapStatus) {
                // 移动结束后，更新中心点
                mMarkLatLngMap = mapStatus.target
            }
        })

        /* 地图点击监听 */
        mBaiduMap?.setOnMapClickListener(object : BaiduMap.OnMapClickListener {
            override fun onMapClick(latLng: LatLng) {
                mBaiduMap?.clear()

                // 定义Maker坐标点
                mMarkLatLngMap = latLng

                // 构建MarkerOption，用于在地图上添加Marker
                val option = MarkerOptions()
                    .position(latLng)
                    .icon(mMapIndicator)
                    .zIndex(9)
                    .draggable(true)

                // 在地图上添加Marker，并显示
                mBaiduMap?.addOverlay(option)
            }

            override fun onMapPoiClick(mapPoi: MapPoi) {
                mBaiduMap?.clear()
                mMarkLatLngMap = mapPoi.position
                mMarkName = mapPoi.name
                val option = MarkerOptions()
                    .position(mMarkLatLngMap)
                    .icon(mMapIndicator)
                    .zIndex(9)
                    .draggable(true)
                mBaiduMap?.addOverlay(option)
                Toast.makeText(this@MainActivity, mapPoi.name, Toast.LENGTH_SHORT).show()
            }
        })

        /* Marker 拖拽监听 */
        mBaiduMap?.setOnMarkerDragListener(object : BaiduMap.OnMarkerDragListener {
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragEnd(marker: Marker) {
                mMarkLatLngMap = marker.position
            }
            override fun onMarkerDragStart(marker: Marker) {}
        })

        /* 反编码监听 */
        val listener: OnGetGeoCoderResultListener = object : OnGetGeoCoderResultListener {
            override fun onGetGeoCodeResult(geoCodeResult: GeoCodeResult) {}

            override fun onGetReverseGeoCodeResult(reverseGeoCodeResult: ReverseGeoCodeResult?) {
                if (reverseGeoCodeResult == null || reverseGeoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
                    Toast.makeText(this@MainActivity, "抱歉，未能找到结果", Toast.LENGTH_LONG).show()
                    return
                }

                mBaiduMap?.clear()
                mMarkLatLngMap = reverseGeoCodeResult.location
                mMarkName = reverseGeoCodeResult.address
                val option = MarkerOptions()
                    .position(mMarkLatLngMap)
                    .icon(mMapIndicator)
                    .zIndex(9)
                    .draggable(true)
                mBaiduMap?.addOverlay(option)
                mBaiduMap?.setMapStatus(MapStatusUpdateFactory.newLatLng(mMarkLatLngMap))
                mBaiduMap?.showInfoWindow(
                    InfoWindow(
                        Button(this@MainActivity).apply {
                            text = reverseGeoCodeResult.address
                            setOnClickListener { mBaiduMap?.hideInfoWindow() }
                        },
                        mMarkLatLngMap,
                        -47
                    )
                )

                /* 保存历史记录 */
                val bd09Lng = mMarkLatLngMap.longitude.toString()
                val bd09Lat = mMarkLatLngMap.latitude.toString()
                val wgs84 = MapUtils.bd2wgs(mMarkLatLngMap.longitude, mMarkLatLngMap.latitude)
                DataBaseHistoryLocation.addHistoryLocation(
                    mLocationHistoryDB,
                    mMarkName ?: "Unknown",
                    wgs84[0].toString(),
                    wgs84[1].toString(),
                    (System.currentTimeMillis() / 1000).toString(),
                    bd09Lng,
                    bd09Lat
                )
            }
        }
        mGeoCoder = GeoCoder.newInstance()
        mGeoCoder?.setOnGetGeoCodeResultListener(listener)
    }

    private fun initMapLocation() {
        mBaiduMap?.isMyLocationEnabled = true
        mLocClient = LocationClient(this)
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
                mCurrentCity = location.city

                val locData = MyLocationData.Builder()
                    .accuracy(location.radius)
                    .direction(mCurrentDirection)
                    .latitude(location.latitude)
                    .longitude(location.longitude)
                    .build()
                mBaiduMap?.setMyLocationData(locData)

                if (isFirstLoc) {
                    isFirstLoc = false
                    val ll = LatLng(location.latitude, location.longitude)
                    val u = MapStatusUpdateFactory.newLatLng(ll)
                    mBaiduMap?.animateMapStatus(u)

                    /* 只有第一次定位时才更新 mMarkLatLngMap */
                    mMarkLatLngMap = ll
                }
            }
        })
        val option = LocationClientOption()
        option.isOpenGps = true
        option.setCoorType("bd09ll")
        option.setScanSpan(1000)
        mLocClient?.locOption = option
        mLocClient?.start()
    }

    private fun initMapButton() {
        /* 缩放按钮 */
        val btnZoomIn = findViewById<ImageButton>(R.id.zoom_in)
        val btnZoomOut = findViewById<ImageButton>(R.id.zoom_out)
        btnZoomIn?.setOnClickListener {
            val zoom = mBaiduMap?.mapStatus?.zoom ?: return@setOnClickListener
            if (zoom <= 21) {
                mBaiduMap?.setMapStatus(MapStatusUpdateFactory.zoomTo(zoom + 1))
            }
        }
        btnZoomOut?.setOnClickListener {
            val zoom = mBaiduMap?.mapStatus?.zoom ?: return@setOnClickListener
            if (zoom >= 4) {
                mBaiduMap?.setMapStatus(MapStatusUpdateFactory.zoomTo(zoom - 1))
            }
        }

        /* 卫星图 */
        val radioGroup = findViewById<RadioGroup>(R.id.RadioGroupMapType)
        radioGroup?.setOnCheckedChangeListener { _, checkedId ->
            if (mBaiduMap == null) return@setOnCheckedChangeListener
            try {
                if (checkedId == R.id.mapSatellite) {
                    mBaiduMap?.mapType = BaiduMap.MAP_TYPE_SATELLITE
                } else {
                    mBaiduMap?.mapType = BaiduMap.MAP_TYPE_NORMAL
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /* 路况图 - 布局中未找到对应按钮，暂不实现 */
        // val btnTraffic = findViewById<Button>(R.id.btn_traffic)
        // btnTraffic.setOnClickListener {
        //     if (mBaiduMap?.isTrafficEnabled == true) {
        //         mBaiduMap?.isTrafficEnabled = false
        //         btnTraffic.text = "路况(关)"
        //     } else {
        //         mBaiduMap?.isTrafficEnabled = true
        //         btnTraffic.text = "路况(开)"
        //     }
        // }

        /* 定位 */
        val btnLocation = findViewById<ImageButton>(R.id.cur_position)
        btnLocation.setOnClickListener {
            val ll = LatLng(mCurrentLat, mCurrentLon)
            val u = MapStatusUpdateFactory.newLatLng(ll)
            mBaiduMap?.animateMapStatus(u)
        }
    }

    private fun initGoBtn() {
        mButtonStart = findViewById(R.id.faBtnStart)
        XLog.i("initGoBtn: mButtonStart = $mButtonStart")
        mButtonStart?.setOnClickListener {
            XLog.i("mButtonStart clicked")
            if (Build.VERSION.SDK_INT >= 23) {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    XLog.i("Overlay permission granted, calling doGoLocation")
                    doGoLocation()
                } else {
                    XLog.i("Overlay permission missing, requesting...")
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    intent.data = Uri.parse("package:$packageName") // 直接跳转到本应用的设置页
                    GoUtils.DisplayToast(this@MainActivity, "需要悬浮窗权限")
                    startActivity(intent)
                }
            } else {
                doGoLocation()
            }
        }
    }

    private fun doGoLocation() {
        XLog.i("doGoLocation called")
        if (!GoUtils.isAllowMockLocation(this)) {
            XLog.i("Mock location permission NOT granted")
            GoUtils.DisplayToast(this, "请在开发者选项中开启模拟位置权限！")
            return
        }

        val lm = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val isGpsEnable = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        if (!isGpsEnable) {
            XLog.i("GPS NOT enabled")
            GoUtils.DisplayToast(this, "请打开GPS")
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
            return
        }

        if (isMockServStart) {
            XLog.i("Stopping Mock Service...")
            val intent = Intent(this, ServiceGo::class.java)
            try {
                if (mConnection != null) {
                    unbindService(mConnection!!)
                }
            } catch (e: Exception) {
                XLog.e("Error unbinding service", e)
            }
            stopService(intent)
            mButtonStart?.setImageResource(R.drawable.ic_play_arrow_black_24dp)
            isMockServStart = false
        } else {
            XLog.i("Starting Mock Service...")
            val intent = Intent(this, ServiceGo::class.java)
            // 传递坐标信息
            intent.putExtra(LAT_MSG_ID, mMarkLatLngMap.latitude)
            intent.putExtra(LNG_MSG_ID, mMarkLatLngMap.longitude)
            XLog.i("Putting extras: lat=${mMarkLatLngMap.latitude}, lng=${mMarkLatLngMap.longitude}")

            // 8.0 之后需要 startForegroundService
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            bindService(intent, mConnection!!, Context.BIND_AUTO_CREATE)
            mButtonStart?.setImageResource(R.drawable.ic_stop_black_24dp)
            isMockServStart = true
        }
    }

    /*============================== SQLite 相关 ==============================*/
    private fun initLocationDataBase() {
        try {
            val hisLocDBHelper = DataBaseHistoryLocation(applicationContext)
            mLocationHistoryDB = hisLocDBHelper.writableDatabase
        } catch (e: Exception) {
            XLog.e("MainActivity", "ERROR - initLocationDataBase")
        }
    }

    private fun initSearchDataBase() {
        try {
            val hisSearchDBHelper = DataBaseHistorySearch(applicationContext)
            mSearchHistoryDB = hisSearchDBHelper.writableDatabase
        } catch (e: Exception) {
            XLog.e("MainActivity", "ERROR - initSearchDataBase")
        }
    }

    /*============================== Search 相关 ==============================*/
    private fun initSearchView() {
        mSearchLayout = findViewById(R.id.search_linear)
        mSearchList = findViewById(R.id.search_list_view)
        mHistoryLayout = findViewById(R.id.search_history_linear)
        mSearchHistoryList = findViewById(R.id.search_history_list_view)
        mSuggestionSearch = SuggestionSearch.newInstance()

        mSuggestionSearch?.setOnGetSuggestionResultListener { suggestionResult ->
            if (suggestionResult == null || suggestionResult.allSuggestions == null) {
                return@setOnGetSuggestionResultListener
            }

            val data = ArrayList<Map<String, Any>>()
            for (info in suggestionResult.allSuggestions) {
                if (info.key != null) {
                    val item = HashMap<String, Any>()
                    item[POI_NAME] = info.key
                    item[POI_ADDRESS] = if (info.city != null && info.district != null) info.city + info.district else ""
                    if (info.pt != null) {
                        item[POI_LATITUDE] = info.pt.latitude
                        item[POI_LONGITUDE] = info.pt.longitude
                        data.add(item)
                    }
                }
            }

            val simpleAdapter = SimpleAdapter(
                this@MainActivity,
                data,
                R.layout.search_poi_item,
                arrayOf(POI_NAME, POI_ADDRESS),
                intArrayOf(R.id.poi_name, R.id.poi_address)
            )
            mSearchList?.adapter = simpleAdapter
            simpleAdapter.notifyDataSetChanged()
        }

        mSearchList?.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val map = parent.adapter.getItem(position) as HashMap<*, *>
            mMarkName = map[POI_NAME] as String
            mMarkLatLngMap = LatLng(
                (map[POI_LATITUDE] as Double),
                (map[POI_LONGITUDE] as Double)
            )
            searchView?.setQuery(mMarkName, true)

            /* 隐藏搜索框 */
            mSearchLayout?.visibility = View.GONE
            mHistoryLayout?.visibility = View.GONE
            searchItem?.collapseActionView()
        }
    }

    private fun showSearchHistory() {
        val data = ArrayList<Map<String, Any>>()
        val cursor = mSearchHistoryDB?.query(
            DataBaseHistorySearch.TABLE_NAME, null, null, null, null, null,
            DataBaseHistorySearch.DB_COLUMN_TIMESTAMP + " DESC", null
        )

        cursor?.let {
            while (it.moveToNext()) {
                val item = HashMap<String, Any>()
                val id = it.getInt(0)
                val text = it.getString(1)
                item["id"] = id
                item["text"] = text
                data.add(item)
            }
            it.close()
        }

        val adapter = SimpleAdapter(
            this,
            data,
            R.layout.search_history_item,
            arrayOf("text"),
            intArrayOf(R.id.HistoryText)
        )
        mSearchHistoryList?.adapter = adapter

        mSearchHistoryList?.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val map = parent.adapter.getItem(position) as HashMap<*, *>
            val text = map["text"] as String
            searchView?.setQuery(text, true)
        }

        val closeBtnId = resources.getIdentifier("search_close_btn", "id", "androidx.appcompat")
        val closeButton = searchView?.findViewById<ImageView>(closeBtnId)
        closeButton?.setOnClickListener {
            val searchSrcTextId = resources.getIdentifier("search_src_text", "id", "androidx.appcompat")
            val et = findViewById<EditText>(searchSrcTextId)
            et?.setText("")
            searchView?.setQuery("", false)
            mSearchLayout?.visibility = View.INVISIBLE
            mHistoryLayout?.visibility = View.VISIBLE
        }
    }

    /*============================== Menu 相关 ==============================*/
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem?.actionView as SearchView?

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isNotEmpty()) {
                    /* Geo搜索 */
                    mGeoCoder?.geocode(
                        GeoCodeOption().city(mCurrentCity ?: "").address(query)
                    )
                    /* 保存搜索历史 */
                    DataBaseHistorySearch.addHistorySearch(mSearchHistoryDB, query)
                }
                mSearchLayout?.visibility = View.GONE
                mHistoryLayout?.visibility = View.GONE
                searchItem?.collapseActionView()
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    mSearchLayout?.visibility = View.GONE
                    mHistoryLayout?.visibility = View.VISIBLE
                    showSearchHistory()
                } else {
                    mSearchLayout?.visibility = View.VISIBLE
                    mHistoryLayout?.visibility = View.GONE
                    mSuggestionSearch?.requestSuggestion(
                        SuggestionSearchOption().city(mCurrentCity ?: "").keyword(newText)
                    )
                }
                return false
            }
        })

        searchView?.setOnSearchClickListener {
            mHistoryLayout?.visibility = View.VISIBLE
            showSearchHistory()
        }
        
        // Handle search view close/collapse to hide lists
        searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
             override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                 return true
             }

             override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                 mSearchLayout?.visibility = View.GONE
                 mHistoryLayout?.visibility = View.GONE
                 return true
             }
        })

        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.nav_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_history -> {
                val intent = Intent(this, HistoryActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_dev -> {
                if (!GoUtils.isDeveloperOptionsEnabled(this)) {
                    GoUtils.DisplayToast(this, resources.getString(R.string.app_error_dev))
                } else {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                        startActivity(intent)
                    } catch (e: Exception) {
                        GoUtils.DisplayToast(this, resources.getString(R.string.app_error_dev))
                    }
                }
            }
            R.id.nav_update -> {
                checkUpdate(true)
            }
            R.id.nav_feedback -> {
                val file = File(getExternalFilesDir("Logs"), GoApplication.LOG_FILE_NAME)
                ShareUtils.shareFile(this, file, item.title.toString())
            }
            R.id.nav_contact -> {
                val uri = Uri.parse("https://gitee.com/itexp/gogogo/issues")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    /*============================== Update 相关 ==============================*/
    private fun checkUpdate(isAuto: Boolean) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/zcshou/GoGoGo/releases/latest")
            .build()
        val call = mOkHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!isAuto) {
                    runOnUiThread {
                        GoUtils.DisplayToast(this@MainActivity, "检查更新失败！")
                    }
                }
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val res = response.body?.string() ?: return
                try {
                    val jsonObject = JSONObject(res)
                    val tag_name = jsonObject.getString("tag_name")
                    val body = jsonObject.getString("body")
                    val assets = jsonObject.getJSONArray("assets")
                    if (assets.length() > 0) {
                        val asset = assets.getJSONObject(0)
                        val browser_download_url = asset.getString("browser_download_url")
                        mUpdateFilename = asset.getString("name")

                        val version_new = tag_name.replace("v", "").replace(".", "").toInt()
                        val version_old = GoUtils.getVersionCode(this@MainActivity)

                        if (version_new > version_old) {
                            runOnUiThread {
                                val markwon = Markwon.create(this@MainActivity)
                                val builder = AlertDialog.Builder(this@MainActivity)
                                builder.setTitle("发现新版本: $tag_name")
                                val tvContent = TextView(this@MainActivity)
                                markwon.setMarkdown(tvContent, body)
                                tvContent.setPadding(40, 20, 40, 20)
                                builder.setView(tvContent)
                                builder.setPositiveButton("下载") { _, _ ->
                                    downloadApk(browser_download_url)
                                }
                                builder.setNegativeButton("取消", null)
                                builder.show()
                            }
                        } else {
                            if (!isAuto) {
                                runOnUiThread {
                                    GoUtils.DisplayToast(this@MainActivity, "当前已是最新版本！")
                                }
                            }
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        })
    }

    private fun downloadApk(url: String) {
        mDownloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(
            android.os.Environment.DIRECTORY_DOWNLOADS,
            mUpdateFilename
        )
        mDownloadId = mDownloadManager!!.enqueue(request)

        mDownloadBdRcv = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    installApk()
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                mDownloadBdRcv,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(
                mDownloadBdRcv,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    private fun installApk() {
        val file = File(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            mUpdateFilename ?: ""
        )
        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (Build.VERSION.SDK_INT >= 24) {
            val apkUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                file
            )
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
        }
        startActivity(intent)
    }
}
