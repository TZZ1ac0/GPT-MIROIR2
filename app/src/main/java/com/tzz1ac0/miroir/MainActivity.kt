package com.tzz1ac0.miroir

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
    val permissionRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Miroir") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
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
                    Text(if (useRootEncoder) "Revenir à l’aperçu CameraX validé" else "Tester l’aperçu RootEncoder (diagnostic)")
                }
                if (useRootEncoder) {
                    RootEncoderCameraPreview(activity = activity, modifier = Modifier.weight(1f))
                } else {
                    CameraXCameraPreview(activity = activity, modifier = Modifier.weight(1f))
                }
            } else {
                Text("L’autorisation caméra est nécessaire pour démarrer la capture.")
                Button(onClick = { permissionRequest.launch(Manifest.permission.CAMERA) }) {
                    Text("Autoriser la caméra")
                }
            }
        }
    }
}

/** Baseline validated on the target phone: keep this path independent from RootEncoder. */
@Composable
private fun CameraXCameraPreview(activity: ComponentActivity, modifier: Modifier = Modifier) {
    val previewView = remember {
        PreviewView(activity).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var frameCount by remember { mutableIntStateOf(0) }
    var cameraStatus by remember { mutableStateOf("Initialisation de la caméra…") }

    Column(modifier = modifier.fillMaxWidth()) {
        AndroidView(factory = { previewView }, modifier = Modifier.weight(1f).fillMaxWidth())
        Text(cameraStatus)
        Text("Images reçues : $frameCount")
        Text("Mode de référence : CameraX PreviewView, exposition automatique demandée.")
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
                var count = 0
                analysis.setAnalyzer(analysisExecutor) { image ->
                    count++
                    if (count % 15 == 0) {
                        val snapshot = count
                        activity.runOnUiThread {
                            if (!disposed) {
                                frameCount = snapshot
                                cameraStatus = "Caméra active"
                            }
                        }
                    }
                    image.close()
                }
                provider?.unbindAll()
                provider?.bindToLifecycle(activity, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
                cameraStatus = "Caméra avant active — exposition automatique demandée"
            } catch (error: Exception) {
                cameraStatus = "Échec de démarrage : ${error.localizedMessage ?: "erreur caméra"}"
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
private fun RootEncoderCameraPreview(activity: ComponentActivity, modifier: Modifier = Modifier) {
    var status by remember { mutableStateOf("Initialisation de RootEncoder…") }
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
            status = if (prepared) "prepareVideo=true — attente de SurfaceView" else "prepareVideo=false"
        } catch (error: Exception) {
            prepared = false
            status = "Échec prepareVideo : ${error.localizedMessage ?: "erreur vidéo"}"
        }
        onDispose {
            if (stream.isOnPreview) stream.stopPreview()
            stream.release()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (prepared) {
            AndroidView(
                factory = { context ->
                    SurfaceView(context).apply {
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                status = "SurfaceView créée — démarrage RootEncoder"
                                try {
                                    stream.startPreview(this@apply, true)
                                    post {
                                        status = "startPreview appelé; sourceRunning=${source.isRunning()}"
                                    }
                                } catch (error: Exception) {
                                    status = "Échec startPreview : ${error.localizedMessage ?: "erreur"}"
                                }
                            }
                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                            }
                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                            }
                        })
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
        Text(status)
        Text("Mode de test uniquement : aucune diffusion RTSP n’est démarrée.")
        Text("Source active : ${source.isRunning()} ; aperçu RootEncoder : ${stream.isOnPreview}")
    }
}

fun greetingFor(name: String = "World"): String = "Hello $name!"
