package ar.uade.cine.service.candy;

// Aviso de que se vendió candy; evento del Observer que publica GestorCandy y oye ComprobantesDeCandy.
// Solo el id: el que escucha corre después del commit, con otra sesión, y relee la compra.
public record CompraCandyRegistrada(int compraId) {
}
