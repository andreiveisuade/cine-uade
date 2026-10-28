package ar.uade.cine.controller.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// En Render el backend sirve el build del front; acá, la misma página desde public/, que Vite copia tal cual.
// Google la pide para publicar el ingreso: sin sesión y sin JS, así que tiene que ser 200 con el HTML.
@SpringBootTest(properties = "spring.web.resources.static-locations=file:../cine-frontend/public/")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaginaDePrivacidadTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("la política de privacidad se sirve sin sesión, como el resto del front")
    void laPoliticaEsPublica() throws Exception {
        mvc.perform(get("/privacidad.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                // Sin charset en la cabecera: el navegador lo toma del <meta charset>.
                .andExpect(pagina -> assertThat(pagina.getResponse().getContentAsString(StandardCharsets.UTF_8))
                        .contains("<h1>Política de privacidad</h1>"));
    }
}
