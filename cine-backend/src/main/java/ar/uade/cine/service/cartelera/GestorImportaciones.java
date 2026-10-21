package ar.uade.cine.service.cartelera;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import ar.uade.cine.model.cartelera.Importacion;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.infrastructure.importador.CatalogoExterno;
import ar.uade.cine.infrastructure.importador.ImportadorError;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.repository.cartelera.ImportacionRepository;
import ar.uade.cine.infrastructure.reloj.Reloj;

// Corre el importador de TMDB y registra cada corrida; de a una por vez y con espera entre corridas.
// Sin @Transactional: la primera alta rechazada marcaría rollback-only y perdería la corrida entera.
@Service
@RequiredArgsConstructor
@Slf4j
public class GestorImportaciones {

    private static final int HISTORIAL = 20;

    private final ImportacionRepository importacionRepository;
    private final CatalogoExterno catalogo;
    private final GestorCartelera cartelera;
    private final GestorRevisionCartelera revision;
    private final PropiedadesImportador propiedades;
    private final Reloj reloj;

    public Importacion ejecutar(Integer paginas) {
        Importacion importacion = reservarTurno(paginas);
        try {
            correr(importacion);
        } catch (ImportadorError e) {
            importacion.fallar(e.getMessage(), reloj.ahora());
        }
        importacionRepository.save(importacion);
        return importacion;
    }

    private void correr(Importacion importacion) {
        List<DatosPelicula> candidatas = catalogo.enCartelera(importacion.getPaginas());
        Set<String> yaEstan = titulosCargados();
        Corrida corrida = new Corrida();

        for (DatosPelicula candidata : candidatas) {
            // También saltea lo que TMDB repite entre páginas; si no, R1 lo contaría como falla.
            String clave = clave(candidata.titulo());
            if (!clave.isEmpty() && !yaEstan.add(clave)) {
                corrida.salteada();
                continue;
            }
            try {
                Pelicula creada = revision.importar(candidata);
                corrida.nueva(creada.getId(), creada.getTitulo());
            } catch (RuntimeException e) {
                // Cualquier falla y no solo un Rechazo: si no, la corrida quedaba EN_CURSO y bloqueaba el importador.
                // Una candidata no frena a las demás.
                corrida.fallida(nombreDe(candidata), motivoDe(candidata, e));
            }
        }

        importacion.terminar(corrida.resumen(), reloj.ahora());
    }

    // Incluye las descartadas: si no, cada corrida volvería a proponer lo ya rechazado.
    private Set<String> titulosCargados() {
        Set<String> titulos = new HashSet<>();
        for (String titulo : cartelera.titulos()) {
            titulos.add(clave(titulo));
        }
        return titulos;
    }

    private static String clave(String titulo) {
        return titulo == null ? "" : titulo.strip().toLowerCase(Locale.ROOT);
    }

    // El texto de un Rechazo es para el encargado; el de cualquier otra falla es técnico y va al log.
    private static String motivoDe(DatosPelicula candidata, RuntimeException e) {
        if (e instanceof Rechazo) {
            return e.getMessage();
        }
        log.warn("El importador no pudo guardar «{}»", nombreDe(candidata), e);
        return "No se pudo guardar: el motivo quedó en el log del servidor";
    }

    private static String nombreDe(DatosPelicula candidata) {
        String titulo = candidata.titulo();
        return titulo == null || titulo.isBlank() ? "(sin título)" : titulo.strip();
    }

    // Sincronizado y aparte de la corrida para rechazar el segundo pedido sin esperar; un solo backend, un candado.
    // La corrida se arma primero: páginas fuera de rango se rechazan sin dejar registro.
    private synchronized Importacion reservarTurno(Integer paginas) {
        Importacion importacion = new Importacion(paginas, reloj.ahora());
        List<Importacion> ultimas = listar();
        if (!ultimas.isEmpty()) {
            exigirQueNoHayaOtraEnCurso(ultimas.get(0));
            exigirQueHayaPasadoUnRato(ultimas.get(0));
        }
        importacionRepository.save(importacion);
        return importacion;
    }

    private static void exigirQueNoHayaOtraEnCurso(Importacion ultima) {
        if (ultima.estaEnCurso()) {
            throw new DatoInvalido(
                    "Ya hay una importación en curso: esperá a que termine");
        }
    }

    private void exigirQueHayaPasadoUnRato(Importacion ultima) {
        if (ultima.terminoHaceMenosDe(propiedades.esperaEntreCorridas(), reloj.ahora())) {
            throw new DatoInvalido("El importador corrió recién: esperá "
                    + propiedades.esperaEntreCorridas().toSeconds() + " segundos antes de volver a pedirlo");
        }
    }

    // De paso caduca las EN_CURSO vencidas: si no, un reinicio a mitad de corrida bloquearía el importador.
    public List<Importacion> listar() {
        List<Importacion> ultimas = importacionRepository.findAllByOrderByIdDesc(Limit.of(HISTORIAL));
        LocalDateTime ahora = reloj.ahora();
        for (Importacion importacion : ultimas) {
            if (importacion.quedoColgada(propiedades.corridaMaxima(), ahora)) {
                importacion.fallar("La corrida no terminó a tiempo: puede haber cargado "
                        + "algunas películas igual, mirá el buzón", ahora);
                importacionRepository.save(importacion);
            }
        }
        return ultimas;
    }

    public CatalogoExterno.Estado estadoDelImportador() {
        return catalogo.consultar();
    }
}
