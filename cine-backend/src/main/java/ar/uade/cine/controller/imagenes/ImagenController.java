package ar.uade.cine.controller.imagenes;

import java.io.IOException;
import java.time.Duration;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ar.uade.cine.controller.http.Creado;
import ar.uade.cine.dto.imagenes.ImagenSubidaVistaDTO;
import ar.uade.cine.model.imagenes.Imagen;
import ar.uade.cine.service.imagenes.GestorImagenes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas de /api/imagenes: recibe el archivo multipart y sirve los bytes; qué se acepta lo decide Imagen.
@Tag(name = "Imágenes", description = "Las fotos de promociones y productos")
@RestController
@RequiredArgsConstructor
public class ImagenController {

    // El nombre es un uuid nuevo por subida: el archivo no cambia nunca y el navegador lo guarda un año.
    private static final CacheControl INMUTABLE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    private final GestorImagenes imagenes;

    @Operation(summary = "Subir una imagen JPG, PNG o WEBP de hasta 2 MB, en el campo archivo")
    @PostMapping("/api/imagenes")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ImagenSubidaVistaDTO> subir(@RequestParam(required = false) MultipartFile archivo)
            throws IOException {
        String url = imagenes.subir(archivo == null ? null : archivo.getBytes());
        return Creado.en(url, new ImagenSubidaVistaDTO(url));
    }

    @Operation(summary = "Una imagen subida")
    @GetMapping("/api/imagenes/{nombre}")
    public ResponseEntity<byte[]> imagen(@PathVariable String nombre) {
        Imagen imagen = imagenes.obtener(nombre);
        return ResponseEntity.ok().cacheControl(INMUTABLE)
                .contentType(MediaType.parseMediaType(imagen.formato().tipoDeContenido()))
                .body(imagen.contenido());
    }
}
