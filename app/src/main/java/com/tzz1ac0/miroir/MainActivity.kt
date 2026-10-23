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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.sources.audio.NoAudioSource
import com.pedro.library.generic.GenericStream
import com.pedro.library.view.RenderErrorCallback
import com.tzz1ac0.miroir.camera.AutoExposureCameraXSource
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
    var useRootEncoder by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var settingsDialogVisible by remember { mutableStateOf(false) }
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
                    Text {
                        append("Miroir — ")
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(if (useRootEncoder) "RootEncoder" else "CameraX")
                        }
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
                Button(onClick = { useRootEncoder = !useRootEncoder }) {
                    Text(if (useRootEncoder) "Revenir à l’aperçu CameraX" else "Tester l’aperçu RootEncoder")
                }
                if (useRootEncoder) {
                    RootEncoderCameraPreview(
                        activity = activity,
                        modifier = Modifier.weight(1f),
                        onStreamResolutionChanged = { videoStreamResolution = it },
                        onDisplayedResolutionChanged = { displayedImageResolution = it },
                        onSurfaceResolutionChanged = { availableSurfaceResolution = it }
                    )
                } else {
                    CameraXCameraPreview(
                        activity = activity,
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
                Column {
                    Text("Résolution du flux vidéo d’entrée : $videoStreamResolution")
                    Text(
                        "Provenance : " + if (useRootEncoder) {
                            "RootEncoder — GenericStream.prepareVideo(640 × 480), source caméra AutoExposureCameraXSource"
                        } else {
                            "CameraX — Preview.resolutionInfo (résolution négociée avec la caméra)"
                        }
                    )
                    Text("Résolution de l’image affichée : $displayedImageResolution")
                    Text("Mesure : taille du composant AndroidView dans Compose")
                    Text("Surface d’affichage disponible dans l’interface : $availableSurfaceResolution")
                    Text("Mesure : taille du conteneur d’aperçu dans Compose")
                    Text("Résolution native du capteur : $nativeSensorResolution")
                    Text("Source : Camera2 CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE (caméra avant)")
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
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    Column(
        modifier = modifier.fillMaxWidth().onSizeChanged { size ->
            onSurfaceResolutionChanged("${size.width} × ${size.height} px")
        }
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.weight(1f).fillMaxWidth().onSizeChanged { size ->
                onDisplayedResolutionChanged("${size.width} × ${size.height} px")
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
                provider?.bindToLifecycle(activity, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
                preview.resolutionInfo?.resolution?.let { resolution ->
                    onStreamResolutionChanged("${resolution.width} × ${resolution.height} px")
                }
            } catch (error: Exception) {
                Log.e("MiroirCamera", "Échec de démarrage de la caméra CameraX", error)
            }
        }, ContextCompat.getMainExecutor(activity))
        onDispose {
            disposed = true
            provider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }
}

/** Opt-in experiment. The validated CameraX path remains the default and is not changed. */
@Composable
private fun RootEncoderCameraPreview(
    activity: ComponentActivity,
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
    DisposableEffect(stream, source) {
        try {
            stream.getGlInterface().setRenderErrorCallback(object : RenderErrorCallback {
                override fun onRenderError(error: RuntimeException) {
                }
            })
            stream.getGlInterface().autoHandleOrientation = true
            prepared = stream.prepareVideo(width = 640, height = 480, bitrate = 1_200_000, fps = 30)
            onStreamResolutionChanged(if (prepared) "640 × 480 px" else "Indisponible")
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
            onSurfaceResolutionChanged("${size.width} × ${size.height} px")
        }
    ) {
        if (prepared) {
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
                            }
                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                            }
                        })
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth().onSizeChanged { size ->
                    onDisplayedResolutionChanged("${size.width} × ${size.height} px")
                }
            )
        }
    }
}

fun greetingFor(name: String = "World"): String = "Hello $name!"


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
        "${size.width} × ${size.height} px"
    } catch (error: Exception) {
        Log.e("MiroirCamera", "Impossible de lire la résolution native du capteur", error)
        "Indisponible"
    }
}
