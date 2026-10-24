package com.tzz1ac0.miroir

import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.sources.audio.NoAudioSource
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.library.generic.GenericStream
import com.pedro.library.view.RenderErrorCallback
import com.tzz1ac0.miroir.camera.AutoExposureCameraXSource
import java.util.concurrent.Executors
import kotlin.math.pow


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CameraScreen(activity = this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraScreen(activity: ComponentActivity) {
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraMode by rememberSaveable { mutableStateOf(CameraMode.DEFAULT) }
    var menuExpanded by remember { mutableStateOf(false) }
    var settingsDialogVisible by remember { mutableStateOf(false) }
    var selectedSettingsTab by rememberSaveable { mutableStateOf(0) }
    var fullCameraX by rememberSaveable { mutableStateOf(false) }
    var fullRootEncoder by rememberSaveable { mutableStateOf(false) }
    var videoStreamResolution by remember { mutableStateOf("Indisponible") }
    var displayedImageResolution by remember { mutableStateOf("Indisponible") }
    var availableSurfaceResolution by remember { mutableStateOf("Indisponible") }
    val nativeSensorResolution = remember { getNativeSensorResolution(activity) }
    val permissionRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = buildAnnotatedString {
                            append("Miroir — ")
                            withStyle(
                                SpanStyle(
                                    fontSize = MaterialTheme.typography.titleSmall.fontSize,
                                    fontStyle = FontStyle.Italic
                                )
                            ) {
                                append(cameraMode.title)
                            }
                        }
                    )
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("CameraX") },
                            enabled = CameraMode.CAMERAX.isSelectableWhen(cameraMode),
                            onClick = {
                                cameraMode = CameraMode.CAMERAX
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("RootEncoder") },
                            enabled = CameraMode.ROOT_ENCODER.isSelectableWhen(cameraMode),
                            onClick = {
                                cameraMode = CameraMode.ROOT_ENCODER
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Paramètres…") },
                            onClick = {
                                menuExpanded = false
                                settingsDialogVisible = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Quitter") },
                            onClick = {
                                menuExpanded = false
                                activity.finish()
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasCameraPermission) {
                if (cameraMode == CameraMode.ROOT_ENCODER) {
                    key(fullRootEncoder) {
                        RootEncoderCameraPreview(
                            activity = activity,
                            full = fullRootEncoder,
                            modifier = Modifier.weight(1f),
                        onStreamResolutionChanged = { videoStreamResolution = it },
                        onDisplayedResolutionChanged = { displayedImageResolution = it },
                            onSurfaceResolutionChanged = { availableSurfaceResolution = it }
                        )
                    }
                } else {
                    CameraXCameraPreview(
                        activity = activity,
                        full = fullCameraX,
                        modifier = Modifier.weight(1f),
                        onStreamResolutionChanged = { videoStreamResolution = it },
                        onDisplayedResolutionChanged = { displayedImageResolution = it },
                        onSurfaceResolutionChanged = { availableSurfaceResolution = it }
                    )
                }
            } else {
                Text("L’autorisation caméra est nécessaire pour démarrer la capture.")
                Button(onClick = { permissionRequest.launch(Manifest.permission.CAMERA) }) {
                    Text("Autoriser la caméra")
                }
            }
        }
    }

    if (settingsDialogVisible) {
        AlertDialog(
            onDismissRequest = { settingsDialogVisible = false },
            title = { Text("Paramètres") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TabRow(selectedTabIndex = selectedSettingsTab) {
                        Tab(
                            selected = selectedSettingsTab == 0,
                            onClick = { selectedSettingsTab = 0 },
                            text = { Text("Flux") }
                        )
                        Tab(
                            selected = selectedSettingsTab == 1,
                            onClick = { selectedSettingsTab = 1 },
                            text = { Text("CameraX") }
                        )
                        Tab(
                            selected = selectedSettingsTab == 2,
                            onClick = { selectedSettingsTab = 2 },
                            text = { Text("RootEncoder") }
                        )
                    }
                    when (selectedSettingsTab) {
                        0 -> Column(
                            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        ) {
                            SettingsRow(
                                label = "Résolution native du capteur",
                                value = nativeSensorResolution
                            )
                            HorizontalDivider()
                            SettingsRow(
                                label = "Source de la résolution native",
                                value = "Camera2 — SENSOR_INFO_PIXEL_ARRAY_SIZE (caméra avant)"
                            )
                            HorizontalDivider()
                            SettingsRow(
                                label = "Flux vidéo d’entrée",
                                value = videoStreamResolution
                            )
                            HorizontalDivider()
                            SettingsRow(
                                label = "Provenance du flux",
                                value = if (cameraMode == CameraMode.ROOT_ENCODER) {
                                    "RootEncoder — GenericStream.prepareVideo (640 × 480 px, proportion 4/3), source AutoExposureCameraXSource"
                                } else {
                                    "CameraX — Preview.resolutionInfo (résolution négociée avec la caméra)"
                                }
                            )
                            HorizontalDivider()
                            SettingsRow(
                                label = "Image affichée",
                                value = displayedImageResolution
                            )
                            HorizontalDivider()
                            SettingsRow(
                                label = "Mesure de l’image",
                                value = "Taille du composant AndroidView dans Compose"
                            )
                            HorizontalDivider()
                            SettingsRow(
                                label = "Surface d’affichage disponible",
                                value = availableSurfaceResolution
                            )
                            HorizontalDivider()
                            SettingsRow(
                                label = "Mesure de la surface",
                                value = "Taille du conteneur d’aperçu dans Compose"
                            )
                        }
                        1 -> Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Full",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Checkbox(
                                    checked = fullCameraX,
                                    onCheckedChange = { fullCameraX = it }
                                )
                            }
                        }
                        2 -> Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Full",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Checkbox(
                                    checked = fullRootEncoder,
                                    onCheckedChange = { fullRootEncoder = it }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { settingsDialogVisible = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}

/** Baseline validated on the target phone: keep this path independent from RootEncoder. */
@Composable
private fun CameraXCameraPreview(
    activity: ComponentActivity,
    full: Boolean,
    modifier: Modifier = Modifier,
    onStreamResolutionChanged: (String) -> Unit,
    onDisplayedResolutionChanged: (String) -> Unit,
    onSurfaceResolutionChanged: (String) -> Unit
) {
    val previewView = remember {
        PreviewView(activity).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var boundCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var gestureZoom by remember { mutableStateOf(1f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    LaunchedEffect(full, boundCamera) {
        previewView.scaleType = if (full) {
            PreviewView.ScaleType.FIT_CENTER
        } else {
            PreviewView.ScaleType.FILL_CENTER
        }
        gestureZoom = 1f
        panX = 0f
        panY = 0f
        previewView.scaleX = 1f
        previewView.scaleY = 1f
        previewView.translationX = 0f
        previewView.translationY = 0f
        boundCamera?.cameraControl?.setZoomRatio(1f)
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    Column(
        modifier = modifier.fillMaxWidth().onSizeChanged { size ->
            onSurfaceResolutionChanged(formatResolution(size.width, size.height))
        }
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.weight(1f).fillMaxWidth()
                .pointerInput(boundCamera) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        val zoomState = boundCamera?.cameraInfo?.zoomState?.value
                        val maxZoom = zoomState?.maxZoomRatio ?: 4f
                        val minZoom = zoomState?.minZoomRatio ?: 1f
                        gestureZoom = (gestureZoom * zoomChange).coerceIn(minZoom, maxZoom)
                        val width = previewView.width.toFloat()
                        val height = previewView.height.toFloat()
                        val maxPanX = width * (gestureZoom - 1f) / 2f
                        val maxPanY = height * (gestureZoom - 1f) / 2f
                        panX = (panX + panChange.x).coerceIn(-maxPanX, maxPanX)
                        panY = (panY + panChange.y).coerceIn(-maxPanY, maxPanY)
                        previewView.scaleX = gestureZoom
                        previewView.scaleY = gestureZoom
                        previewView.translationX = panX
                        previewView.translationY = panY
                    }
                }
                .pointerInput(boundCamera) {
                    detectTapGestures(
                        onDoubleTap = {
                            gestureZoom = 1f
                            panX = 0f
                            panY = 0f
                            previewView.scaleX = 1f
                            previewView.scaleY = 1f
                            previewView.translationX = 0f
                            previewView.translationY = 0f
                            boundCamera?.cameraControl?.setZoomRatio(1f)
                        }
                    )
                }
                .onSizeChanged { size ->
                    onDisplayedResolutionChanged(formatResolution(size.width, size.height))
                }
        )
    }

    DisposableEffect(activity, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(activity)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        providerFuture.addListener({
            if (disposed) return@addListener
            try {
                provider = providerFuture.get()
                val previewBuilder = Preview.Builder()
                Camera2Interop.Extender(previewBuilder).setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE,
                    android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE_ON
                )
                val preview = previewBuilder.build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysisBuilder = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                Camera2Interop.Extender(analysisBuilder).setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE,
                    android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE_ON
                )
                val analysis = analysisBuilder.build()
                analysis.setAnalyzer(analysisExecutor) { image -> image.close() }
                provider?.unbindAll()
                boundCamera = provider?.bindToLifecycle(
                    activity, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis
                )
                preview.resolutionInfo?.resolution?.let { resolution ->
                    onStreamResolutionChanged(formatResolution(resolution.width, resolution.height))
                }
            } catch (error: Exception) {
                Log.e("MiroirCamera", "Échec de démarrage de la caméra CameraX", error)
            }
        }, ContextCompat.getMainExecutor(activity))
        onDispose {
            disposed = true
            provider?.unbindAll()
            boundCamera = null
            analysisExecutor.shutdown()
        }
    }
}

/** Opt-in experiment. The validated CameraX path remains the default and is not changed. */
@Composable
private fun RootEncoderCameraPreview(
    activity: ComponentActivity,
    full: Boolean,
    modifier: Modifier = Modifier,
    onStreamResolutionChanged: (String) -> Unit,
    onDisplayedResolutionChanged: (String) -> Unit,
    onSurfaceResolutionChanged: (String) -> Unit
) {
    var prepared by remember { mutableStateOf(false) }
    val source = remember { AutoExposureCameraXSource(activity.applicationContext) }
    val stream = remember {
        GenericStream(
            activity.applicationContext,
            object : ConnectChecker {
                override fun onConnectionStarted(url: String) {}
                override fun onConnectionSuccess() {}
                override fun onConnectionFailed(reason: String) {}
                override fun onDisconnect() {}
                override fun onAuthError() {}
                override fun onAuthSuccess() {}
                override fun onStreamingStats(report: com.pedro.common.StreamingStatsReport) {}
            },
            source,
            NoAudioSource()
        )
    }
    DisposableEffect(stream, source, full) {
        try {
            if (full) {
                val largestResolution = source.getCameraResolutions(CameraHelper.Facing.FRONT)
                    .maxByOrNull { it.width.toLong() * it.height.toLong() }
                source.setRequiredResolution(largestResolution)
            } else {
                source.setRequiredResolution(null)
            }
            stream.getGlInterface().setRenderErrorCallback(object : RenderErrorCallback {
                override fun onRenderError(error: RuntimeException) {
                }
            })
            stream.getGlInterface().autoHandleOrientation = true
            prepared = stream.prepareVideo(width = 640, height = 480, bitrate = 1_200_000, fps = 30)
            onStreamResolutionChanged(if (prepared) formatResolution(640, 480) else "Indisponible")
        } catch (error: Exception) {
            prepared = false
            Log.e("MiroirCamera", "Échec de préparation de RootEncoder", error)
        }
        onDispose {
            if (stream.isOnPreview) stream.stopPreview()
            stream.release()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().onSizeChanged { size ->
            onSurfaceResolutionChanged(formatResolution(size.width, size.height))
        }
    ) {
        if (prepared) {
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val targetAspectRatio = if (full) {
                    val resolution = source.getCameraResolutions(CameraHelper.Facing.FRONT)
                        .maxByOrNull { it.width.toLong() * it.height.toLong() }
                    if (resolution != null && resolution.height > 0) {
                        resolution.width.toFloat() / resolution.height.toFloat()
                    } else 4f / 3f
                } else 0f
                val availableAspectRatio = maxWidth.value / maxHeight.value.coerceAtLeast(1f)
                val previewModifier = if (full && targetAspectRatio > 0f) {
                    if (availableAspectRatio > targetAspectRatio) {
                        Modifier.fillMaxHeight().width(maxHeight * targetAspectRatio)
                    } else {
                        Modifier.fillMaxWidth().height(maxWidth / targetAspectRatio)
                    }
                } else Modifier.fillMaxSize()
                AndroidView(
                factory = { context ->
                    SurfaceView(context).apply {
                        val preview = this
                        var gestureZoom = 1f
                        var panX = 0f
                        var panY = 0f
                        var lastFocusX = 0f
                        var lastFocusY = 0f

                        fun applyGestureTransform() {
                            val maxPanX = preview.width * (gestureZoom - 1f) / 2f
                            val maxPanY = preview.height * (gestureZoom - 1f) / 2f
                            panX = panX.coerceIn(-maxPanX, maxPanX)
                            panY = panY.coerceIn(-maxPanY, maxPanY)
                            preview.scaleX = gestureZoom
                            preview.scaleY = gestureZoom
                            preview.translationX = panX
                            preview.translationY = panY
                        }

                        val scaleDetector = ScaleGestureDetector(
                            context,
                            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                override fun onScale(detector: ScaleGestureDetector): Boolean {
                                    gestureZoom = (gestureZoom * detector.scaleFactor.pow(1.5f)).coerceIn(1f, 4f)
                                    applyGestureTransform()
                                    return true
                                }
                            }
                        )
                        val tapDetector = GestureDetector(
                            context,
                            object : GestureDetector.SimpleOnGestureListener() {
                                override fun onDown(event: MotionEvent): Boolean = true

                                override fun onDoubleTap(event: MotionEvent): Boolean {
                                    gestureZoom = 1f
                                    panX = 0f
                                    panY = 0f
                                    applyGestureTransform()
                                    return true
                                }
                            }
                        )

                        setOnTouchListener { _, event ->
                            scaleDetector.onTouchEvent(event)
                            tapDetector.onTouchEvent(event)

                            if (event.actionMasked == MotionEvent.ACTION_POINTER_UP) {
                                // Exclude the pointer that is being lifted when resetting the pan anchor.
                                val remainingCount = event.pointerCount - 1
                                if (remainingCount >= 2) {
                                    val liftedIndex = event.actionIndex
                                    val focusX = (0 until event.pointerCount)
                                        .filter { it != liftedIndex }
                                        .map { event.getX(it) }
                                        .average().toFloat()
                                    val focusY = (0 until event.pointerCount)
                                        .filter { it != liftedIndex }
                                        .map { event.getY(it) }
                                        .average().toFloat()
                                    lastFocusX = focusX
                                    lastFocusY = focusY
                                } else {
                                    lastFocusX = 0f
                                    lastFocusY = 0f
                                }
                            } else if (event.pointerCount >= 2) {
                                val focusX = (0 until event.pointerCount).map { event.getX(it) }.average().toFloat()
                                val focusY = (0 until event.pointerCount).map { event.getY(it) }.average().toFloat()
                                if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                                    lastFocusX = focusX
                                    lastFocusY = focusY
                                } else if (event.actionMasked == MotionEvent.ACTION_MOVE && gestureZoom > 1f) {
                                    panX += (focusX - lastFocusX) * 2.5f
                                    panY += (focusY - lastFocusY) * 2.5f
                                    applyGestureTransform()
                                    lastFocusX = focusX
                                    lastFocusY = focusY
                                }
                            } else if (event.actionMasked == MotionEvent.ACTION_UP) {
                                lastFocusX = 0f
                                lastFocusY = 0f
                            }
                            true
                        }

                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                try {
                                    stream.startPreview(this@apply, true)
                                } catch (error: Exception) {
                                    Log.e("MiroirCamera", "Échec du démarrage de l’aperçu RootEncoder", error)
                                }
                            }
                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                            }
                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                            }
                        })
                    }
                },
                modifier = previewModifier.onSizeChanged { size ->
                    onDisplayedResolutionChanged(formatResolution(size.width, size.height))
                }
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.42f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.58f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

fun greetingFor(name: String = "World"): String = "Hello $name!"



private fun formatResolution(width: Int, height: Int): String {
    fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    val divisor = gcd(width, height).coerceAtLeast(1)
    return "$width × $height px [${width / divisor}/${height / divisor}]"
}

private fun getNativeSensorResolution(activity: ComponentActivity): String {
    return try {
        val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val frontCameraId = cameraManager.cameraIdList.firstOrNull { cameraId ->
            cameraManager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        } ?: return "Indisponible (caméra avant introuvable)"
        val size = cameraManager.getCameraCharacteristics(frontCameraId)
            .get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            ?: return "Indisponible"
        formatResolution(size.width, size.height)
    } catch (error: Exception) {
        Log.e("MiroirCamera", "Impossible de lire la résolution native du capteur", error)
        "Indisponible"
    }
}
