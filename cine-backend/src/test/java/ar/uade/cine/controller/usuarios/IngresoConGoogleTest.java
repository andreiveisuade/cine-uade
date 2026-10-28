package ar.uade.cine.controller.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;

import ar.uade.cine.PruebaDeApi;

class IngresoConGoogleTest extends PruebaDeApi {

    private static final String GOOGLE = "https://accounts.google.com/o/oauth2/v2/auth";

    @LocalServerPort
    private int puerto;

    @Test
    @DisplayName("la web pregunta sin sesión si el ingreso con Google está disponible")
    void elIngresoConGoogleEstaDisponible() {
        Respuesta respuesta = pedirComo(HttpMethod.GET, "/api/sesion/google", null, null, null);

        assertThat(respuesta.estado()).isEqualTo(200);
        assertThat(respuesta.json().get("disponible").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("el botón lleva a Google con openid, email y profile, y la vuelta al backend")
    void elBotonLlevaAGoogle() throws Exception {
        String destino = ubicacion(sinSeguir("/oauth2/authorization/google", null, null));

        assertThat(destino).startsWith(GOOGLE).contains("scope=openid email profile")
                .contains("redirect_uri=http://localhost:" + puerto + "/login/oauth2/code/google");
    }

    // Render termina el TLS en su proxy: sin forward-headers-strategy, Google recibiría una vuelta http.
    @Test
    @DisplayName("detrás de un proxy https, la vuelta que se le pide a Google también es https")
    void detrasDelProxyLaVueltaEsHttps() throws Exception {
        String destino = ubicacion(sinSeguir("/oauth2/authorization/google", "https", "cine.example"));

        assertThat(destino).contains("redirect_uri=https://cine.example/login/oauth2/code/google");
    }

    // La sesión nace al ir a Google (guarda el pedido) y es la misma cookie que después queda autenticada.
    @Test
    @DisplayName("la cookie de la sesión es HttpOnly y SameSite=Lax: la vuelta de Google es otro sitio")
    void laCookieDeLaSesion() throws Exception {
        String cookie = sinSeguir("/oauth2/authorization/google", null, null).headers()
                .firstValue(HttpHeaders.SET_COOKIE).orElseThrow();

        assertThat(cookie).startsWith("JSESSIONID=").contains("HttpOnly").contains("SameSite=Lax");
    }

    @Test
    @DisplayName("si el ingreso falla vuelve a #/ingresar con el motivo, sin sesión")
    void unIngresoFallidoVuelveALaWeb() throws Exception {
        HttpResponse<String> vuelta = sinSeguir("/login/oauth2/code/google?code=abc&state=xyz", null, null);

        assertThat(vuelta.statusCode()).isEqualTo(302);
        assertThat(ubicacion(vuelta)).isEqualTo("http://localhost:" + puerto
                + "/#/ingresar?error=No se pudo ingresar con Google: volvé a intentarlo");
    }

    @Test
    @DisplayName("R28: con la sesión de Google el cliente es CLIENTE y ve sus reservas")
    void laSesionDeGoogleEsDeCliente() {
        MockHttpSession sesion = entrarConGoogle("ana@mail.com");

        Respuesta quien = conSesion(HttpMethod.POST, "/api/sesion", sesion, null);

        assertThat(quien.estado()).isEqualTo(200);
        assertThat(quien.json().get("email").asText()).isEqualTo("ana@mail.com");
        assertThat(quien.json().get("rol").asText()).isEqualTo("CLIENTE");
        assertThat(quien.json().get("empleado").asBoolean()).isFalse();
        assertThat(conSesion(HttpMethod.GET, "/api/reservas/mias", sesion, null).estado()).isEqualTo(200);
    }

    @Test
    @DisplayName("R28: la sesión de Google no entra al panel: lo del encargado es 403")
    void laSesionDeGoogleNoEsDelPanel() {
        MockHttpSession sesion = entrarConGoogle("ana@mail.com");

        assertThat(conSesion(HttpMethod.GET, "/api/reservas", sesion, null).estado()).isEqualTo(403);
        assertThat(conSesion(HttpMethod.POST, "/api/acceso", sesion, "{}").estado()).isEqualTo(403);
    }

    private HttpResponse<String> sinSeguir(String ruta, String protocolo, String host) throws Exception {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta));
        if (protocolo != null) {
            pedido.header("X-Forwarded-Proto", protocolo).header("X-Forwarded-Host", host);
        }
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
                .send(pedido.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String ubicacion(HttpResponse<String> respuesta) {
        return URLDecoder.decode(respuesta.headers().firstValue(HttpHeaders.LOCATION).orElseThrow(),
                StandardCharsets.UTF_8);
    }
}
