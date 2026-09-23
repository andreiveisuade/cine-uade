package ar.uade.cine.service.cartelera;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.repository.ProgramacionRepository;
import ar.uade.cine.model.programaciones.Programacion;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.PeliculaRepository;
import ar.uade.cine.repository.SalaRepository;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.service.cartelera.GestorRevisionCartelera;

class GestorCarteleraTest extends PruebaDeIntegracion {

    @Autowired
    private FuncionRepository funcionRepository;
    @Autowired
    private PeliculaRepository peliculaRepository;
    @Autowired
    private GestorCartelera gestor;
    @Autowired
    private ProgramacionRepository programacionRepository;

    @Autowired
    private GestorRevisionCartelera revision;
    @Autowired
    private SalaRepository salaRepository;

    private Sala sala;

    private void programarProxima(int peliculaId) {
        programar(peliculaId, reloj.ahora().plusDays(7));
    }

    private void programarPasada(int peliculaId) {
        programar(peliculaId, reloj.ahora().minusDays(1));
    }

    // Por el repositorio: el gestor no deja programar en el pasado ni sin confirmar.
    private void programar(int peliculaId, LocalDateTime inicio) {
        if (sala == null) {
            sala = salaRepository.save(new Sala("Sala 1", TipoSala.DOS_D, 15));
        }
        funcionRepository.save(new Funcion(peliculaRepository.findById(peliculaId).orElseThrow(), sala,
                inicio, Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
    }

    @Test
    void agregaYLista() {
        gestor.agregar("El Padrino", 175, List.of(Genero.DRAMA), Clasificacion.ATP);
        assertEquals(1, gestor.listar().size());
        assertTrue(gestor.buscar(1).isPresent());
    }

    @Test
    void rechazaTituloRepetido() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        assertThrows(IllegalArgumentException.class,
                () -> gestor.agregar("matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP));
    }

    @Test
    void rechazaDuracionInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> gestor.agregar("Sin duración", 0, List.of(Genero.DRAMA), Clasificacion.ATP));
    }

    @Test
    void rechazaPeliculaSinGenero() {
        assertThrows(IllegalArgumentException.class,
                () -> gestor.agregar("Sin género", 100, List.of(), Clasificacion.ATP));
    }

    @Test
    void guardaLaClasificacionYSuEtiqueta() {
        gestor.agregar("Terrifier", 100, List.of(Genero.TERROR), Clasificacion.MAS_18);

        Pelicula pelicula = gestor.buscar(1).orElseThrow();
        assertEquals(Clasificacion.MAS_18, pelicula.getClasificacion());
        assertEquals(18, pelicula.getClasificacion().getEdadMinima());
        assertEquals("+18", pelicula.getClasificacion().getEtiqueta());
        assertEquals("ATP", Clasificacion.ATP.getEtiqueta());
    }

    @Test
    void rechazaPeliculaSinClasificacion() {
        assertThrows(IllegalArgumentException.class,
                () -> gestor.agregar("Sin clasificar", 100, List.of(Genero.DRAMA), null));
    }

    @Test
    void losDatosDeCatalogoSeCarganDespuesDelAlta() {
        Pelicula pelicula = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        pelicula.setDirector("Denis Villeneuve");
        pelicula.setAnio(2021);
        pelicula.setIdiomaOriginal("Inglés");
        gestor.actualizar(pelicula);

        Pelicula leida = gestor.buscar(pelicula.getId()).orElseThrow();
        assertEquals("Denis Villeneuve", leida.getDirector());
        assertEquals(2021, leida.getAnio());
    }

    @Test
    void laCarteleraExcluyeLasPeliculasDadasDeBaja() {
        Pelicula vieja = gestor.agregar("Titanic", 194, List.of(Genero.ROMANCE), Clasificacion.MAS_13);
        Pelicula actual = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        programarProxima(vieja.getId());
        programarProxima(actual.getId());

        vieja.setEnCartelera(false);
        gestor.actualizar(vieja);

        assertEquals(2, gestor.listar().size());
        assertEquals(1, gestor.listarEnCartelera().size());
        assertEquals("Dune", gestor.listarEnCartelera().get(0).getTitulo());
    }

    @Test
    void unaPeliculaSinFuncionesNoEstaEnCartelera() {
        Pelicula pelicula = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        assertTrue(pelicula.estaEnCartelera(), "el flag arranca en true");
        assertEquals(0, gestor.listarEnCartelera().size(), "pero sin funciones no está en cartelera");
    }

    @Test
    void programarUnaFuncionLaPoneEnCartelera() {
        Pelicula pelicula = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        programarProxima(pelicula.getId());

        assertEquals(1, gestor.listarEnCartelera().size());
    }

    @Test
    void saleDeCarteleraSolaCuandoSusFuncionesQuedaronAtras() {
        Pelicula pelicula = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        programarPasada(pelicula.getId());

        assertTrue(pelicula.estaEnCartelera(), "nadie tocó el flag");
        assertEquals(0, gestor.listarEnCartelera().size(), "y aun así ya no está en cartelera");
    }

    @Test
    void conUnaFuncionPasadaYOtraFuturaSigueEnCartelera() {
        Pelicula pelicula = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        programarPasada(pelicula.getId());
        programarProxima(pelicula.getId());

        assertEquals(1, gestor.listarEnCartelera().size());
    }

    @Test
    void noSePuedeEditarUnaPeliculaParaQueQuedeConElTituloDeOtra() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Pelicula dune = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        dune.actualizar("Matrix", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        assertThrows(IllegalArgumentException.class, () -> gestor.actualizar(dune));
    }

    @Test
    void editarSinCambiarElTituloNoChocaConsigoMisma() {
        Pelicula dune = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        dune.setDirector("Denis Villeneuve");

        assertDoesNotThrow(() -> gestor.actualizar(dune));
    }

    @Test
    void editarSoloPisaLoQueVieneEnElPedido() {
        Pelicula dune = gestor.agregar(new DatosPelicula("Dune", 155,
                List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13, "Denis Villeneuve",
                "Arrakis", 2021, "Inglés", "dune.jpg", true, 8.1, 1200));

        gestor.editar(dune.getId(), new DatosPelicula(null, null, null, null,
                null, "Otra sinopsis", null, null, null, null, null, null));

        Pelicula leida = gestor.buscar(dune.getId()).orElseThrow();
        assertEquals("Otra sinopsis", leida.getSinopsis());
        assertEquals("Dune", leida.getTitulo());
        assertEquals(155, leida.getDuracionMinutos());
        assertEquals("Denis Villeneuve", leida.getDirector());
        assertEquals(2021, leida.getAnio());
        assertEquals(List.of(Genero.CIENCIA_FICCION), leida.getGeneros());
        assertEquals(Clasificacion.MAS_13, leida.getClasificacion());
    }

    @Test
    void elAltaCompletaGuardaElCatalogoDeUnaSolaVez() {
        Pelicula matrix = gestor.agregar(new DatosPelicula("Matrix", 136, List.of(Genero.ACCION),
                Clasificacion.MAS_13, "Wachowski", "Un hacker", 1999, "Inglés", "matrix.jpg", false, 8.7, 4300));

        Pelicula leida = gestor.buscar(matrix.getId()).orElseThrow();
        assertEquals("Wachowski", leida.getDirector());
        assertEquals(1999, leida.getAnio());
        assertFalse(leida.estaEnCartelera());
        assertTrue(gestor.listarEnCartelera().isEmpty());
    }

    @Test
    void editarNoDejaRenombrarConElTituloDeOtra() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Pelicula dune = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        assertThrows(IllegalArgumentException.class, () -> gestor.editar(dune.getId(),
                new DatosPelicula("Matrix", null, null, null, null, null, null, null, null, null, null, null)));
    }

    @Test
    void editarNoDejaUnaPeliculaSinTituloNiConDuracionCero() {
        Pelicula dune = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        assertThrows(IllegalArgumentException.class, () -> gestor.editar(dune.getId(),
                new DatosPelicula("  ", null, null, null, null, null, null, null, null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> gestor.editar(dune.getId(),
                new DatosPelicula(null, 0, null, null, null, null, null, null, null, null, null, null)));
    }

    @Test
    void editarUnaPeliculaInexistenteFalla() {
        assertThrows(IllegalArgumentException.class,
                () -> gestor.editar(99, new DatosPelicula(null, null, null, null,
                        "Alguien", null, null, null, null, null, null, null)));
    }

    @Test
    void filtraPorGenero() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION, Genero.CIENCIA_FICCION), Clasificacion.ATP);
        gestor.agregar("Amelie", 122, List.of(Genero.ROMANCE), Clasificacion.ATP);

        assertEquals(1, gestor.buscar(null, Genero.CIENCIA_FICCION, null).size());
        assertEquals("Matrix", gestor.buscar(null, Genero.ACCION, null).get(0).getTitulo());
    }

    private void cargarCatalogo() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION, Genero.CIENCIA_FICCION), Clasificacion.ATP);
        gestor.agregar("Matrix Reloaded", 138, List.of(Genero.ACCION), Clasificacion.MAS_13);
        gestor.agregar("El Resplandor", 146, List.of(Genero.TERROR), Clasificacion.MAS_18);
    }

    @Test
    void buscarSinCriteriosDevuelveTodo() {
        cargarCatalogo();

        assertEquals(3, gestor.buscar(null, null, null).size());
        assertEquals(3, gestor.buscar("", null, null).size());
    }

    @Test
    void buscarPorTituloEsParcialYNoDistingueMayusculas() {
        cargarCatalogo();

        assertEquals(2, gestor.buscar("matrix", null, null).size());
        assertEquals(2, gestor.buscar("MATRIX", null, null).size());
        assertEquals(1, gestor.buscar("reloaded", null, null).size());
        assertEquals(1, gestor.buscar("esplandor", null, null).size());
    }

    @Test
    void buscarPorGeneroYPorEstado() {
        cargarCatalogo();

        assertEquals(2, gestor.buscar(null, Genero.ACCION, null).size());
        assertEquals(3, gestor.buscar(null, null, true).size(), "el alta las publica");

        Pelicula resplandor = gestor.buscar(3).orElseThrow();
        resplandor.setEnCartelera(false);
        gestor.actualizar(resplandor);

        assertEquals(2, gestor.buscar(null, null, true).size());
        assertEquals(1, gestor.buscar(null, null, false).size());
        assertEquals(3, gestor.buscar(null, null, null).size(), "null es todas, no ninguna");
    }

    @Test
    void losCriteriosSeCombinan() {
        cargarCatalogo();

        assertEquals(1, gestor.buscar("matrix", Genero.CIENCIA_FICCION, true).size());
        assertTrue(gestor.buscar("matrix", Genero.TERROR, null).isEmpty(),
                "ninguna Matrix es de terror: combinar tiene que poder dar vacío");
    }

    @Test
    void buscarSinCoincidenciasDevuelveVacioYNoFalla() {
        cargarCatalogo();

        assertTrue(gestor.buscar("titanic", null, null).isEmpty());
    }

    private DatosPelicula deTmdb(String titulo) {
        return DatosPelicula.deAlta(titulo, 120, List.of(Genero.ACCION), Clasificacion.ATP);
    }

    @Test
    void loQueCargaElEncargadoNaceConfirmado() {
        Pelicula pelicula = gestor.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);

        assertEquals(EstadoRevision.CONFIRMADA, pelicula.getEstadoRevision());
        assertTrue(pelicula.estaEnCartelera());
    }

    @Test
    void loQueTraeElImportadorNacePendienteYFueraDeCartelera() {
        Pelicula pelicula = revision.importar(deTmdb("Dune"));

        assertEquals(EstadoRevision.PENDIENTE, pelicula.getEstadoRevision());
        assertFalse(pelicula.estaEnCartelera(),
                "una película que nadie confirmó no puede estar ofreciéndose al cliente");
    }

    @Test
    void elBuzonSoloTraeLasPendientes() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        revision.importar(deTmdb("Dune"));
        revision.importar(deTmdb("Oppenheimer"));

        assertEquals(2, revision.listarPendientes().size());
    }

    @Test
    void confirmarLaSacaDelBuzonYLaPoneEnCartelera() {
        Pelicula importada = revision.importar(deTmdb("Dune"));

        Pelicula confirmada = revision.confirmar(importada.getId());

        assertEquals(EstadoRevision.CONFIRMADA, confirmada.getEstadoRevision());
        assertTrue(confirmada.estaEnCartelera());
        assertTrue(revision.listarPendientes().isEmpty());
    }

    @Test
    void descartarLaGuardaEnVezDeBorrarla() {
        Pelicula importada = revision.importar(deTmdb("Dune"));

        revision.descartar(importada.getId());

        assertEquals(EstadoRevision.DESCARTADA,
                gestor.buscar(importada.getId()).orElseThrow().getEstadoRevision());
        assertTrue(revision.listarPendientes().isEmpty());
    }

    @Test
    void noSePuedeDescartarUnaPeliculaConFuncionesProgramadas() {
        Pelicula importada = revision.importar(deTmdb("Dune"));
        revision.confirmar(importada.getId());
        programarProxima(importada.getId());

        assertThrows(IllegalArgumentException.class, () -> revision.descartar(importada.getId()));
    }

    @Test
    void borraLaQueNoDejoRastro() {
        Pelicula pelicula = gestor.agregar("Sin Estrenar", 100, List.of(Genero.DRAMA),
                Clasificacion.ATP);

        gestor.eliminar(pelicula.getId());

        assertTrue(gestor.listar().isEmpty());
    }

    @Test
    void noBorraLaQueTieneFunciones() {
        Pelicula pelicula = gestor.agregar("Matrix", 136, List.of(Genero.ACCION),
                Clasificacion.MAS_13);
        programarProxima(pelicula.getId());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> gestor.eliminar(pelicula.getId()));

        assertTrue(e.getMessage().contains("Matrix"), e.getMessage());
        assertEquals(1, gestor.listar().size());
    }

    @Test
    void noBorraLaProgramadaAunqueNoTengaFunciones() {
        Pelicula pelicula = gestor.agregar("La Odisea", 150, List.of(Genero.DRAMA),
                Clasificacion.ATP);
        programacionRepository.save(new Programacion(pelicula.getId(), 1,
                reloj.hoy().plusMonths(2), null, LocalTime.of(20, 30), Set.of(),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> gestor.eliminar(pelicula.getId()));

        assertTrue(e.getMessage().contains("La Odisea"), e.getMessage());
        assertTrue(funcionRepository.findByPelicula_Id(pelicula.getId()).isEmpty());
        assertEquals(1, gestor.listar().size());
    }
}
