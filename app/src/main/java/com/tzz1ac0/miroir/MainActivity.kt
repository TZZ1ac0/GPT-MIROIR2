package com.tzz1ac0.miroir

import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import com.pedro.rtspserver.RtspServerStream
import com.pedro.library.view.RenderErrorCallback
import com.tzz1ac0.miroir.camera.AutoExposureCameraXSource
import com.tzz1ac0.miroir.camera.PanFilterRender
import java.util.concurrent.Executors


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
    var isFrontCamera by rememberSaveable { mutableStateOf(true) }
    var menuExpanded by remember { mutableStateOf(false) }
    var settingsDialogVisible by remember { mutableStateOf(false) }
    var selectedSettingsTab by rememberSaveable { mutableStateOf(0) }
    var fullCameraX by rememberSaveable { mutableStateOf(false) }
    var fullRootEncoder by rememberSaveable { mutableStateOf(false) }
    var rtspEnabled by rememberSaveable { mutableStateOf(false) }
    var rtspPortText by rememberSaveable { mutableStateOf("8554") }
    var rtspServerRunning by remember { mutableStateOf(false) }
    val rtspPort = rtspPortText.toIntOrNull()?.takeIf { it in 1..65535 }
    var videoStreamResolution by remember { mutableStateOf("Indisponible") }
    var displayedImageResolution by remember { mutableStateOf("Indisponible") }
    var availableSurfaceResolution by remember { mutableStateOf("Indisponible") }
    val nativeSensorResolution = remember(isFrontCamera) { getNativeSensorResolution(activity, isFrontCamera) }
    val permissionRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(48.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Miroir — ")
                        Text(
                            text = cameraMode.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontStyle = FontStyle.Italic
                            )
                        )
                        Text(
                            text = if (cameraMode == CameraMode.ROOT_ENCODER && rtspServerRunning) {
                                " · RTSP"
                            } else {
                                " · no RTSP"
                            }
                        )
                        Text(text = if (isFrontCamera) " (avant)" else " (arrière)")
                    }
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
                            text = { Text(if (isFrontCamera) "Arrière" else "Avant") },
                            onClick = {
                                isFrontCamera = !isFrontCamera
                                menuExpanded = false
                            }
                        )
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (hasCameraPermission) {
                if (cameraMode == CameraMode.ROOT_ENCODER) {
                    key(fullRootEncoder, rtspPort) {
                        RootEncoderCameraPreview(
                            activity = activity,
                            full = fullRootEncoder,
                            isFrontCamera = isFrontCamera,
                            rtspEnabled = rtspEnabled && rtspPort != null,
                            rtspPort = rtspPort ?: 8554,
                            modifier = Modifier.weight(1f),
                            onStreamResolutionChanged = { videoStreamResolution = it },
                            onDisplayedResolutionChanged = { displayedImageResolution = it },
                            onSurfaceResolutionChanged = { availableSurfaceResolution = it },
                            onRtspServerStateChanged = { rtspServerRunning = it }
                        )
                    }
                } else {
                    CameraXCameraPreview(
                        activity = activity,
                        full = fullCameraX,
                        isFrontCamera = isFrontCamera,
                        modifier = Modifier.weight(1f),
                        onStreamResolutionChanged = { videoStreamResolution = it },
                        onDisplayedResolutionChanged = { displayedImageResolution = it },
                        onSurfaceResolutionChanged = { availableSurfaceResolution = it }
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "L’autorisation caméra est nécessaire pour démarrer la capture.",
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { permissionRequest.launch(Manifest.permission.CAMERA) }) {
                            Text("Autoriser la caméra")
                        }
                    }
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
                        Tab(
                            selected = selectedSettingsTab == 3,
                            onClick = { selectedSettingsTab = 3 },
                            text = { Text("RTSP") }
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
                                value = "Camera2 — SENSOR_INFO_PIXEL_ARRAY_SIZE (${if (isFrontCamera) "caméra avant" else "caméra arrière"})"
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
                        3 -> Column(
                            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Serveur RTSP",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Checkbox(
                                    checked = rtspEnabled,
                                    onCheckedChange = { enabled ->
                                        if (!enabled || rtspPort != null) rtspEnabled = enabled
                                    }
                                )
                            }
                            OutlinedTextField(
                                value = rtspPortText,
                                onValueChange = { value ->
                                    if (value.length <= 5 && value.all { it.isDigit() }) {
                                        rtspPortText = value
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Port RTSP") },
                                singleLine = true,
                                enabled = !rtspEnabled,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = rtspPort == null
                            )
                            if (rtspPort == null) {
                                Text(
                                    text = "Saisissez un port compris entre 1 et 65535.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = "Exemple de commande VLC",
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = rtspPort?.let { "vlc rtsp://<IP_DU_TÉLÉPHONE>:$it/" }
                                    ?: "Le port doit être compris entre 1 et 65535.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Le serveur est disponible uniquement en mode RootEncoder. L’adresse IP doit être celle du téléphone sur le réseau local.",
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
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
    isFrontCamera: Boolean,
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
    // Keep zoom and normalized pan offsets across Activity recreation (device rotation).
    // Pan is stored as a fraction of the available translation, not as screen pixels.
    var gestureZoom by rememberSaveable { mutableStateOf(1f) }
    var panX by rememberSaveable { mutableStateOf(0f) }
    var panY by rememberSaveable { mutableStateOf(0f) }
    var previousFull by rememberSaveable { mutableStateOf(full) }

    LaunchedEffect(full) {
        previewView.scaleType = if (full) {
            PreviewView.ScaleType.FIT_CENTER
        } else {
            PreviewView.ScaleType.FILL_CENTER
        }
        // Changing the display mode intentionally resets framing, but Activity recreation
        // with the same mode must restore the saved framing.
        if (previousFull != full) {
            gestureZoom = 1f
            panX = 0f
            panY = 0f
            boundCamera?.cameraControl?.setZoomRatio(1f)
            previousFull = full
        }
    }
    LaunchedEffect(gestureZoom, panX, panY) {
        val maxPanX = previewView.width * (gestureZoom - 1f) / 2f
        val maxPanY = previewView.height * (gestureZoom - 1f) / 2f
        previewView.scaleX = gestureZoom
        previewView.scaleY = gestureZoom
        previewView.translationX = panX * maxPanX
        previewView.translationY = panY * maxPanY
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
                        val nextZoom = (gestureZoom * zoomChange).coerceIn(minZoom, maxZoom)
                        val maxPanX = previewView.width * (nextZoom - 1f) / 2f
                        val maxPanY = previewView.height * (nextZoom - 1f) / 2f
                        gestureZoom = nextZoom
                        panX = if (maxPanX > 0f) {
                            (panX + panChange.x / maxPanX).coerceIn(-1f, 1f)
                        } else 0f
                        panY = if (maxPanY > 0f) {
                            (panY + panChange.y / maxPanY).coerceIn(-1f, 1f)
                        } else 0f
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
                    // Reapply the saved normalized framing when the preview gets its new size.
                    val maxPanX = size.width * (gestureZoom - 1f) / 2f
                    val maxPanY = size.height * (gestureZoom - 1f) / 2f
                    previewView.scaleX = gestureZoom
                    previewView.scaleY = gestureZoom
                    previewView.translationX = panX * maxPanX
                    previewView.translationY = panY * maxPanY
                }
        )
    }

    DisposableEffect(activity, previewView, isFrontCamera) {
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
                    activity,
                    if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
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
    isFrontCamera: Boolean,
    rtspEnabled: Boolean,
    rtspPort: Int,
    modifier: Modifier = Modifier,
    onStreamResolutionChanged: (String) -> Unit,
    onDisplayedResolutionChanged: (String) -> Unit,
    onSurfaceResolutionChanged: (String) -> Unit,
    onRtspServerStateChanged: (Boolean) -> Unit
) {
    var prepared by remember { mutableStateOf(false) }
    // Save the crop state across Activity recreation (including device rotation).
    // Pan values are normalized texture offsets, so they remain independent of view size.
    var gestureZoom by rememberSaveable { mutableStateOf(1f) }
    var panX by rememberSaveable { mutableStateOf(0f) }
    var panY by rememberSaveable { mutableStateOf(0f) }
    val source = remember(rtspPort) { AutoExposureCameraXSource(activity.applicationContext) }
    val stream = remember(rtspPort) {
        RtspServerStream(
            activity.applicationContext,
            rtspPort,
            object : ConnectChecker {
                override fun onConnectionStarted(url: String) {}
                override fun onConnectionSuccess() {}
                override fun onConnectionFailed(reason: String) {}
                override fun onDisconnect() {}
                override fun onAuthError() {}
                override fun onAuthSuccess() {}
            },
            source,
            NoAudioSource()
        )
    }
    val panFilter = remember(stream) { PanFilterRender() }
    LaunchedEffect(source, isFrontCamera, full) {
        val facing = if (isFrontCamera) CameraHelper.Facing.FRONT else CameraHelper.Facing.BACK
        if (full) {
            val largestResolution = source.getCameraResolutions(facing)
                .maxByOrNull { it.width.toLong() * it.height.toLong() }
            source.setRequiredResolution(largestResolution)
        } else {
            source.setRequiredResolution(null)
        }
        source.setCameraFacing(isFrontCamera)
    }
    DisposableEffect(stream, source, full) {
        try {
            if (full) {
                val facing = if (isFrontCamera) CameraHelper.Facing.FRONT else CameraHelper.Facing.BACK
                val largestResolution = source.getCameraResolutions(facing)
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
            val videoPrepared = stream.prepareVideo(width = 640, height = 480, bitrate = 1_200_000, fps = 30)
            val audioPrepared = stream.prepareAudio(sampleRate = 44_100, isStereo = true, bitrate = 64_000)
            prepared = videoPrepared && audioPrepared
            if (prepared) {
                stream.getGlInterface().setFilter(panFilter)
                panFilter.setTransform(gestureZoom, panX, panY)
            }
            onStreamResolutionChanged(if (prepared) formatResolution(640, 480) else "Indisponible")
        } catch (error: Exception) {
            prepared = false
            Log.e("MiroirCamera", "Échec de préparation de RootEncoder", error)
        }

        onDispose {
            onRtspServerStateChanged(false)
            if (stream.isStreaming) stream.stopStream()
            if (stream.isOnPreview) stream.stopPreview()
            stream.release()
        }
    }

    // RTSP is controlled independently from the preview/encoder lifecycle. Toggling
    // the option must not release the StreamBase instance and then try to reuse it.
    LaunchedEffect(stream, prepared, rtspEnabled) {
        if (!prepared || !rtspEnabled) {
            if (stream.isStreaming) stream.stopStream()
            onRtspServerStateChanged(false)
        } else if (stream.isStreaming) {
            // The stream may already have been started by an earlier effect pass.
            onRtspServerStateChanged(true)
        } else {
            try {
                stream.startStream()
                onRtspServerStateChanged(stream.isStreaming)
                Log.i("MiroirCamera", "Serveur RTSP démarré sur le port $rtspPort")
            } catch (error: Exception) {
                onRtspServerStateChanged(false)
                Log.e("MiroirCamera", "Échec de démarrage du serveur RTSP sur le port $rtspPort", error)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().onSizeChanged { size ->
            onSurfaceResolutionChanged(formatResolution(size.width, size.height))
        }
    ) {
        if (prepared) {
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val targetAspectRatio = if (full) {
                    val facing = if (isFrontCamera) CameraHelper.Facing.FRONT else CameraHelper.Facing.BACK
                    val resolution = source.getCameraResolutions(facing)
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
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                try {
                                    stream.startPreview(this@apply, true)
                                } catch (error: Exception) {
                                    Log.e("MiroirCamera", "Échec du démarrage de l’aperçu RootEncoder", error)
                                }
                            }
                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                                stream.getGlInterface().setPreviewResolution(width, height)
                            }
                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                if (stream.isOnPreview) stream.stopPreview()
                            }
                        })
                    }
                },
                modifier = previewModifier
                    .pointerInput(source, prepared, panFilter) {
                        detectTransformGestures { _, panChange, zoomChange, _ ->
                            if (prepared) {
                                val zoomRange = source.getZoomRange()
                                val minZoom = if (zoomRange.lower > 0f) zoomRange.lower else 1f
                                val maxZoom = if (zoomRange.upper >= minZoom) zoomRange.upper else 4f
                                gestureZoom = (gestureZoom * zoomChange).coerceIn(minZoom, maxZoom)
                                panFilter.setTransform(gestureZoom, panX, panY)

                                val width = size.width.toFloat().coerceAtLeast(1f)
                                val height = size.height.toFloat().coerceAtLeast(1f)
                                val panScale = 1f / gestureZoom
                                val maxPan = 0.5f * (1f - panScale)
                                panX = (panX - panChange.x / width * panScale).coerceIn(-maxPan, maxPan)
                                panY = (panY + panChange.y / height * panScale).coerceIn(-maxPan, maxPan)
                                panFilter.setTransform(gestureZoom, panX, panY)
                            }
                        }
                    }
                    .pointerInput(source, prepared, panFilter) {
                        detectTapGestures(
                            onDoubleTap = {
                                gestureZoom = 1f
                                panX = 0f
                                panY = 0f
                                panFilter.setTransform(1f, 0f, 0f)
                            }
                        )
                    }
                    .onSizeChanged { size ->
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

private fun getNativeSensorResolution(activity: ComponentActivity, isFrontCamera: Boolean): String {
    return try {
        val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val frontCameraId = cameraManager.cameraIdList.firstOrNull { cameraId ->
            cameraManager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.LENS_FACING) == if (isFrontCamera) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
        } ?: return "Indisponible (caméra ${if (isFrontCamera) "avant" else "arrière"} introuvable)"
        val size = cameraManager.getCameraCharacteristics(frontCameraId)
            .get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            ?: return "Indisponible"
        formatResolution(size.width, size.height)
    } catch (error: Exception) {
        Log.e("MiroirCamera", "Impossible de lire la résolution native du capteur", error)
        "Indisponible"
    }
}
