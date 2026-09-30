package ar.uade.cine;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ar.uade.cine.infrastructure.importador.CatalogoDePrueba;

@SpringBootTest
@ActiveProfiles("test")
public abstract class PruebaDeIntegracion {

    @Autowired
    private LimpiezaDeBase limpieza;

    @Autowired
    private CatalogoDePrueba catalogoExterno;

    @Autowired
    protected ConfiguracionDePrueba.RelojMovible reloj;

    @BeforeEach
    void dejarLaBaseComoNueva() {
        limpieza.limpiar();
        catalogoExterno.reiniciar();
        reloj.reiniciar();
    }
}
