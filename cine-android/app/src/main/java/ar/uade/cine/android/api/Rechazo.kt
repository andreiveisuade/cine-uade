package ar.uade.cine.android.api

// Lo que no salió: el mensaje ya está listo para mostrar tal cual.
class Rechazo(message: String, val status: Int? = null) : Exception(message)
