package ar.uade.cine;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ar.uade.cine.infrastructure.bloqueos.BloqueoButacas;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacasMemoria;
import ar.uade.cine.infrastructure.importador.CatalogoDePrueba;

/**
 * La base de los tests que necesitan el sistema armado: el contexto de Spring completo,
 * contra H2, con la base limpia antes de cada prueba.
 *
 * <p>Reemplaza al armado a mano que hacía cada test —trece DAO en memoria y los gestores
 * cableados uno por uno en el {@code @BeforeEach}— por lo que ya sabe hacer el contenedor.
 * Además de ahorrar el andamiaje, esto prueba algo que antes no se probaba: que el sistema
 * <em>levanta</em>. Si un gestor le pide al contenedor algo que nadie declara, la suite
 * entera falla al arrancar en vez de descubrirse al abrir la aplicación.
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
    private ConfiguracionDePrueba.Reloj relojDeLosBloqueos;

    @BeforeEach
    void dejarLaBaseComoNueva() {
        limpieza.limpiar();
        catalogoExterno.reiniciar();
        // El adaptador de bloqueos y su reloj son beans, o sea uno solo para toda la suite:
        // sin esto, la butaca que un test dejó elegida le aparece tomada al siguiente.
        ((BloqueoButacasMemoria) bloqueoButacas).limpiar();
        relojDeLosBloqueos.reiniciar();
    }
}
