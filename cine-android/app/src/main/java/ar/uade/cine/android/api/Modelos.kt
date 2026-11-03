package ar.uade.cine.android.api

import kotlinx.serialization.Serializable

// Solo los campos que muestra la Puerta; el resto del JSON se ignora.
@Serializable
data class Usuario(val nombre: String, val empleado: Boolean)

@Serializable
data class Reserva(
    val entradas: List<Entrada> = emptyList(),
    val funcion: Funcion? = null,
    val pelicula: Pelicula? = null,
    val sala: Sala? = null,
    val candy: List<CompraCandy> = emptyList(),
)

@Serializable
data class Entrada(val codigo: String, val tarifa: String, val requiereAcreditacion: Boolean = false)

@Serializable
data class Funcion(val inicio: String)

@Serializable
data class Pelicula(val titulo: String)

@Serializable
data class Sala(val nombre: String)

// Cobrada y sin retiradaEn es lo que falta entregar, como lo lee Swing.
@Serializable
data class CompraCandy(val estado: String? = null, val retiradaEn: String? = null, val items: List<ItemCompra> = emptyList()) {
    val aRetirar: Boolean get() = estado == "COBRADA" && retiradaEn == null
}

@Serializable
data class ItemCompra(val nombre: String, val cantidad: Int)

@Serializable
internal data class CuerpoError(val error: String)

@Serializable
internal data class PedidoCodigo(val codigo: String)
