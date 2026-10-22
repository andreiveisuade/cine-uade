package ar.uade.cine.android.resultado

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

// Un pulso corto si pasa, tres largos si no: el acomodador no necesita mirar la pantalla.
object Vibracion {
    fun adelante(contexto: Context) = vibrar(contexto, longArrayOf(0, 120))
    fun noPasa(contexto: Context) = vibrar(contexto, longArrayOf(0, 300, 150, 300, 150, 300))

    @Suppress("DEPRECATION")
    private fun vibrar(contexto: Context, patron: LongArray) {
        contexto.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createWaveform(patron, -1))
    }
}
