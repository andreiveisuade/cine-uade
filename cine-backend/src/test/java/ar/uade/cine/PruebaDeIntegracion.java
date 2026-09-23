package ar.uade.cine;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ar.uade.cine.infrastructure.bloqueos.BloqueoButacas;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacasMemoria;
import ar.uade.cine.infrastructure.importador.CatalogoDePrueba;

/**
 * Contexto de Spring completo contra H2, con la base limpia antes de cada prueba. Si un
 * gestor pide algo que nadie declara, la suite falla al arrancar.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class PruebaDeIntegracion {

    @Autowired
    private LimpiezaDeBase limpieza;

    @Autowired
    private CatalogoDePrueba catalogoExterno;

    @Autowired
    private BloqueoButacas bloqueoButacas;

    @Autowired
    protected ConfiguracionDePrueba.RelojMovible reloj;

    @BeforeEach
    void dejarLaBaseComoNueva() {
        limpieza.limpiar();
        catalogoExterno.reiniciar();
        // Bloqueos y reloj son beans compartidos por toda la suite.
        ((BloqueoButacasMemoria) bloqueoButacas).limpiar();
        reloj.reiniciar();
    }
}
