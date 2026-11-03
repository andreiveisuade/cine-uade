package ar.uade.cine.android.api

object Ejemplos {
    const val RESERVA = """{"id":12,"funcionId":4,"clienteId":2,"estado":"PAGADA","codigo":"K7M2P9XQ",
        "ingresadaEn":"2026-10-22T20:12:00","cantidadEntradas":2,"total":7500.0,"cobrable":false,"cancelable":false,
        "entradas":[{"asientoId":30,"codigo":"C5","tarifa":"GENERAL","requiereAcreditacion":false,"precio":5000.0},
                    {"asientoId":31,"codigo":"C6","tarifa":"JUBILADO","requiereAcreditacion":true,"precio":2500.0}],
        "funcion":{"id":4,"inicio":"2026-10-22T20:30:00","idioma":"ESPANOL"},
        "pelicula":{"id":41,"titulo":"Hablan las aves","duracionMinutos":95},
        "sala":{"id":2,"nombre":"Sala 2","tipo":"DOS_D"},
        "candy":[{"id":7,"reservaId":12,"estado":"COBRADA","items":[{"nombre":"Combo clásico","cantidad":1,"subtotal":5500.0}]},
                 {"id":8,"reservaId":12,"estado":"COBRADA","retiradaEn":"2026-10-22T20:05:00",
                  "items":[{"nombre":"Agua","cantidad":1,"subtotal":1500.0}]}]}"""

    const val CANDY_RETIRADO = """[{"id":7,"reservaId":12,"estado":"COBRADA","retiradaEn":"2026-10-22T20:14:00","total":8500.0,
        "items":[{"nombre":"Agua","cantidad":2,"subtotal":3000.0},{"nombre":"Combo clásico","cantidad":1,"subtotal":5500.0}]}]"""
}
