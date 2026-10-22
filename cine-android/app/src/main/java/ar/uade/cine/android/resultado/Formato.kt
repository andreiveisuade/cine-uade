package ar.uade.cine.android.resultado

// El backend manda enums y fechas ISO; acá solo se traducen para leer de un vistazo.
object Formato {
    fun hora(iso: String?): String = iso?.substringAfter('T')?.take(5).orEmpty()

    fun tarifa(nombre: String): String = nombre.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

    fun personas(cantidad: Int): String = if (cantidad == 1) "1 persona" else "$cantidad personas"
}
