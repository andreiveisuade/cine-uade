package ar.uade.cine.persistencia;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ar.uade.cine.persistencia.memoria.AsientoDAOMemoria;
import ar.uade.cine.persistencia.memoria.BloqueoButacasMemoria;
import ar.uade.cine.persistencia.memoria.ClienteDAOMemoria;
import ar.uade.cine.persistencia.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.persistencia.memoria.EmpleadoDAOMemoria;
import ar.uade.cine.persistencia.memoria.FuncionDAOMemoria;
import ar.uade.cine.persistencia.memoria.ImportacionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PagoDAOMemoria;
import ar.uade.cine.persistencia.memoria.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProductoDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProgramacionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PromocionDAOMemoria;
import ar.uade.cine.persistencia.memoria.ReservaDAOMemoria;
import ar.uade.cine.persistencia.memoria.SalaDAOMemoria;

/**
 * El mismo sistema, guardando todo en memoria. Es lo que activa el perfil {@code memoria}.
 *
 * <p>Es la contracara de {@link PersistenciaMySQL} y la prueba de que la inversión de
 * dependencias es real y no una promesa del diagrama: los gestores no cambian una línea,
 * y ninguno de ellos se entera de que abajo no hay una base de datos. Lo usan los tests
 * que levantan la aplicación entera —incluida la API por HTTP— sin necesitar MySQL ni
 * Redis corriendo.
 */
@Configuration
@Profile("memoria")
public class PersistenciaEnMemoria {

    @Bean
    public PeliculaDAO peliculaDAO() {
        return new PeliculaDAOMemoria();
    }

    @Bean
    public SalaDAO salaDAO() {
        return new SalaDAOMemoria();
    }

    @Bean
    public AsientoDAO asientoDAO() {
        return new AsientoDAOMemoria();
    }

    @Bean
    public FuncionDAO funcionDAO() {
        return new FuncionDAOMemoria();
    }

    @Bean
    public ClienteDAO clienteDAO() {
        return new ClienteDAOMemoria();
    }

    @Bean
    public EmpleadoDAO empleadoDAO() {
        return new EmpleadoDAOMemoria();
    }

    @Bean
    public ReservaDAO reservaDAO() {
        return new ReservaDAOMemoria();
    }

    @Bean
    public PagoDAO pagoDAO() {
        return new PagoDAOMemoria();
    }

    @Bean
    public PromocionDAO promocionDAO() {
        return new PromocionDAOMemoria();
    }

    @Bean
    public ProgramacionDAO programacionDAO() {
        return new ProgramacionDAOMemoria();
    }

    @Bean
    public ProductoDAO productoDAO() {
        return new ProductoDAOMemoria();
    }

    @Bean
    public CompraCandyDAO compraCandyDAO() {
        return new CompraCandyDAOMemoria();
    }

    @Bean
    public ImportacionDAO importacionDAO() {
        return new ImportacionDAOMemoria();
    }

    @Bean
    public BloqueoButacas bloqueoButacas() {
        return new BloqueoButacasMemoria();
    }
}
