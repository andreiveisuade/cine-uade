package ar.uade.cine.android.escaner

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

// ML Kit en el teléfono, solo QR: el ticket trae nada más que el código.
class AnalizadorQr(private val alLeer: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val lector = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )

    @ExperimentalGetImage
    override fun analyze(cuadro: ImageProxy) {
        val imagen = cuadro.image ?: return cuadro.close()
        lector.process(InputImage.fromMediaImage(imagen, cuadro.imageInfo.rotationDegrees))
            .addOnSuccessListener { codigos -> codigos.firstNotNullOfOrNull { it.rawValue }?.let(alLeer) }
            .addOnCompleteListener { cuadro.close() }
    }
}
