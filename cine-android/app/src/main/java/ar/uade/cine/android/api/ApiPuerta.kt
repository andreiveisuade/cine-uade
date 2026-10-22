package ar.uade.cine.android.api

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

// Habla con el backend con HTTP Basic en cada pedido, como Swing; la clave vive solo acá, en memoria.
class ApiPuerta(
    servidor: String,
    usuario: String,
    clave: String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val http: OkHttpClient = clienteHttp,
) {
    private val base = servidor.trim().trimEnd('/')
    private val autorizacion = Credentials.basic(usuario.trim(), clave)

    suspend fun iniciarSesion(): Usuario = pedir("/sesion", "")

    suspend fun registrarAcceso(codigo: String): Reserva =
        pedir("/acceso", json.encodeToString(PedidoAcceso.serializer(), PedidoAcceso(codigo)))

    private suspend inline fun <reified T> pedir(ruta: String, cuerpo: String): T = withContext(io) {
        val pedido = Request.Builder()
            .url(base + ruta)
            .header("Authorization", autorizacion)
            .header("Accept", "application/json")
            .post(cuerpo.toRequestBody(JSON))
            .build()
        val texto: String
        val status: Int
        try {
            http.newCall(pedido).execute().use {
                status = it.code
                texto = it.body?.string().orEmpty()
            }
        } catch (e: InterruptedIOException) {
            throw Rechazo("El servidor no respondió a tiempo: volvé a intentarlo")
        } catch (e: IOException) {
            throw Rechazo("No se pudo conectar con el servidor: revisá la dirección y la red")
        } catch (e: IllegalArgumentException) {
            throw Rechazo("La dirección del servidor no es válida")
        }
        if (status !in 200..299) throw Rechazo(mensajeDeError(texto, status), status)
        try {
            json.decodeFromString<T>(texto)
        } catch (e: SerializationException) {
            throw Rechazo("El servidor respondió algo que no se entiende", status)
        } catch (e: IllegalArgumentException) {
            throw Rechazo("El servidor respondió algo que no se entiende", status)
        }
    }

    companion object {
        private val JSON = "application/json".toMediaType()
        internal val json = Json { ignoreUnknownKeys = true }

        // Render free tarda en despertar: los tiempos son largos a propósito.
        private val clienteHttp = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build()

        // Un cuerpo que no es {error} (HTML de un proxy) nunca se muestra crudo.
        internal fun mensajeDeError(texto: String, status: Int): String = try {
            json.decodeFromString(CuerpoError.serializer(), texto).error
        } catch (e: Exception) {
            "El servidor no está disponible (HTTP $status): volvé a intentarlo en un rato"
        }
    }
}
