package ar.uade.cine.android.escaner

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun Camara(alLeer: (String) -> Unit, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val vista = remember { PreviewView(contexto) }
    val hilo = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(ciclo) {
        val futuro = ProcessCameraProvider.getInstance(contexto)
        futuro.addListener({
            val proveedor = futuro.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = vista.surfaceProvider }
            val analisis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(hilo, AnalizadorQr(alLeer)) }
            proveedor.unbindAll()
            proveedor.bindToLifecycle(ciclo, CameraSelector.DEFAULT_BACK_CAMERA, preview, analisis)
        }, ContextCompat.getMainExecutor(contexto))
        onDispose {
            if (futuro.isDone) futuro.get().unbindAll()
            hilo.shutdown()
        }
    }
    AndroidView({ vista }, modifier)
}
