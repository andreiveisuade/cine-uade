package ar.uade.cine.controller.imagenes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ar.uade.cine.PruebaDeApi;

class ImagenControllerTest extends PruebaDeApi {

    private static final byte[] PNG = Arrays.copyOf(HexFormat.of().parseHex("89504e470d0a1a0a"), 64);

    @Autowired
    private TestRestTemplate cliente;

    private Respuesta subir(String campo, byte[] contenido, String nombreDeArchivo, String email) {
        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        partes.add(campo, new ByteArrayResource(contenido) {
            @Override
            public String getFilename() {
                return nombreDeArchivo;
            }
        });
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (email != null) {
            cabeceras.setBasicAuth(email, CLAVE_ADMIN);
        }
        ResponseEntity<String> respuesta = cliente.exchange(URI.create("/api/imagenes"), HttpMethod.POST,
                new HttpEntity<>(partes, cabeceras), String.class);
        return new Respuesta(respuesta.getStatusCode().value(), respuesta.getBody(), respuesta.getHeaders());
    }

    private Respuesta subir(byte[] contenido, String nombreDeArchivo) {
        return subir("archivo", contenido, nombreDeArchivo, EMAIL_ADMIN);
    }

    private ResponseEntity<byte[]> bajar(String ruta) {
        return cliente.getForEntity(URI.create(ruta), byte[].class);
    }

    @Test
    void laSubidaDevuelveLaUrlYCualquieraLaVeConSuTipoYCacheLargo() {
        Respuesta subida = subir(PNG, "foto.png");

        assertEquals(201, subida.estado());
        String url = subida.json().get("url").asText();
        assertTrue(url.matches("/api/imagenes/[0-9a-f-]{36}\\.png"), url);
        assertEquals(url, subida.cabeceras().getLocation().toString());

        ResponseEntity<byte[]> imagen = bajar(url);
        assertEquals(200, imagen.getStatusCode().value());
        assertEquals(MediaType.IMAGE_PNG, imagen.getHeaders().getContentType());
        assertTrue(imagen.getHeaders().getCacheControl().contains("max-age=31536000"));
        assertArrayEquals(PNG, imagen.getBody());
    }

    // La extensión la pone el sistema según el contenido, no el nombre que trae el archivo.
    @Test
    void laExtensionSaleDelContenidoYNoDelNombre() {
        String url = subir(Arrays.copyOf(HexFormat.of().parseHex("ffd8ffe0"), 64), "foto.png").json()
                .get("url").asText();

        assertTrue(url.endsWith(".jpg"), url);
        assertEquals(MediaType.IMAGE_JPEG, bajar(url).getHeaders().getContentType());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            texto disfrazado de png, 686f6c61206d756e646f, foto.png, La imagen tiene que ser JPG
            gif,                     47494638396101,       foto.gif, La imagen tiene que ser JPG
            archivo vacio,           '',                   foto.png, Falta la imagen
            """)
    void loQueNoEsUnaImagenAceptadaEs400(String caso, String hex, String nombre, String mensaje) {
        Respuesta respuesta = subir(HexFormat.of().parseHex(hex), nombre);

        assertEquals(400, respuesta.estado());
        assertTrue(respuesta.error().startsWith(mensaje), respuesta.error());
    }

    @Test
    void masDeDosMegasEs400() {
        Respuesta respuesta = subir(Arrays.copyOf(PNG, 2 * 1024 * 1024 + 1), "grande.png");

        assertEquals(400, respuesta.estado());
        assertEquals("La imagen tiene que pesar 2 MB como máximo", respuesta.error());
    }

    // Pasado el tope de multipart Spring corta antes de que llegue a Imagen: el mismo 400 y el mismo texto.
    @Test
    void masDeTresMegasCortaSpringConElMismoMensaje() {
        Respuesta respuesta = subir(Arrays.copyOf(PNG, 3 * 1024 * 1024 + 1), "enorme.png");

        assertEquals(400, respuesta.estado());
        assertEquals("La imagen tiene que pesar 2 MB como máximo", respuesta.error());
    }

    @Test
    void sinElCampoArchivoFaltaLaImagen() {
        Respuesta otroCampo = subir("foto", PNG, "foto.png", EMAIL_ADMIN);
        assertEquals(400, otroCampo.estado());
        assertEquals("Falta la imagen", otroCampo.error());

        Respuesta sinCuerpo = post("/api/imagenes", "{}");
        assertEquals(400, sinCuerpo.estado());
        assertEquals("Falta la imagen", sinCuerpo.error());
    }

    @Test
    void subirPideSerAdministrador() {
        assertEquals(401, subir("archivo", PNG, "foto.png", null).estado());
    }

    @Test
    void unaQueNoExisteEs404() {
        Respuesta respuesta = pedirComo(HttpMethod.GET,
                "/api/imagenes/0f8fad5b-d9cb-469f-a165-70867728950e.png", null, null, null);

        assertEquals(404, respuesta.estado());
        assertEquals("No existe la imagen 0f8fad5b-d9cb-469f-a165-70867728950e.png", respuesta.error());
    }

    // Lo que no tiene forma de uuid ni siquiera se busca en disco.
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/api/imagenes/application.yml", "/api/imagenes/..%2Fschema.sql",
            "/api/imagenes/%2e%2e", "/api/imagenes/..%5Cschema.sql"})
    void unNombreQueSaleDeLaCarpetaNoDevuelveNada(String ruta) {
        ResponseEntity<byte[]> respuesta = bajar(ruta);

        assertNotEquals(200, respuesta.getStatusCode().value());
        assertTrue(respuesta.getStatusCode().is4xxClientError());
    }
}
