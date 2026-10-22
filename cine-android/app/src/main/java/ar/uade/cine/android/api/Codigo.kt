package ar.uade.cine.android.api

// El QR o el teclado pueden traer minúsculas o espacios; el backend valida el resto.
object Codigo {
    fun normalizar(texto: String): String = texto.filterNot { it.isWhitespace() }.uppercase()
}
