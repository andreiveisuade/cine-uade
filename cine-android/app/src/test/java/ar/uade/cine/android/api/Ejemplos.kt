package ar.uade.cine.android.api

object Ejemplos {
    const val RESERVA = """{"id":12,"funcionId":4,"clienteId":2,"estado":"PAGADA","codigo":"K7M2P9XQ",
        "ingresadaEn":"2026-10-22T20:12:00","cantidadEntradas":2,"total":7500.0,"cobrable":false,"cancelable":false,
        "entradas":[{"asientoId":30,"codigo":"C5","tarifa":"GENERAL","requiereAcreditacion":false,"precio":5000.0},
                    {"asientoId":31,"codigo":"C6","tarifa":"JUBILADO","requiereAcreditacion":true,"precio":2500.0}],
        "funcion":{"id":4,"inicio":"2026-10-22T20:30:00","idioma":"ESPANOL"},
        "pelicula":{"id":41,"titulo":"Hablan las aves","duracionMinutos":95},
        "sala":{"id":2,"nombre":"Sala 2","tipo":"DOS_D"}}"""
}
