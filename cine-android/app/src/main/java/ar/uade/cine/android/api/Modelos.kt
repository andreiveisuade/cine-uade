package ar.uade.cine.android.api

import kotlinx.serialization.Serializable

// Solo los campos que muestra la Puerta; el resto del JSON se ignora.
@Serializable
data class Usuario(val nombre: String, val rol: String)

@Serializable
data class Reserva(
    val codigo: String,
    val ingresadaEn: String? = null,
    val entradas: List<Entrada> = emptyList(),
    val funcion: Funcion? = null,
    val pelicula: Pelicula? = null,
    val sala: Sala? = null,
)

@Serializable
data class Entrada(val codigo: String, val tarifa: String)

@Serializable
data class Funcion(val inicio: String)

@Serializable
data class Pelicula(val titulo: String)

@Serializable
data class Sala(val nombre: String)

@Serializable
internal data class CuerpoError(val error: String)

@Serializable
internal data class PedidoAcceso(val codigo: String)
