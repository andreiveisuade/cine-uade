package ar.uade.cine.controller.usuarios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Como MP sin token o TMDB sin clave: sin GOOGLE_CLIENT_ID el sistema levanta y la web avisa que no se puede ingresar.
@SpringBootTest(properties = {"cine.google.client-id=", "cine.google.client-secret="})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SinGoogleTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("sin credenciales de Google levanta igual y el ingreso figura como no disponible")
    void sinCredencialesElIngresoNoEstaDisponible() throws Exception {
        mvc.perform(get("/api/sesion/google"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false));
    }

    @Test
    @DisplayName("sin credenciales, ir a Google es un 401 en JSON y no un 500")
    void sinCredencialesNoSeVaAGoogle() throws Exception {
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Hace falta iniciar sesión para esta operación"));
    }
}
