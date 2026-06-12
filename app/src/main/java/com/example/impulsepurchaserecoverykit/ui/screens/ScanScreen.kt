package com.example.impulsepurchaserecoverykit.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.provider.MediaStore
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Receipt scanning screen that allows the user to select or capture a receipt
 * image for processing.
 *
 * ScanScreen operates in two visual modes depending on the user's interaction:
 *
 * 1. **Gallery mode** (default) — displays a 3-column grid of the 150 most
 *    recent images from the device photo library, with a camera button in the
 *    top bar for taking a new photo and a menu option to browse the full file
 *    system. Tapping any thumbnail switches to preview mode.
 *
 * 2. **Preview mode** — displays the selected image in a full-screen swipeable
 *    [HorizontalPager], allowing the user to review the image before scanning.
 *    A bottom bar provides a "Scan this" button to confirm and a "Gallery"
 *    button to return to the grid.
 *
 * The screen handles runtime permissions for photo library access and camera
 * access, adapting the required permission based on the Android API level
 * (READ_MEDIA_IMAGES on API 33+, READ_EXTERNAL_STORAGE on older versions).
 * If permissions are not granted, a [PermissionRequestScreen] is shown instead.
 *
 * @param onScanReceipt Callback invoked with the [Uri] of the confirmed receipt
 *                      image when the user taps "Scan this". The URI is passed
 *                      back to [MainActivity] to begin the OCR and AI parsing pipeline.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(onScanReceipt: (Uri) -> Unit) {
    val context = LocalContext.current

    // Select the correct permission based on the device's Android version
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionState = rememberPermissionState(permission)
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // List of image URIs loaded from the device photo library
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Index of the currently selected image; -1 means no image is selected (gallery mode)
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // URI of the photo taken by the camera, held temporarily until it is added to the grid
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Load recent images from the device whenever photo library permission is granted
    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            imageUris = withContext(Dispatchers.IO) {
                loadRecentImages(context, limit = 150)
            }
        }
    }

    // Show the permission request screen if photo library access has not been granted
    if (!permissionState.status.isGranted) {
        PermissionRequestScreen(
            shouldShowRationale = permissionState.status.shouldShowRationale,
            onRequestPermission = { permissionState.launchPermissionRequest() }
        )
        return
    }

    val inPreview = selectedIndex >= 0 && selectedIndex < imageUris.size

    // Launcher for the system camera — prepends the captured photo to the grid on success
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            imageUris = listOf(cameraImageUri!!) + imageUris
            selectedIndex = 0
        }
    }

    /**
     * Creates a temporary file in the app's cache directory, generates a
     * FileProvider URI for it, and launches the system camera. The captured
     * photo is written to this URI, then prepended to the image grid.
     */
    fun launchCamera() {
        val photoFile = File.createTempFile("receipt_", ".jpg", context.cacheDir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        cameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    // Launcher for the system file picker — prepends the selected file to the grid
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                imageUris = listOf(uri) + imageUris
                selectedIndex = 0
            }
        }
    }

    if (!inPreview) {
        // ── Gallery mode ──────────────────────────────────────────────────
        Scaffold(
            topBar = {
                var menuExpanded by remember { mutableStateOf(false) }
                TopAppBar(
                    title = { Text("Choose a receipt photo") },
                    actions = {
                        // Camera button — requests permission if not already granted
                        IconButton(
                            onClick = {
                                if (cameraPermissionState.status.isGranted) {
                                    launchCamera()
                                } else {
                                    cameraPermissionState.launchPermissionRequest()
                                }
                            }
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Take photo")
                        }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Browse files / albums") },
                                onClick = {
                                    menuExpanded = false
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
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
                // Empty state — shown when no photos are found or permission was just granted
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("No photos found", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Try using 'Browse files / albums' from the menu above",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "image/*"
                                }
                                filePickerLauncher.launch(intent)
                            }
                        ) { Text("Browse files") }
                    }
                }
            } else {
                // 3-column thumbnail grid of recent device photos
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.padding(padding).fillMaxSize(),
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
        // ── Preview mode ──────────────────────────────────────────────────
        val pagerState = rememberPagerState(initialPage = selectedIndex) { imageUris.size }

        // Keep selectedIndex in sync as the user swipes between images
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

                        // Confirms the selected image and triggers the scan pipeline
                        Button(
                            onClick = { onScanReceipt(currentUri) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Scan this") }
                    }
                }
            }
        ) { padding ->
            // Full-screen swipeable pager for previewing the selected image
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(padding).fillMaxSize()
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

/**
 * Permission request screen shown when the user has not yet granted photo
 * library access.
 *
 * Displays an explanation of why the permission is needed and a button to
 * trigger the system permission request dialog. If the user has previously
 * denied the permission, a rationale message is shown instead directing them
 * to grant access manually in their device settings.
 *
 * @param shouldShowRationale True if the user has previously denied the permission
 *                            and a rationale explanation should be shown
 * @param onRequestPermission Callback invoked when the user taps the permission button
 */
@Composable
private fun PermissionRequestScreen(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text("Photo access needed", style = MaterialTheme.typography.headlineSmall)
            Text(
                if (shouldShowRationale) {
                    "We need access to your photos to scan receipts. " +
                            "Please grant photo access in your device settings."
                } else {
                    "To scan receipts, the app needs permission to access your photo library."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                Text(if (shouldShowRationale) "Open Settings" else "Grant Access")
            }
        }
    }
}

/**
 * Queries the device's MediaStore to retrieve the URIs of the most recently
 * added images from the external photo library.
 *
 * Runs on a background thread via [Dispatchers.IO] — always call from a
 * coroutine context. Results are sorted by date added descending so the
 * most recent photos appear first in the gallery grid.
 *
 * @param context The application context used to access the ContentResolver
 * @param limit The maximum number of image URIs to return
 * @return A list of [Uri] objects pointing to the most recent device images,
 *         capped at [limit] entries
 */
private fun loadRecentImages(context: android.content.Context, limit: Int): List<Uri> {
    val uris = mutableListOf<Uri>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
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
        while (cursor.moveToNext() && count < limit) {
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