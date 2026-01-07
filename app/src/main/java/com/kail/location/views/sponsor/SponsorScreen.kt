package com.kail.location.views.sponsor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kail.location.R
import android.content.ClipData
import android.content.ClipboardManager
import com.kail.location.viewmodels.SponsorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorScreen(viewModel: SponsorViewModel, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val address = viewModel.address.collectAsState().value
    val qrBitmap = viewModel.qrBitmap.collectAsState().value
    var copied = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sponsor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = stringResource(R.string.sponsor_tron_note))
            Spacer(modifier = Modifier.height(16.dp))
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "TRON QR",
                    modifier = Modifier.size(220.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(text = address, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard?.setPrimaryClip(ClipData.newPlainText("TRON Address", address))
                copied.value = true
            }) {
                Text(text = if (copied.value) "已复制" else "复制地址")
            }
        }
    }
}
