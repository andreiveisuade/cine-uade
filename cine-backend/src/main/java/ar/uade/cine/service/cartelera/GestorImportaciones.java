package ar.uade.cine.service.cartelera;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import ar.uade.cine.model.cartelera.EstadoImportacion;
import ar.uade.cine.model.cartelera.Importacion;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.infrastructure.importador.CatalogoExterno;
import ar.uade.cine.infrastructure.importador.ImportadorError;
import ar.uade.cine.repository.ImportacionRepository;
import ar.uade.cine.infrastructure.reloj.Reloj;

// Sin @Transactional a propósito: la primera alta rechazada marcaría la transacción
// rollback-only y se perdería la corrida entera, incluso el registro.
@Service
public class GestorImportaciones {

    private static final int HISTORIAL = 20;

    private static final int PAGINAS_MAXIMAS = 3;

    private final ImportacionRepository importacionRepository;
    private final CatalogoExterno catalogo;
    private final GestorCartelera cartelera;
    private final GestorRevisionCartelera revision;
    private final Duration corridaMaxima;
    private final Duration esperaEntreCorridas;
    private final Reloj reloj;

    public GestorImportaciones(ImportacionRepository importacionRepository, CatalogoExterno catalogo,
                               GestorCartelera cartelera, GestorRevisionCartelera revision,
                               @Value("${cine.importador.corrida-maxima}") Duration corridaMaxima,
                               @Value("${cine.importador.espera-entre-corridas}") Duration esperaEntreCorridas,
                               Reloj reloj) {
        this.importacionRepository = importacionRepository;
        this.catalogo = catalogo;
        this.cartelera = cartelera;
        this.revision = revision;
        this.corridaMaxima = corridaMaxima;
        this.esperaEntreCorridas = esperaEntreCorridas;
        this.reloj = reloj;
    }

    public Importacion ejecutar(Integer paginas) {
        Importacion importacion = reservarTurno(validarPaginas(paginas));
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
        StringBuilder detalle = new StringBuilder();
        int nuevas = 0;
        int salteadas = 0;
        int fallidas = 0;

        for (DatosPelicula candidata : candidatas) {
            // También saltea lo que TMDB repite entre páginas; si no, R1 lo contaría como falla.
            String clave = clave(candidata.titulo());
            if (!clave.isEmpty() && !yaEstan.add(clave)) {
                salteadas++;
                continue;
            }
            try {
                Pelicula creada = revision.importar(candidata);
                detalle.append("+ [").append(creada.getId()).append("] ")
                        .append(creada.getTitulo()).append('\n');
                nuevas++;
            } catch (IllegalArgumentException e) {
                detalle.append("✗ ").append(nombreDe(candidata)).append(": ")
                        .append(e.getMessage()).append('\n');
                fallidas++;
            }
        }

        importacion.terminar(nuevas, salteadas, fallidas,
                detalle.isEmpty() ? null : detalle.toString().strip(), reloj.ahora());
    }

    // Incluye las descartadas: si no, cada corrida volvería a proponer lo ya rechazado.
    private Set<String> titulosCargados() {
        Set<String> titulos = new HashSet<>();
        for (Pelicula pelicula : cartelera.listar()) {
            titulos.add(clave(pelicula.getTitulo()));
        }
        return titulos;
    }

    private static String clave(String titulo) {
        return titulo == null ? "" : titulo.strip().toLowerCase();
    }

    private static String nombreDe(DatosPelicula candidata) {
        String titulo = candidata.titulo();
        return titulo == null || titulo.isBlank() ? "(sin título)" : titulo.strip();
    }

    // Sincronizado y aparte de la corrida para rechazar el segundo pedido sin hacerlo esperar.
    // Alcanza con un candado porque hay un solo backend.
    private synchronized Importacion reservarTurno(int paginas) {
        List<Importacion> ultimas = listar();
        if (!ultimas.isEmpty()) {
            exigirQueNoHayaOtraEnCurso(ultimas.get(0));
            exigirQueHayaPasadoUnRato(ultimas.get(0));
        }
        Importacion importacion = new Importacion(paginas, reloj.ahora());
        importacionRepository.save(importacion);
        return importacion;
    }

    private static void exigirQueNoHayaOtraEnCurso(Importacion ultima) {
        if (ultima.getEstado() == EstadoImportacion.EN_CURSO) {
            throw new IllegalArgumentException(
                    "Ya hay una importación en curso: esperá a que termine");
        }
    }

    private void exigirQueHayaPasadoUnRato(Importacion ultima) {
        LocalDateTime desde = ultima.getTerminoEn();
        if (desde != null && desde.plus(esperaEntreCorridas).isAfter(reloj.ahora())) {
            throw new IllegalArgumentException("El importador corrió recién: esperá "
                    + esperaEntreCorridas.toSeconds() + " segundos antes de volver a pedirlo");
        }
    }

    // De paso caduca las EN_CURSO vencidas: si no, un reinicio a mitad de corrida bloquearía el importador.
    public List<Importacion> listar() {
        List<Importacion> ultimas = importacionRepository.findAllByOrderByIdDesc(Limit.of(HISTORIAL));
        LocalDateTime ahora = reloj.ahora();
        for (Importacion importacion : ultimas) {
            if (quedoColgada(importacion, ahora)) {
                importacion.fallar("La corrida no terminó a tiempo. Puede haber cargado "
                        + "algunas películas igual: mirá el buzón.", ahora);
                importacionRepository.save(importacion);
            }
        }
        return ultimas;
    }

    private boolean quedoColgada(Importacion importacion, LocalDateTime ahora) {
        return importacion.getEstado() == EstadoImportacion.EN_CURSO
                && importacion.getPedidaEn().plus(corridaMaxima).isBefore(ahora);
    }

    public CatalogoExterno.Estado estadoDelImportador() {
        return catalogo.consultar();
    }

    private static int validarPaginas(Integer paginas) {
        if (paginas == null) {
            return 1;
        }
        if (paginas < 1 || paginas > PAGINAS_MAXIMAS) {
            throw new IllegalArgumentException(
                    "Las páginas a importar van de 1 a " + PAGINAS_MAXIMAS);
        }
        return paginas;
    }
}
