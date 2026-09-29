package ar.uade.cine.service.cartelera;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.programaciones.Programacion;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.repository.cartelera.PeliculaRepository;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.programaciones.ProgramacionRepository;
import ar.uade.cine.repository.salas.SalaRepository;
import ar.uade.cine.service.cartelera.GestorRevisionCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.programaciones.GestorProgramaciones;

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
    @Autowired
    private DataSource dataSource;

    private Sala sala;

    private void programarProxima(int peliculaId) {
        programar(peliculaId, reloj.ahora().plusDays(7));
    }

    private void programarPasada(int peliculaId) {
        programar(peliculaId, reloj.ahora().minusDays(1));
    }

    // Por el repositorio: el gestor no deja programar una película sin confirmar.
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
        assertThrows(Rechazo.class,
                () -> gestor.agregar("matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP));
    }

    // R1: con espacios en los bordes seguiría siendo la misma película.
    @Test
    void elTituloRepetidoNoSeBurlaConEspacios() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Pelicula dune = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        ConflictoDeNegocio alta = assertThrows(ConflictoDeNegocio.class,
                () -> gestor.agregar("  matrix ", 136, List.of(Genero.ACCION), Clasificacion.ATP));
        ConflictoDeNegocio edicion = assertThrows(ConflictoDeNegocio.class, () -> gestor.editar(dune.getId(),
                new DatosPelicula(" Matrix", null, null, null, null, null, null, null, null, null, null, null)));

        assertEquals("Ya existe una película con ese título", alta.getMessage());
        assertEquals("Ya existe una película con ese título", edicion.getMessage());
        assertEquals(2, gestor.listar().size());
    }

    @Test
    void rechazaDuracionInvalida() {
        assertThrows(Rechazo.class,
                () -> gestor.agregar("Sin duración", 0, List.of(Genero.DRAMA), Clasificacion.ATP));
    }

    // El mismo texto que el pedido HTTP: una duración que no vino falta, no es "cero".
    @Test
    void unaAltaSinDuracionDiceQueFalta() {
        Rechazo error = assertThrows(Rechazo.class, () -> gestor.agregar(
                new DatosPelicula("Dune", null, List.of(Genero.DRAMA), Clasificacion.ATP,
                        null, null, null, null, null, null, null, null)));

        assertEquals("Falta la duración", error.getMessage());
    }

    @Test
    void rechazaPeliculaSinGenero() {
        assertThrows(Rechazo.class,
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
    void elAnioVaDelCineAUnosAniosPorDelanteYCeroEsSinDato() {
        DatosPelicula base = DatosPelicula.deAlta("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        Pelicula dune = gestor.agregar(base);

        Rechazo negativo = assertThrows(Rechazo.class,
                () -> gestor.editar(dune.getId(), conAnio(-3)));
        assertEquals("El año tiene que estar entre 1895 y 2031", negativo.getMessage());
        assertThrows(Rechazo.class, () -> gestor.editar(dune.getId(), conAnio(1800)));
        assertThrows(Rechazo.class, () -> gestor.editar(dune.getId(), conAnio(2032)));
        assertDoesNotThrow(() -> gestor.editar(dune.getId(), conAnio(0)));
        assertEquals(2021, gestor.editar(dune.getId(), conAnio(2021)).getAnio());
    }

    private static DatosPelicula conAnio(int anio) {
        return new DatosPelicula(null, null, null, null, null, null, anio, null, null, null, null, null);
    }

    @Test
    void rechazaPeliculaSinClasificacion() {
        assertThrows(Rechazo.class,
                () -> gestor.agregar("Sin clasificar", 100, List.of(Genero.DRAMA), null));
    }

    @Test
    void losDatosDeCatalogoSeCarganDespuesDelAlta() {
        Pelicula pelicula = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        gestor.editar(pelicula.getId(), new DatosPelicula(null, null, null, null, "Denis Villeneuve",
                null, 2021, "Inglés", null, null, null, null));

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

        sacarDeCartelera(vieja);

        assertEquals(2, gestor.listar().size());
        assertEquals(1, gestor.listarEnCartelera(null).size());
        assertEquals("Dune", gestor.listarEnCartelera(null).get(0).getTitulo());
    }

    @Test
    void unaPeliculaSinFuncionesNoEstaEnCartelera() {
        Pelicula pelicula = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        assertTrue(pelicula.estaEnCartelera(), "el flag arranca en true");
        assertEquals(0, gestor.listarEnCartelera(null).size(), "pero sin funciones no está en cartelera");
    }

    @Test
    void programarUnaFuncionLaPoneEnCartelera() {
        Pelicula pelicula = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        programarProxima(pelicula.getId());

        assertEquals(1, gestor.listarEnCartelera(null).size());
    }

    @Test
    void saleDeCarteleraSolaCuandoSusFuncionesQuedaronAtras() {
        Pelicula pelicula = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        programarPasada(pelicula.getId());

        assertTrue(pelicula.estaEnCartelera(), "nadie tocó el flag");
        assertEquals(0, gestor.listarEnCartelera(null).size(), "y aun así ya no está en cartelera");
    }

    @Test
    void conUnaFuncionPasadaYOtraFuturaSigueEnCartelera() {
        Pelicula pelicula = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        programarPasada(pelicula.getId());
        programarProxima(pelicula.getId());

        assertEquals(1, gestor.listarEnCartelera(null).size());
    }

    // Si retuviera su conexión mientras extiende las grillas, pediría una segunda: con el pool
    // entero pidiendo la cartelera a la vez, todos esperarían una que nadie suelta.
    @Test
    void laCarteleraPublicaSeArreglaConUnaSolaConexionLibre() throws Exception {
        HikariDataSource pool = dataSource.unwrap(HikariDataSource.class);
        List<Connection> tomadas = new ArrayList<>();
        try (ExecutorService hilo = Executors.newSingleThreadExecutor()) {
            try {
                while (tomadas.size() < pool.getMaximumPoolSize() - 1) {
                    tomadas.add(pool.getConnection());
                }
                Future<List<Pelicula>> cartelera = hilo.submit(() -> gestor.listarEnCartelera(null));

                assertDoesNotThrow(() -> cartelera.get(5, TimeUnit.SECONDS));
            } finally {
                for (Connection conexion : tomadas) {
                    conexion.close();
                }
            }
        }
    }

    @Test
    void noSePuedeEditarUnaPeliculaParaQueQuedeConElTituloDeOtra() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Pelicula dune = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        assertThrows(Rechazo.class, () -> gestor.editar(dune.getId(),
                new DatosPelicula("Matrix", null, null, null, null, null, null, null, null, null, null, null)));
    }

    @Test
    void editarSinCambiarElTituloNoChocaConsigoMisma() {
        Pelicula dune = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        assertDoesNotThrow(() -> gestor.editar(dune.getId(),
                new DatosPelicula("Dune", null, null, null, "Denis Villeneuve", null, null, null, null, null, null, null)));
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
        assertTrue(gestor.listarEnCartelera(null).isEmpty());
    }

    @Test
    void editarNoDejaRenombrarConElTituloDeOtra() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Pelicula dune = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        assertThrows(Rechazo.class, () -> gestor.editar(dune.getId(),
                new DatosPelicula("Matrix", null, null, null, null, null, null, null, null, null, null, null)));
    }

    @Test
    void editarRechazaPrimeroLosDatosDespuesElTituloRepetidoYAlFinalElCatalogoSinTocarNada() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Pelicula dune = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        Rechazo datos = assertThrows(Rechazo.class, () -> gestor.editar(
                dune.getId(), new DatosPelicula("Matrix", 0, null, null, null, null, null, null, null, null, 15.0, null)));
        ConflictoDeNegocio titulo = assertThrows(ConflictoDeNegocio.class, () -> gestor.editar(
                dune.getId(), new DatosPelicula("Matrix", 150, null, null, null, null, null, null, null, null, 15.0, null)));

        assertEquals("La duración tiene que ser mayor a cero", datos.getMessage());
        assertEquals("Ya existe una película con ese título", titulo.getMessage());
        Pelicula leida = gestor.buscar(dune.getId()).orElseThrow();
        assertEquals("Dune", leida.getTitulo());
        assertEquals(155, leida.getDuracionMinutos());
    }

    @Test
    void editarNoDejaUnaPeliculaSinTituloNiConDuracionCero() {
        Pelicula dune = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        assertThrows(Rechazo.class, () -> gestor.editar(dune.getId(),
                new DatosPelicula("  ", null, null, null, null, null, null, null, null, null, null, null)));
        assertThrows(Rechazo.class, () -> gestor.editar(dune.getId(),
                new DatosPelicula(null, 0, null, null, null, null, null, null, null, null, null, null)));
    }

    @Test
    void editarUnaPeliculaInexistenteFalla() {
        assertThrows(Rechazo.class,
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

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin criterios devuelve todo,                                     ,          ,                ,     3
            un título vacío no filtra,                                       '',        ,                ,     3
            el título es parcial,                                            matrix,    ,                ,     2
            el título no distingue mayúsculas,                               MATRIX,    ,                ,     2
            por una palabra del título,                                      reloaded,  ,                ,     1
            por un pedazo del medio del título,                              esplandor, ,                ,     1
            los criterios se combinan,                                       matrix,    CIENCIA_FICCION, true, 1
            ninguna Matrix es de terror: combinar tiene que poder dar vacío, matrix,    TERROR,          ,     0
            sin coincidencias devuelve vacío y no falla,                     titanic,   ,                ,     0
            """)
    void buscarFiltraPorTituloGeneroYEstado(String caso, String titulo, Genero genero, Boolean publicada,
            int esperadas) {
        cargarCatalogo();

        assertEquals(esperadas, gestor.buscar(titulo, genero, publicada).size(), caso);
    }

    @Test
    void buscarPorGeneroYPorEstado() {
        cargarCatalogo();

        assertEquals(2, gestor.buscar(null, Genero.ACCION, null).size());
        assertEquals(3, gestor.buscar(null, null, true).size(), "el alta las publica");

        Pelicula resplandor = gestor.buscar(3).orElseThrow();
        sacarDeCartelera(resplandor);

        assertEquals(2, gestor.buscar(null, null, true).size());
        assertEquals(1, gestor.buscar(null, null, false).size());
        assertEquals(3, gestor.buscar(null, null, null).size(), "null es todas, no ninguna");
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

    // El alta la construye publicada y confirmada; recién después el importador la deja pendiente.
    @Test
    void unaImportadaQueVieneMarcadaParaPublicarIgualQuedaPendiente() {
        Pelicula pelicula = revision.importar(new DatosPelicula("Dune", 155, List.of(Genero.ACCION),
                Clasificacion.ATP, null, null, null, null, null, true, null, null));

        assertEquals(EstadoRevision.PENDIENTE, pelicula.getEstadoRevision());
        assertFalse(pelicula.estaEnCartelera());
    }

    @Test
    void publicarUnaPendienteSeRechazaYNoGuardaNada() {
        Pelicula importada = revision.importar(deTmdb("Dune"));

        Rechazo error = assertThrows(Rechazo.class,
                () -> gestor.editar(importada.getId(), new DatosPelicula(null, null, null, null,
                        "Denis Villeneuve", null, null, null, null, true, null, null)));

        assertEquals("La película Dune no está confirmada: revisala antes de publicarla", error.getMessage());
        Pelicula leida = gestor.buscar(importada.getId()).orElseThrow();
        assertFalse(leida.estaEnCartelera());
        assertEquals("", leida.getDirector(), "el rechazo deshace también el resto del pedido");
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

        assertThrows(Rechazo.class, () -> revision.descartar(importada.getId()));
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

        Rechazo e = assertThrows(Rechazo.class,
                () -> gestor.eliminar(pelicula.getId()));

        assertTrue(e.getMessage().contains("Matrix"), e.getMessage());
        assertEquals(1, gestor.listar().size());
    }

    @Test
    void noBorraLaProgramadaAunqueNoTengaFunciones() {
        Pelicula pelicula = gestor.agregar("La Odisea", 150, List.of(Genero.DRAMA),
                Clasificacion.ATP);
        sala = salaRepository.save(new Sala("Sala 1", TipoSala.DOS_D, 15));
        programacionRepository.save(new Programacion(pelicula, sala,
                reloj.hoy().plusMonths(2), null, LocalTime.of(20, 30), Set.of(),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));

        Rechazo e = assertThrows(Rechazo.class,
                () -> gestor.eliminar(pelicula.getId()));

        assertTrue(e.getMessage().contains("La Odisea"), e.getMessage());
        assertTrue(funcionRepository.findByPelicula_IdOrderByInicioAsc(pelicula.getId()).isEmpty());
        assertEquals(1, gestor.listar().size());
    }

    private void sacarDeCartelera(Pelicula pelicula) {
        gestor.editar(pelicula.getId(), new DatosPelicula(null, null, null, null, null, null, null, null,
                null, false, null, null));
    }
}
