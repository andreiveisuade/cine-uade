package ar.uade.cine.service.imagenes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import ar.uade.cine.infrastructure.imagenes.AlmacenDeImagenes;
import ar.uade.cine.model.imagenes.Imagen;
import ar.uade.cine.model.imagenes.NombreDeImagen;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;

// Sube y entrega las imágenes de promociones y productos; Controlador: Imagen valida, el almacén guarda.
@Service
@RequiredArgsConstructor
public class GestorImagenes {

    private final AlmacenDeImagenes almacen;

    // Devuelve la URL que después se guarda como imagenUrl.
    public String subir(byte[] contenido) {
        Imagen imagen = Imagen.subida(contenido);
        NombreDeImagen nombre = NombreDeImagen.nuevo(imagen.formato());
        almacen.guardar(nombre, imagen.contenido());
        return nombre.url();
    }

    public Imagen obtener(String nombre) {
        return NombreDeImagen.leer(nombre).flatMap(almacen::leer).map(Imagen::subida)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la imagen " + nombre));
    }
}
