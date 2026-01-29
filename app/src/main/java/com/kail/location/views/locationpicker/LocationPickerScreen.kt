package com.kail.location.views.locationpicker

import com.kail.location.views.locationpicker.LocationPickerActivity

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.baidu.mapapi.map.MapView
import com.kail.location.R
import com.kail.location.viewmodels.LocationPickerViewModel.PoiInfo
import com.kail.location.viewmodels.LocationPickerViewModel.UpdateInfo
import com.kail.location.viewmodels.LocationPickerViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ListItem
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import android.widget.ImageView
import com.kail.location.views.common.DrawerHeader
import com.kail.location.views.common.PoiDetailCard
import com.baidu.mapapi.map.BaiduMap


/**
 * 位置选择屏幕 Composable
 * 整合了地图视图、抽屉导航、浮动按钮、缩放控件以及 POI 信息卡片。
 *
 * @param mapView 百度地图 View 实例
 * @param isMocking 是否正在模拟位置
 * @param onToggleMock 切换模拟状态的回调
 * @param onZoomIn 放大地图回调
 * @param onZoomOut 缩小地图回调
 * @param onLocate 定位到当前位置回调
 * @param onLocationInputConfirm 输入经纬度确认回调
 * @param onMapTypeChange 切换地图类型回调
 * @param onNavigate 导航菜单点击回调
 * @param appVersion 应用版本号
 * @param selectedPoi 当前选中的 POI 信息
 * @param onPoiClose 关闭 POI 卡片回调
 * @param onPoiSave 保存 POI 回调
 * @param onPoiCopy 复制 POI 信息回调
 * @param onPoiShare 分享 POI 信息回调
 * @param onPoiFly "飞行"（模拟位置）到 POI 回调
 * @param updateInfo 更新信息（若有）
 * @param onUpdateDismiss 关闭更新对话框回调
 * @param onUpdateConfirm 确认更新（下载）回调
 * @param searchResults 搜索结果列表
 * @param onSearch 发起搜索回调
 * @param onClearSearchResults 清除搜索结果回调
 * @param onSelectSearchResult 选中搜索结果回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    mapView: MapView?,
    isMocking: Boolean,
    targetLocation: com.baidu.mapapi.model.LatLng,
    mapType: Int,
    currentCity: String?,
    runMode: String,
    onRunModeChange: (String) -> Unit,
    onToggleMock: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onLocate: () -> Unit,
    onLocationInputConfirm: (Double, Double, Boolean) -> Unit,
    onMapTypeChange: (Int) -> Unit,
    onNavigate: (Int) -> Unit,
    appVersion: String,
    selectedPoi: PoiInfo?,
    onPoiClose: () -> Unit,
    onPoiSave: (PoiInfo) -> Unit,
    onPoiCopy: (PoiInfo) -> Unit,
    onPoiShare: (PoiInfo) -> Unit,
    onPoiFly: (PoiInfo) -> Unit,
    updateInfo: UpdateInfo?,
    onUpdateDismiss: () -> Unit,
    onUpdateConfirm: (String) -> Unit,
    searchResults: List<Map<String, Any>>?,
    onSearch: (String) -> Unit,
    onClearSearchResults: () -> Unit,
    onSelectSearchResult: (Map<String, Any>) -> Unit,
    onNavigateUp: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Map Type Dialog State
    var showMapTypeDialog by remember { mutableStateOf(false) }
    
    // Location Input Dialog State
    var showLocationInputDialog by remember { mutableStateOf(false) }

    // Run Mode Dialog State
    var showRunModeDialog by remember { mutableStateOf(false) }
    
    // Search State
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    if (showRunModeDialog) {
        AlertDialog(
            onDismissRequest = { showRunModeDialog = false },
            title = { Text(stringResource(R.string.run_mode_dialog_title)) },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onRunModeChange("root")
                                showRunModeDialog = false
                            }
                            .padding(16.dp)
                    ) {
                        RadioButton(
                            selected = runMode == "root",
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.run_mode_root))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onRunModeChange("noroot")
                                showRunModeDialog = false
                            }
                            .padding(16.dp)
                    ) {
                        RadioButton(
                            selected = runMode == "noroot",
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.run_mode_noroot))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRunModeDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showLocationInputDialog) {
        LocationInputDialog(
            onDismiss = { showLocationInputDialog = false },
            onConfirm = { lat, lng, isBd09 ->
                onLocationInputConfirm(lat, lng, isBd09)
                showLocationInputDialog = false
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                DrawerHeader(appVersion)
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_menu_location_simulation)) },
                    icon = { Icon(painterResource(R.drawable.ic_position), contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onNavigate(R.id.nav_location_simulation) } }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_menu_route_simulation)) },
                    icon = { Icon(painterResource(R.drawable.ic_move), contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onNavigate(R.id.nav_route_simulation) } }
                )
                
                Text(
                    text = stringResource(R.string.nav_menu_settings),
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.labelSmall
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_menu_settings)) },
                    icon = { Icon(painterResource(R.drawable.ic_menu_settings), contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onNavigate(R.id.nav_settings) } }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_menu_run_mode)) },
                    icon = { Icon(painterResource(R.drawable.ic_menu_dev), contentDescription = null) }, // Reusing dev icon
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); showRunModeDialog = true } }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_menu_dev)) },
                    icon = { Icon(painterResource(R.drawable.ic_menu_dev), contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onNavigate(R.id.nav_dev) } }
                )

                Text(
                    text = stringResource(R.string.nav_menu_more),
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.labelSmall
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_menu_upgrade)) },
                    icon = { Icon(painterResource(R.drawable.ic_menu_upgrade), contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onNavigate(R.id.nav_update) } }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_menu_feedback)) },
                    icon = { Icon(painterResource(R.drawable.ic_menu_feedback), contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onNavigate(R.id.nav_feedback) } }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_menu_contact)) },
                    icon = { Icon(painterResource(R.drawable.ic_contact), contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onNavigate(R.id.nav_contact) } }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_menu_sponsor)) },
                    icon = { Icon(painterResource(R.drawable.ic_user), contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onNavigate(R.id.nav_sponsor) } }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_menu_github)) },
                    icon = { Icon(painterResource(R.drawable.ic_menu_dev), contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onNavigate(R.id.nav_source_code) } }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (isSearchActive) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { 
                            searchQuery = it
                            onSearch(it)
                        },
                        onSearch = { onSearch(it) },
                        active = true,
                        onActiveChange = { isSearchActive = it },
                        placeholder = { Text("搜索地点") },
                        leadingIcon = {
                            IconButton(onClick = { 
                                isSearchActive = false
                                searchQuery = ""
                                onClearSearchResults()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { 
                                    searchQuery = ""
                                    onSearch("")
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    ) {
                        if (searchResults != null) {
                            LazyColumn {
                                items(searchResults.size) { index ->
                                    val item = searchResults[index]
                                    val name = item[LocationPickerViewModel.POI_NAME].toString()
                                    val address = item[LocationPickerViewModel.POI_ADDRESS].toString()
                                    ListItem(
                                        headlineContent = { Text(name) },
                                        supportingContent = { Text(address) },
                                        modifier = Modifier.clickable {
                                            onSelectSearchResult(item)
                                            isSearchActive = false
                                            searchQuery = ""
                                            onClearSearchResults()
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateUp) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White
                        )
                    )
                }
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    FloatingActionButton(
                        onClick = onToggleMock,
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) {
                        if (isMocking) {
                            Icon(painterResource(R.drawable.ic_stop_black_24dp), contentDescription = "Stop", tint = Color.White)
                        } else {
                            Icon(painterResource(R.drawable.ic_play_arrow_black_24dp), contentDescription = "Start", tint = Color.White)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                if (mapView != null) {
                    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
                }

                // Map Controls Overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MapControlButton(
                        iconRes = R.drawable.ic_map,
                        onClick = { showMapTypeDialog = true }
                    )
                    MapControlButton(
                        iconRes = R.drawable.ic_input,
                        onClick = { showLocationInputDialog = true }
                    )
                    MapControlButton(
                        iconRes = R.drawable.ic_home_position,
                        onClick = onLocate
                    )
                    MapControlButton(
                        iconRes = R.drawable.ic_zoom_in,
                        onClick = onZoomIn
                    )
                    MapControlButton(
                        iconRes = R.drawable.ic_zoom_out,
                        onClick = onZoomOut
                    )
                }

                // POI Detail Card
                if (selectedPoi != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        PoiDetailCard(
                            poiName = selectedPoi.name,
                            poiAddress = selectedPoi.address,
                            onClose = onPoiClose,
                            onSave = { onPoiSave(selectedPoi) },
                            onCopy = { onPoiCopy(selectedPoi) },
                            onShare = { onPoiShare(selectedPoi) },
                            onFly = { onPoiFly(selectedPoi) }
                        )
                    }
                }
            }
        }
    }

    if (showMapTypeDialog) {
        AlertDialog(
            onDismissRequest = { showMapTypeDialog = false },
            title = { Text("选择地图类型") },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { 
                            onMapTypeChange(BaiduMap.MAP_TYPE_NORMAL)
                            showMapTypeDialog = false 
                        }
                    ) {
                        RadioButton(selected = mapType == BaiduMap.MAP_TYPE_NORMAL, onClick = { 
                            onMapTypeChange(BaiduMap.MAP_TYPE_NORMAL)
                            showMapTypeDialog = false 
                        })
                        Text("普通地图")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { 
                            onMapTypeChange(BaiduMap.MAP_TYPE_SATELLITE)
                            showMapTypeDialog = false 
                        }
                    ) {
                        RadioButton(selected = mapType == BaiduMap.MAP_TYPE_SATELLITE, onClick = { 
                            onMapTypeChange(BaiduMap.MAP_TYPE_SATELLITE)
                            showMapTypeDialog = false 
                        })
                        Text("卫星地图")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMapTypeDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (updateInfo != null) {
        UpdateDialog(
            info = updateInfo,
            onDismiss = onUpdateDismiss,
            onConfirm = { onUpdateConfirm(updateInfo.downloadUrl) }
        )
    }
}

/**
 * 更新提示对话框
 * 显示新版本信息并提供下载选项。
 *
 * @param info 更新信息对象
 * @param onDismiss 取消/关闭回调
 * @param onConfirm 确认下载回调
 */
@Composable
fun UpdateDialog(
    info: UpdateInfo,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本: ${info.version}") },
        text = {
             Text(info.content)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("下载")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun MapControlButton(iconRes: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.9f),
        shadowElevation = 4.dp,
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun LocationInputDialog(onDismiss: () -> Unit, onConfirm: (Double, Double, Boolean) -> Unit) {
    var latStr by remember { mutableStateOf("") }
    var lngStr by remember { mutableStateOf("") }
    var isBd09 by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("输入坐标") },
        text = {
            Column {
                OutlinedTextField(
                    value = latStr,
                    onValueChange = { latStr = it },
                    label = { Text("纬度") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lngStr,
                    onValueChange = { lngStr = it },
                    label = { Text("经度") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isBd09, onCheckedChange = { isBd09 = it })
                    Text("BD09坐标 (默认WGS84)")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val lat = latStr.toDoubleOrNull()
                val lng = lngStr.toDoubleOrNull()
                if (lat != null && lng != null) {
                    onConfirm(lat, lng, isBd09)
                }
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
