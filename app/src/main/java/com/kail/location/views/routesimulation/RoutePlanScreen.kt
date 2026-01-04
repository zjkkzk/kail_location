package com.kail.location.views.routesimulation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.text.BasicTextField
import com.baidu.mapapi.map.MapView
import com.kail.location.R

@Composable
fun RoutePlanScreen(
    mapView: MapView?,
    onBackClick: () -> Unit,
    onConfirmClick: () -> Unit = {}
) {
    var startPoint by remember { mutableStateOf("") }
    var endPoint by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Map Layer (Bottom)
        if (mapView != null) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Top Search Bar Layer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(top = 48.dp, bottom = 16.dp) // Adjust for status bar
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Back Button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Search Inputs
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                ) {
                    // Start Point Input
                    SearchInputRow(
                        iconRes = R.drawable.ic_home_position, // Placeholder, need to check available icons
                        hint = "搜索起点",
                        value = startPoint,
                        onValueChange = { startPoint = it }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // End Point Input
                    SearchInputRow(
                        iconRes = R.drawable.ic_home_position, // Placeholder
                        hint = "搜索终点",
                        value = endPoint,
                        onValueChange = { endPoint = it }
                    )
                }
            }
        }

        // 3. Right Side Map Controls (Layers)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 180.dp, end = 16.dp) // Below the top bar
        ) {
            SmallFloatingActionButton(
                onClick = { /* TODO: Layers */ },
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Place, contentDescription = "Place")
            }
        }

        // 4. Bottom Right Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Undo
            SmallFloatingActionButton(
                onClick = { /* TODO: Undo */ },
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Undo")
            }

            // Pinpoint (Custom icon usually)
            SmallFloatingActionButton(
                onClick = { /* TODO: Pinpoint */ },
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                 // Using a placeholder icon, maybe a map marker
                 Icon(painter = painterResource(id = R.drawable.icon_gcoding), contentDescription = "Pin")
            }

            // Location
            SmallFloatingActionButton(
                onClick = { /* TODO: Locate */ },
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Home, contentDescription = "Locate")
            }

            // Confirm (Big Green Button)
            FloatingActionButton(
                onClick = onConfirmClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Confirm")
            }
        }
    }
}

@Composable
fun SearchInputRow(
    iconRes: Int,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            //.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
    ) {
        // Icon
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp).padding(start = 8.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))

        // Text Field
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

// Helper for BasicTextField (Material3 TextField is too heavy for this custom look)
