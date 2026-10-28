package ar.uade.cine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ar.uade.cine.infrastructure.importador.CatalogoDePrueba;
import ar.uade.cine.model.rechazos.Rechazo;

@SpringBootTest
@ActiveProfiles("test")
public abstract class PruebaDeIntegracion {

    @Autowired
    private LimpiezaDeBase limpieza;

    @Autowired
    private CatalogoDePrueba catalogoExterno;

    @Autowired
    protected ConfiguracionDePrueba.RelojMovible reloj;

    // El mensaje es contrato: llega tal cual al usuario, y Rechazo abarca también el 404.
    protected static Rechazo rechaza(String mensaje, Executable accion) {
        Rechazo rechazo = assertThrows(Rechazo.class, accion);
        assertEquals(mensaje, rechazo.getMessage());
        return rechazo;
    }

    @BeforeEach
    void dejarLaBaseComoNueva() {
        limpieza.limpiar();
        catalogoExterno.reiniciar();
        reloj.reiniciar();
    }
}
