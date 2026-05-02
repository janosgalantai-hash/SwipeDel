package com.swipedel

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.swipedel.ui.theme.SwipeDelTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

// ─── Models ────────────────────────────────────────────────────────────────────

data class MediaImage(val uri: Uri, val id: Long, val name: String)
enum class Screen { SWIPE, TRASH }

// ─── TrashManager ──────────────────────────────────────────────────────────────
// Tracks which originals were moved to the in-app trash folder.
// Copy lives in getExternalFilesDir("SwipeDelTrash") — no permission dialog needed.
// Original URIs are stored so we can batch-delete them later with one confirmation.

object TrashManager {
    private const val PREFS = "swipedel_trash"
    private const val KEY_IDS = "ids"
    private const val KEY_URIS = "uris"

    fun trashDir(context: Context): File =
        File(context.getExternalFilesDir(null), "SwipeDelTrash").also { it.mkdirs() }

    fun add(context: Context, id: Long, uri: Uri) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = p.getStringSet(KEY_IDS, emptySet())!!.toMutableSet().also { it.add(id.toString()) }
        val uris = p.getStringSet(KEY_URIS, emptySet())!!.toMutableSet().also { it.add(uri.toString()) }
        p.edit().putStringSet(KEY_IDS, ids).putStringSet(KEY_URIS, uris).apply()
    }

    fun trashedIds(context: Context): Set<Long> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_IDS, emptySet())!!.map { it.toLong() }.toSet()

    fun originalUris(context: Context): List<Uri> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_URIS, emptySet())!!.map { Uri.parse(it) }

    fun removeById(context: Context, id: Long) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = p.getStringSet(KEY_IDS, emptySet())!!.toMutableSet().also { it.remove(id.toString()) }
        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
        val uris = p.getStringSet(KEY_URIS, emptySet())!!.toMutableSet().also { it.remove(uri) }
        p.edit().putStringSet(KEY_IDS, ids).putStringSet(KEY_URIS, uris).apply()
    }

    fun clearAll(context: Context) {
        trashDir(context).listFiles()?.forEach { it.delete() }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

// ─── Activity ──────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SwipeDelTheme { SwipeDelApp() } }
    }
}

// ─── Root ──────────────────────────────────────────────────────────────────────

@Composable
fun SwipeDelApp() {
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    var images by remember { mutableStateOf<List<MediaImage>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var permissionGranted by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf(Screen.SWIPE) }
    val scope = rememberCoroutineScope()

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    fun reloadImages() {
        scope.launch(Dispatchers.IO) {
            val loaded = loadImages(context)
            withContext(Dispatchers.Main) { images = loaded; currentIndex = 0 }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        permissionGranted = perms.values.any { it }
        if (permissionGranted) reloadImages()
    }

    LaunchedEffect(Unit) {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
        else arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        permissionLauncher.launch(perms)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when {
            !permissionGranted -> Text(
                "Storage permission required\nto access photos",
                color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center
            )
            screen == Screen.TRASH -> TrashScreen(
                context = context,
                onBack = { reloadImages(); screen = Screen.SWIPE }
            )
            images.isEmpty() -> Text("No photos found", color = Color.White, fontSize = 18.sp)
            currentIndex >= images.size -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("All done!", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                Button(onClick = ::reloadImages) { Text("Start Over") }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { screen = Screen.TRASH }) {
                    Icon(Icons.Default.Delete, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("View Trash", color = Color.White)
                }
            }
            else -> SwipeScreen(
                image = images[currentIndex],
                progress = "${currentIndex + 1} / ${images.size}",
                onSwipeLeft = {
                    val img = images[currentIndex]
                    // Copy to app-private trash — no permission dialog
                    scope.launch(Dispatchers.IO) { moveToAppTrash(context, img) }
                    images = images.toMutableList().also { it.removeAt(currentIndex) }
                    if (currentIndex >= images.size && currentIndex > 0) currentIndex--
                },
                onSwipeRight = { currentIndex++ },
                onOpenTrash = { screen = Screen.TRASH },
                onExit = { (context as? android.app.Activity)?.finishAndRemoveTask() }
            )
        }
    }
}

// ─── Swipe Screen ──────────────────────────────────────────────────────────────

@Composable
fun SwipeScreen(
    image: MediaImage,
    progress: String,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onOpenTrash: () -> Unit,
    onExit: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember(image.uri) { Animatable(0f) }
    val offsetY = remember(image.uri) { Animatable(0f) }
    val swipeThreshold = 350f
    val overlayAlpha = (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 1f)
    val isLeft = offsetX.value < 0

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offsetX.value
                    translationY = offsetY.value
                    rotationZ = (offsetX.value / 25f).coerceIn(-20f, 20f)
                }
                .pointerInput(image.uri) {
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetX.value < -swipeThreshold -> {
                                        offsetX.animateTo(-2500f, tween(250))
                                        onSwipeLeft()
                                        offsetX.snapTo(0f); offsetY.snapTo(0f)
                                    }
                                    offsetX.value > swipeThreshold -> {
                                        offsetX.animateTo(2500f, tween(250))
                                        onSwipeRight()
                                        offsetX.snapTo(0f); offsetY.snapTo(0f)
                                    }
                                    else -> {
                                        offsetX.animateTo(0f, tween(300))
                                        offsetY.animateTo(0f, tween(300))
                                    }
                                }
                            }
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo(offsetX.value + drag.x)
                                offsetY.snapTo(offsetY.value + drag.y)
                            }
                        }
                    )
                }
        ) {
            AsyncImage(
                model = image.uri,
                contentDescription = image.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            if (overlayAlpha > 0.05f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isLeft) Color.Red.copy(alpha = overlayAlpha * 0.55f)
                            else Color.Green.copy(alpha = overlayAlpha * 0.55f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(if (isLeft) R.drawable.ic_arrow_left else R.drawable.ic_arrow_right),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = Color.White.copy(alpha = overlayAlpha)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(progress, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            IconButton(
                onClick = onExit,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_power),
                    contentDescription = "Exit",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(
                onClick = onOpenTrash,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Trash", tint = Color.White)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(80.dp)
        ) {
            ActionHint(R.drawable.ic_arrow_left, "Delete", Color(0xFFFF4444))
            ActionHint(R.drawable.ic_arrow_right, "Keep", Color(0xFF44CC44))
        }
    }
}

// ─── Trash Screen ──────────────────────────────────────────────────────────────

@Composable
fun TrashScreen(context: Context, onBack: () -> Unit) {
    var trashFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()

    val deleteOriginalsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            TrashManager.clearAll(context)
            trashFiles = emptyList()
            selected = emptySet()
        }
    }

    LaunchedEffect(Unit) {
        trashFiles = withContext(Dispatchers.IO) {
            TrashManager.trashDir(context).listFiles()
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
        }
    }

    fun restoreSelected() {
        scope.launch(Dispatchers.IO) {
            selected.forEach { path ->
                val file = File(path)
                val id = file.nameWithoutExtension.toLongOrNull() ?: return@forEach
                TrashManager.removeById(context, id)
                file.delete()
            }
            val remaining = trashFiles.filter { it.absolutePath !in selected }
            withContext(Dispatchers.Main) {
                trashFiles = remaining
                selected = emptySet()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onBack) {
                    Text("← Back", color = Color.White, fontSize = 16.sp)
                }
                Text(
                    if (selected.isEmpty()) "Trash (${trashFiles.size})"
                    else "${selected.size} selected",
                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
                if (trashFiles.isNotEmpty()) {
                    TextButton(onClick = {
                        scope.launch {
                            val uris = withContext(Dispatchers.IO) { TrashManager.originalUris(context) }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && uris.isNotEmpty()) {
                                val req = MediaStore.createDeleteRequest(context.contentResolver, uris)
                                deleteOriginalsLauncher.launch(IntentSenderRequest.Builder(req).build())
                            } else {
                                withContext(Dispatchers.IO) {
                                    uris.forEach { uri ->
                                        try { context.contentResolver.delete(uri, null, null) } catch (_: Exception) {}
                                    }
                                    TrashManager.clearAll(context)
                                }
                                trashFiles = emptyList()
                                selected = emptySet()
                            }
                        }
                    }) {
                        Text("Empty", color = Color(0xFFFF4444), fontSize = 15.sp)
                    }
                } else {
                    Spacer(Modifier.width(60.dp))
                }
            }

            if (trashFiles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Trash is empty", color = Color.Gray, fontSize = 18.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 2.dp, top = 2.dp, end = 2.dp,
                        bottom = if (selected.isNotEmpty()) 100.dp else 2.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(trashFiles, key = { it.absolutePath }) { file ->
                        val isSelected = file.absolutePath in selected
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .fillMaxWidth()
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp, Color.White, RoundedCornerShape(4.dp)
                                    ) else Modifier
                                )
                                .clickable {
                                    selected = if (isSelected)
                                        selected - file.absolutePath
                                    else
                                        selected + file.absolutePath
                                }
                        ) {
                            AsyncImage(
                                model = file,
                                contentDescription = file.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(22.dp)
                                            .background(Color.White, RoundedCornerShape(50)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_check),
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Restore button — appears when anything is selected
        if (selected.isNotEmpty()) {
            Button(
                onClick = ::restoreSelected,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .height(52.dp)
                    .widthIn(min = 200.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text(
                    "Restore ${selected.size}",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─── Helpers ───────────────────────────────────────────────────────────────────

@Composable
fun ActionHint(iconRes: Int, label: String, tint: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(painterResource(iconRes), null, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** Copies image bytes into the app-private trash folder. No permission dialog ever needed. */
fun moveToAppTrash(context: Context, image: MediaImage) {
    try {
        val ext = image.name.substringAfterLast('.', "jpg")
        val dest = File(TrashManager.trashDir(context), "${image.id}.$ext")
        context.contentResolver.openInputStream(image.uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        }
        TrashManager.add(context, image.id, image.uri)
    } catch (_: Exception) {}
}

fun loadImages(context: Context): List<MediaImage> {
    val excluded = TrashManager.trashedIds(context)
    val images = mutableListOf<MediaImage>()
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME),
        null, null,
        "${MediaStore.Images.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            if (id in excluded) continue
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            images.add(MediaImage(uri, id, cursor.getString(nameCol)))
        }
    }
    return images
}
