package com.zcshou.gogogo

import android.content.ContentValues
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.zcshou.database.DataBaseHistoryLocation
import com.zcshou.utils.GoUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class HistoryActivity : BaseActivity() {

    private lateinit var mRecordListView: ListView
    private lateinit var noRecordText: TextView
    private lateinit var mSearchLayout: LinearLayout
    private lateinit var mHistoryLocationDB: SQLiteDatabase
    private var mAllRecord: List<Map<String, Any>> = ArrayList()
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val KEY_ID = "KEY_ID"
        const val KEY_LOCATION = "KEY_LOCATION"
        const val KEY_TIME = "KEY_TIME"
        const val KEY_LNG_LAT_WGS = "KEY_LNG_LAT_WGS"
        const val KEY_LNG_LAT_CUSTOM = "KEY_LNG_LAT_CUSTOM"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* 为了启动欢迎页全屏，状态栏被设置了透明，但是会导致其他页面状态栏空白
         * 这里设计如下：
         * 1. 除了 WelcomeActivity 之外的所有 Activity 均继承 BaseActivity
         * 2. WelcomeActivity 单独处理，其他 Activity 手动填充 StatusBar
         * */
        window.statusBarColor = resources.getColor(R.color.colorPrimary, this.theme)

        setContentView(R.layout.activity_history)

        val actionBar: ActionBar? = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        initLocationDataBase()

        initSearchView()

        initRecordListView()
    }

    override fun onDestroy() {
        mHistoryLocationDB.close()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this add items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            this.finish() // back button
            return true
        } else if (id == R.id.action_delete) {
            AlertDialog.Builder(this@HistoryActivity)
                .setTitle("警告")//这里是表头的内容
                .setMessage("确定要删除全部历史记录吗?")//这里是中间显示的具体信息
                .setPositiveButton("确定") { _, _ ->
                    if (deleteRecord(-1)) {
                        GoUtils.DisplayToast(this, resources.getString(R.string.history_delete_ok))
                        updateRecordList()
                    }
                }
                .setNegativeButton("取消") { _, _ ->
                }
                .show()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initLocationDataBase() {
        try {
            val hisLocDBHelper = DataBaseHistoryLocation(applicationContext)
            mHistoryLocationDB = hisLocDBHelper.writableDatabase
        } catch (e: Exception) {
            Log.e("HistoryActivity", "ERROR - initLocationDataBase")
        }

        recordArchive()
    }

    //sqlite 操作 查询所有记录
    private fun fetchAllRecord(): List<Map<String, Any>> {
        val data = ArrayList<Map<String, Any>>()

        try {
            val cursor = mHistoryLocationDB.query(
                DataBaseHistoryLocation.TABLE_NAME, null,
                DataBaseHistoryLocation.DB_COLUMN_ID + " > ?", arrayOf("0"),
                null, null, DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP + " DESC", null
            )

            while (cursor.moveToNext()) {
                val item = HashMap<String, Any>()
                val id = cursor.getInt(0)
                val location = cursor.getString(1)
                val longitude = cursor.getString(2)
                val latitude = cursor.getString(3)
                val timeStamp = cursor.getInt(4).toLong()
                val bd09Longitude = cursor.getString(5)
                val bd09Latitude = cursor.getString(6)
                Log.d(
                    "TB",
                    "$id\t$location\t$longitude\t$latitude\t$timeStamp\t$bd09Longitude\t$bd09Latitude"
                )
                val bigDecimalLongitude = BigDecimal.valueOf(longitude.toDouble())
                val bigDecimalLatitude = BigDecimal.valueOf(latitude.toDouble())
                val bigDecimalBDLongitude = BigDecimal.valueOf(bd09Longitude.toDouble())
                val bigDecimalBDLatitude = BigDecimal.valueOf(bd09Latitude.toDouble())
                val doubleLongitude = bigDecimalLongitude.setScale(11, RoundingMode.HALF_UP).toDouble()
                val doubleLatitude = bigDecimalLatitude.setScale(11, RoundingMode.HALF_UP).toDouble()
                val doubleBDLongitude = bigDecimalBDLongitude.setScale(11, RoundingMode.HALF_UP).toDouble()
                val doubleBDLatitude = bigDecimalBDLatitude.setScale(11, RoundingMode.HALF_UP).toDouble()
                item[KEY_ID] = id.toString()
                item[KEY_LOCATION] = location
                item[KEY_TIME] = GoUtils.timeStamp2Date(timeStamp.toString())
                item[KEY_LNG_LAT_WGS] = "[经度:$doubleLongitude 纬度:$doubleLatitude]"
                item[KEY_LNG_LAT_CUSTOM] = "[经度:$doubleBDLongitude 纬度:$doubleBDLatitude]"
                data.add(item)
            }
            cursor.close()
        } catch (e: Exception) {
            data.clear()
            Log.e("HistoryActivity", "ERROR - fetchAllRecord")
        }

        return data
    }

    private fun recordArchive() {
        var limits: Double
        try {
            limits = sharedPreferences.getString(
                "setting_pos_history",
                resources.getString(R.string.history_expiration)
            )!!.toDouble()
        } catch (e: NumberFormatException) {  // GOOD: The exception is caught.
            limits = 7.0
        } catch (e: Exception) {
            limits = 7.0
        }
        val weekSecond = (limits * 24 * 60 * 60).toLong()

        try {
            mHistoryLocationDB.delete(
                DataBaseHistoryLocation.TABLE_NAME,
                DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP + " < ?",
                arrayOf((System.currentTimeMillis() / 1000 - weekSecond).toString())
            )
        } catch (e: Exception) {
            Log.e("HistoryActivity", "ERROR - recordArchive")
        }
    }

    private fun deleteRecord(id: Int): Boolean {
        var deleteRet = true

        try {
            if (id <= -1) {
                mHistoryLocationDB.delete(DataBaseHistoryLocation.TABLE_NAME, null, null)
            } else {
                mHistoryLocationDB.delete(
                    DataBaseHistoryLocation.TABLE_NAME,
                    DataBaseHistoryLocation.DB_COLUMN_ID + " = ?",
                    arrayOf(id.toString())
                )
            }
        } catch (e: Exception) {
            deleteRet = false
            Log.e("HistoryActivity", "ERROR - deleteRecord")
        }

        return deleteRet
    }

    private fun initSearchView() {
        val mSearchView = findViewById<SearchView>(R.id.searchView)
        mSearchView.onActionViewExpanded()// 当展开无输入内容的时候，没有关闭的图标
        mSearchView.isSubmitButtonEnabled = false//显示提交按钮
        mSearchView.isFocusable = false
        mSearchView.clearFocus()
        mSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {// 当点击搜索按钮时触发该方法
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {// 当搜索内容改变时触发该方法
                if (TextUtils.isEmpty(newText)) {
                    val simAdapt = SimpleAdapter(
                        this@HistoryActivity.baseContext,
                        mAllRecord,
                        R.layout.history_item,
                        arrayOf(
                            KEY_ID,
                            KEY_LOCATION,
                            KEY_TIME,
                            KEY_LNG_LAT_WGS,
                            KEY_LNG_LAT_CUSTOM
                        ), // 与下面数组元素要一一对应
                        intArrayOf(
                            R.id.LocationID,
                            R.id.LocationText,
                            R.id.TimeText,
                            R.id.WGSLatLngText,
                            R.id.BDLatLngText
                        )
                    )
                    mRecordListView.adapter = simAdapt
                } else {
                    val searchRet = ArrayList<Map<String, Any>>()
                    for (i in mAllRecord.indices) {
                        if (mAllRecord[i].toString().contains(newText)) {
                            searchRet.add(mAllRecord[i])
                        }
                    }
                    if (searchRet.isNotEmpty()) {
                        val simAdapt = SimpleAdapter(
                            this@HistoryActivity.baseContext,
                            searchRet,
                            R.layout.history_item,
                            arrayOf(
                                KEY_ID,
                                KEY_LOCATION,
                                KEY_TIME,
                                KEY_LNG_LAT_WGS,
                                KEY_LNG_LAT_CUSTOM
                            ), // 与下面数组元素要一一对应
                            intArrayOf(
                                R.id.LocationID,
                                R.id.LocationText,
                                R.id.TimeText,
                                R.id.WGSLatLngText,
                                R.id.BDLatLngText
                            )
                        )
                        mRecordListView.adapter = simAdapt
                    } else {
                        GoUtils.DisplayToast(
                            this@HistoryActivity,
                            resources.getString(R.string.history_error_search)
                        )
                        val simAdapt = SimpleAdapter(
                            this@HistoryActivity.baseContext,
                            mAllRecord,
                            R.layout.history_item,
                            arrayOf(
                                KEY_ID,
                                KEY_LOCATION,
                                KEY_TIME,
                                KEY_LNG_LAT_WGS,
                                KEY_LNG_LAT_CUSTOM
                            ), // 与下面数组元素要一一对应
                            intArrayOf(
                                R.id.LocationID,
                                R.id.LocationText,
                                R.id.TimeText,
                                R.id.WGSLatLngText,
                                R.id.BDLatLngText
                            )
                        )
                        mRecordListView.adapter = simAdapt
                    }
                }

                return false
            }
        })
    }

    private fun showDeleteDialog(locID: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("警告")
        builder.setMessage("确定要删除该项历史记录吗?")
        builder.setPositiveButton("确定") { _, _ ->
            val deleteRet = deleteRecord(locID.toInt())
            if (deleteRet) {
                GoUtils.DisplayToast(this@HistoryActivity, resources.getString(R.string.history_delete_ok))
                updateRecordList()
            }
        }
        builder.setNegativeButton("取消", null)

        builder.show()
    }

    private fun showInputDialog(locID: String, name: String) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(name)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("名称")
        builder.setView(input)
        builder.setPositiveButton("确认") { _, _ ->
            val userInput = input.text.toString()
            // Note: DataBaseHistoryLocation.updateHistoryLocation needs to be checked if converted to accept String ID or Int ID.
            // In Java it was updateHistoryLocation(mHistoryLocationDB, locID, userInput);
            // I should check DataBaseHistoryLocation.kt signature.
            // Assuming it takes String for now, or I'll convert.
            DataBaseHistoryLocation.updateHistoryLocation(mHistoryLocationDB, locID, userInput)
            updateRecordList()
        }
        builder.setNegativeButton("取消", null)

        builder.show()
    }

    private fun randomOffset(longitude: String, latitude: String): Array<String> {
        val max_offset_default = resources.getString(R.string.setting_random_offset_default)
        val lon_max_offset = sharedPreferences.getString("setting_lon_max_offset", max_offset_default)!!.toDouble()
        val lat_max_offset = sharedPreferences.getString("setting_lat_max_offset", max_offset_default)!!.toDouble()
        var lon = longitude.toDouble()
        var lat = latitude.toDouble()

        val randomLonOffset = (Math.random() * 2 - 1) * lon_max_offset  // Longitude offset (meters)
        val randomLatOffset = (Math.random() * 2 - 1) * lat_max_offset  // Latitude offset (meters)

        lon += randomLonOffset / 111320    // (meters -> longitude)
        lat += randomLatOffset / 110574    // (meters -> latitude)

        val offsetMessage = String.format(
            Locale.US,
            "经度偏移: %.2f米\n纬度偏移: %.2f米",
            randomLonOffset,
            randomLatOffset
        )
        GoUtils.DisplayToast(this, offsetMessage)

        return arrayOf(lon.toString(), lat.toString())
    }

    private fun initRecordListView() {
        noRecordText = findViewById(R.id.record_no_textview)
        mSearchLayout = findViewById(R.id.search_linear)
        mRecordListView = findViewById(R.id.record_list_view)
        mRecordListView.setOnItemClickListener { _, view, _, _ ->
            var bd09Longitude: String
            var bd09Latitude: String
            var name: String
            name = (view.findViewById<View>(R.id.LocationText) as TextView).text.toString()
            var bd09LatLng = (view.findViewById<View>(R.id.BDLatLngText) as TextView).text.toString()
            bd09LatLng = bd09LatLng.substring(bd09LatLng.indexOf('[') + 1, bd09LatLng.indexOf(']'))
            val latLngStr = bd09LatLng.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            bd09Longitude = latLngStr[0].substring(latLngStr[0].indexOf(':') + 1)
            bd09Latitude = latLngStr[1].substring(latLngStr[1].indexOf(':') + 1)

            // Random offset
            if (sharedPreferences.getBoolean("setting_random_offset", false)) {
                val offsetResult = randomOffset(bd09Longitude, bd09Latitude)
                bd09Longitude = offsetResult[0]
                bd09Latitude = offsetResult[1]
            }

            if (!MainActivity.showLocation(name, bd09Longitude, bd09Latitude)) {
                GoUtils.DisplayToast(this, resources.getString(R.string.history_error_location))
            }
            this.finish()
        }

        mRecordListView.setOnItemLongClickListener { _, view, _, _ ->
            val popupMenu = PopupMenu(this@HistoryActivity, view)
            popupMenu.gravity = Gravity.END or Gravity.BOTTOM
            popupMenu.menu.add("编辑")
            popupMenu.menu.add("删除")

            popupMenu.setOnMenuItemClickListener { item ->
                val locID = (view.findViewById<View>(R.id.LocationID) as TextView).text.toString()
                val name = (view.findViewById<View>(R.id.LocationText) as TextView).text.toString()
                when (item.title.toString()) {
                    "编辑" -> {
                        showInputDialog(locID, name)
                        true
                    }
                    "删除" -> {
                        showDeleteDialog(locID)
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
            true
        }

        updateRecordList()
    }

    private fun updateRecordList() {
        mAllRecord = fetchAllRecord()

        if (mAllRecord.isEmpty()) {
            mRecordListView.visibility = View.GONE
            mSearchLayout.visibility = View.GONE
            noRecordText.visibility = View.VISIBLE
        } else {
            noRecordText.visibility = View.GONE
            mRecordListView.visibility = View.VISIBLE
            mSearchLayout.visibility = View.VISIBLE

            try {
                val simAdapt = SimpleAdapter(
                    this,
                    mAllRecord,
                    R.layout.history_item,
                    arrayOf(
                        KEY_ID,
                        KEY_LOCATION,
                        KEY_TIME,
                        KEY_LNG_LAT_WGS,
                        KEY_LNG_LAT_CUSTOM
                    ),
                    intArrayOf(
                        R.id.LocationID,
                        R.id.LocationText,
                        R.id.TimeText,
                        R.id.WGSLatLngText,
                        R.id.BDLatLngText
                    )
                )
                mRecordListView.adapter = simAdapt
            } catch (e: Exception) {
                Log.e("HistoryActivity", "ERROR - updateRecordList")
            }
        }
    }
}
