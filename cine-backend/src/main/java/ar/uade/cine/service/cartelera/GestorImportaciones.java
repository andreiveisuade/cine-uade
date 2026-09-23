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

/**
 * Pedir cartelera nueva, ahora, desde el panel del encargado.
 *
 * <p><strong>No sabe qué es TMDB.</strong> Le pide candidatas a un {@link CatalogoExterno}
 * y las da de alta por {@link GestorRevisionCartelera}, así entran al buzón con las mismas
 * reglas que el alta a mano. Saltear lo que ya está —por título, que es lo que R1 hace
 * único— es regla del cine y por eso vive acá y no en el adaptador.
 *
 * <p>Sin {@code @Transactional}, y no es un olvido: una corrida no es atómica. Su razón
 * de ser es que unas películas entren, otras se salteen y otras fallen, y que eso quede
 * contado. Con una transacción envolviendo todo, el primer alta rechazada marcaría
 * rollback-only y el commit final tiraría todo, incluso el registro de la corrida.
 */
@Service
public class GestorImportaciones {

    /** Cuántas corridas muestra la pantalla. El historial crece para siempre; la tabla no. */
    private static final int HISTORIAL = 20;

    /** Una página de TMDB son veinte títulos; más de tres tarda demasiado para un botón. */
    private static final int PAGINAS_MAXIMAS = 3;

    private final ImportacionRepository importacionRepository;
    private final CatalogoExterno catalogo;
    private final GestorCartelera cartelera;
    private final GestorRevisionCartelera revision;
    private final Duration corridaMaxima;
    private final Duration esperaEntreCorridas;
    private final Reloj reloj;

    /**
     * Las dos duraciones salen de la configuración para poder probarlas: el perfil de test
     * las baja a cero en vez de esperar cinco minutos de reloj.
     *
     * @param corridaMaxima cuánto puede estar EN_CURSO antes de darla por perdida
     * @param esperaEntreCorridas el mínimo entre dos corridas seguidas
     */
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

    /**
     * Corre una importación y vuelve cuando terminó. Bloquea el pedido los diez o quince
     * segundos que tarda TMDB: el encargado está esperando, y un «después te aviso»
     * obligaría al navegador a preguntar cada dos segundos.
     *
     * <p>Que el catálogo externo falle no hace fallar esto: la corrida queda FALLIDA con el
     * motivo, que es un resultado. Solo tira lo que el encargado puede corregir.
     *
     * @param paginas cuántas páginas de TMDB traer, o {@code null} para una
     */
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

    /**
     * La corrida: traer, saltear lo que ya está y mandar el resto al buzón. Una película
     * que el alta rechaza no corta la corrida: queda anotada como fallida y se sigue.
     */
    private void correr(Importacion importacion) {
        List<DatosPelicula> candidatas = catalogo.enCartelera(importacion.getPaginas());
        Set<String> yaEstan = titulosCargados();
        StringBuilder detalle = new StringBuilder();
        int nuevas = 0;
        int salteadas = 0;
        int fallidas = 0;

        for (DatosPelicula candidata : candidatas) {
            // El mismo Set saltea lo que ya está en el catálogo y lo que TMDB trajo dos
            // veces entre páginas; si no, el duplicado volvería rechazado por R1 como falla.
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
                // Rechazada por una regla de negocio: es el sistema haciendo su trabajo.
                detalle.append("✗ ").append(nombreDe(candidata)).append(": ")
                        .append(e.getMessage()).append('\n');
                fallidas++;
            }
        }

        importacion.terminar(nuevas, salteadas, fallidas,
                detalle.isEmpty() ? null : detalle.toString().strip(), reloj.ahora());
    }

    /**
     * Los títulos que ya están, normalizados. Incluye las descartadas a propósito: si no,
     * cada corrida volvería a proponer lo que el encargado ya rechazó.
     */
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

    /** Para el renglón del log: sin título, el alta la va a rechazar y hay que nombrarla igual. */
    private static String nombreDe(DatosPelicula candidata) {
        String titulo = candidata.titulo();
        return titulo == null || titulo.isBlank() ? "(sin título)" : titulo.strip();
    }

    /**
     * Deja anotado que esta corrida arrancó, si puede arrancar. Sincronizado y aparte de
     * la corrida: es lo único que dos pedidos simultáneos no pueden hacer a la vez, y
     * encerrar los quince segundos de la corrida haría esperar al segundo para rechazarlo.
     * Alcanza con un candado porque hay un solo backend.
     */
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

    /** Cada corrida son sesenta llamadas a TMDB, que tiene cuota; dos seguidas no traen nada nuevo. */
    private void exigirQueHayaPasadoUnRato(Importacion ultima) {
        LocalDateTime desde = ultima.getTerminoEn();
        if (desde != null && desde.plus(esperaEntreCorridas).isAfter(reloj.ahora())) {
            throw new IllegalArgumentException("El importador corrió recién: esperá "
                    + esperaEntreCorridas.toSeconds() + " segundos antes de volver a pedirlo");
        }
    }

    /**
     * Las últimas corridas, de la más nueva a la más vieja. De paso da por perdidas las que
     * quedaron EN_CURSO de más: sin esto, un backend reiniciado a mitad de corrida dejaría
     * el importador bloqueado para siempre. Lo hace quien consulta, no un proceso de fondo.
     */
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

    /** Si el catálogo externo puede contestar: la pantalla avisa antes de que alguien espere en vano. */
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
