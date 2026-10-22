package ar.uade.cine.android.api

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

// La clave viaja en cada pedido: por http solo a la propia red (el docker local), nunca a internet.
object Servidor {
    fun validar(texto: String): HttpUrl {
        val url = texto.trim().trimEnd('/').toHttpUrlOrNull() ?: throw Rechazo("La dirección del servidor no es válida")
        if (!url.isHttps && !esLocal(url.host)) throw Rechazo("Fuera de la red local el servidor tiene que ser https")
        return url
    }

    private fun esLocal(host: String): Boolean {
        if (host == "localhost" || host == "10.0.2.2") return true
        val partes = host.split('.').map { it.toIntOrNull() ?: return false }
        if (partes.size != 4) return false
        val (a, b) = partes
        return a == 10 || a == 127 || (a == 172 && b in 16..31) || (a == 192 && b == 168)
    }
}
