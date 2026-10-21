package ar.uade.cine.swing.api.dto.imagenes;

// Lo que contesta el backend al subir una imagen: la URL que después se guarda en la promo o el producto.
public record Imagen(String url) {
}
