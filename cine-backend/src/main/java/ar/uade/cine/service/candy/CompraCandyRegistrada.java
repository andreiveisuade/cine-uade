package ar.uade.cine.service.candy;

// Aviso de candy vendido; evento del Observer que publica GestorCandy y oye ComprobantesDeCandy.
// Solo el id: el que escucha corre después del commit, con otra sesión, y relee la compra.
public record CompraCandyRegistrada(int compraId) {
}
