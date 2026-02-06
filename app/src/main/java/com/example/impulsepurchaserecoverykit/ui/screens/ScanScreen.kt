package com.example.impulsepurchaserecoverykit.ui.screens

import android.content.ContentUris
import android.content.Intent
import android.provider.MediaStore
import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.contracts.contract

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(onScanReceipt: (Uri) -> Unit) {
    val context = LocalContext.current

    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        imageUris = withContext(kotlinx.coroutines.Dispatchers.IO) {
            loadRecentImages(context, limit = 150)
        }
    }

    val inPreview = selectedIndex >= 0 && selectedIndex < imageUris.size

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if(result.resultCode == android.app.Activity.RESULT_OK){
            val uri = result.data?.data
            if (uri != null){
                imageUris = listOf(uri) + imageUris
                selectedIndex = 0
        }} }

    if (!inPreview) {
        Scaffold(
            topBar = {
                var menuExpanded by remember { mutableStateOf(false) }
                TopAppBar(
                    title = { Text("Choose a receipt photo") },
                    actions = {
                        IconButton(onClick = { menuExpanded = true}) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = {menuExpanded = false}
                        ) {
                            DropdownMenuItem(
                                text = {Text("Browse files / albums")},
                                onClick = {
                                    menuExpanded = false
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "image/*"
                                    }
                                    filePickerLauncher.launch(intent)
                                }
                            )
                        }
                    }
                )
            }
        ) { padding ->
            if (imageUris.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No recent images found.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(imageUris.size) { index ->
                        val uri = imageUris[index]

                        ElevatedCard(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { selectedIndex = index }
                        ) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    ImageView(ctx).apply {
                                        scaleType = ImageView.ScaleType.CENTER_CROP
                                        setImageURI(uri)
                                    }
                                },
                                update = { it.setImageURI(uri) }
                            )
                        }
                    }
                }
            }
        }
    } else {
        val pagerState = rememberPagerState(initialPage = selectedIndex) { imageUris.size }

        LaunchedEffect(pagerState.currentPage) {
            selectedIndex = pagerState.currentPage
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Preview") },
                    navigationIcon = {
                        IconButton(onClick = { selectedIndex = -1 }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to gallery")
                        }
                    }
                )
            },
            bottomBar = {
                val currentUri = imageUris[pagerState.currentPage]
                Surface(tonalElevation = 4.dp) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .navigationBarsPadding()
                            .padding(bottom = 72.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedIndex = -1 },
                            modifier = Modifier.weight(1f)
                        ) { Text("Gallery") }

                        Button(
                            onClick = { onScanReceipt(currentUri) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Scan this") }
                    }
                }
            }
        ) { padding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) { page ->
                val uri = imageUris[page]
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setImageURI(uri)
                        }
                    },
                    update = { it.setImageURI(uri) }
                )
            }
        }
    }
}
private fun loadRecentImages(
    context: android.content.Context,
    limit: Int
): List<Uri>{
    val uris = mutableListOf<Uri>()
    val projection = arrayOf(
        MediaStore.Images.Media._ID
    )
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        var count = 0
        while (cursor.moveToNext() && count < limit){
            val id = cursor.getLong(idCol)
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )
            uris.add(contentUri)
            count++
        }
    }
    return uris
}