package ar.uade.cine.persistencia;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ar.uade.cine.persistencia.mysql.AsientoDAOMySQL;
import ar.uade.cine.persistencia.mysql.ClienteDAOMySQL;
import ar.uade.cine.persistencia.mysql.CompraCandyDAOMySQL;
import ar.uade.cine.persistencia.mysql.EmpleadoDAOMySQL;
import ar.uade.cine.persistencia.mysql.FuncionDAOMySQL;
import ar.uade.cine.persistencia.mysql.ImportacionDAOMySQL;
import ar.uade.cine.persistencia.mysql.PagoDAOMySQL;
import ar.uade.cine.persistencia.mysql.PeliculaDAOMySQL;
import ar.uade.cine.persistencia.mysql.Plantilla;
import ar.uade.cine.persistencia.mysql.ProductoDAOMySQL;
import ar.uade.cine.persistencia.mysql.ProgramacionDAOMySQL;
import ar.uade.cine.persistencia.mysql.PromocionDAOMySQL;
import ar.uade.cine.persistencia.mysql.ReservaDAOMySQL;
import ar.uade.cine.persistencia.mysql.SalaDAOMySQL;
import ar.uade.cine.persistencia.redis.BloqueoButacasRedis;

/**
 * Dónde se guarda cada cosa cuando el sistema corre de verdad: MySQL, y los bloqueos de
 * butaca en Redis.
 *
 * <p>Es el <strong>único</strong> lugar donde se nombra una implementación concreta de
 * persistencia, igual que antes lo era el constructor de {@code Aplicacion}. Lo que cambió
 * es que ahora la elección es declarativa: la clase entera cuelga del perfil
 * {@code mysql}, y {@link PersistenciaEnMemoria} del perfil {@code memoria}. Levantar el
 * sistema contra otro medio es activar otro perfil, no editar código.
 *
 * <p>Los DAO no llevan anotaciones de Spring y siguen siendo objetos comunes: se los
 * declara acá con {@code @Bean} en vez de marcarlos con {@code @Repository} justamente
 * para que la decisión quede escrita en un archivo que se lee de arriba abajo, y no
 * repartida en veintisiete anotaciones que hay que ir a buscar de a una. Los gestores, que
 * sí son componentes propios sin configuración, se descubren solos con {@code @Service}.
 *
 * <p>El {@link DataSource} lo arma Spring Boot con lo que dice {@code application.yml}, y
 * el pool sigue siendo HikariCP: eso es lo que reemplazó a {@code OrigenMySQL}. Sigue
 * siendo uno solo para los trece DAO, por el mismo motivo de siempre —trece pools de diez
 * conexiones serían ciento treinta conexiones contra la misma base— pero ahora eso no hay
 * que cuidarlo: el contenedor entrega la misma instancia a todos.
 */
@Configuration
@Profile("mysql")
public class PersistenciaMySQL {

    /** El andamiaje de JDBC que comparten los trece DAO, sobre el pool de Spring Boot. */
    @Bean
    public Plantilla plantilla(DataSource origen) {
        return new Plantilla(origen);
    }

    @Bean
    public PeliculaDAO peliculaDAO(Plantilla plantilla) {
        return new PeliculaDAOMySQL(plantilla);
    }

    @Bean
    public SalaDAO salaDAO(Plantilla plantilla) {
        return new SalaDAOMySQL(plantilla);
    }

    @Bean
    public AsientoDAO asientoDAO(Plantilla plantilla) {
        return new AsientoDAOMySQL(plantilla);
    }

    @Bean
    public FuncionDAO funcionDAO(Plantilla plantilla) {
        return new FuncionDAOMySQL(plantilla);
    }

    @Bean
    public ClienteDAO clienteDAO(Plantilla plantilla) {
        return new ClienteDAOMySQL(plantilla);
    }

    @Bean
    public EmpleadoDAO empleadoDAO(Plantilla plantilla) {
        return new EmpleadoDAOMySQL(plantilla);
    }

    @Bean
    public ReservaDAO reservaDAO(Plantilla plantilla) {
        return new ReservaDAOMySQL(plantilla);
    }

    @Bean
    public PagoDAO pagoDAO(Plantilla plantilla) {
        return new PagoDAOMySQL(plantilla);
    }

    @Bean
    public PromocionDAO promocionDAO(Plantilla plantilla) {
        return new PromocionDAOMySQL(plantilla);
    }

    @Bean
    public ProgramacionDAO programacionDAO(Plantilla plantilla) {
        return new ProgramacionDAOMySQL(plantilla);
    }

    @Bean
    public ProductoDAO productoDAO(Plantilla plantilla) {
        return new ProductoDAOMySQL(plantilla);
    }

    @Bean
    public CompraCandyDAO compraCandyDAO(Plantilla plantilla) {
        return new CompraCandyDAOMySQL(plantilla);
    }

    @Bean
    public ImportacionDAO importacionDAO(Plantilla plantilla) {
        return new ImportacionDAOMySQL(plantilla);
    }

    /**
     * Los bloqueos van a Redis y no a MySQL porque son lo contrario de todo lo demás que
     * hay acá: duran tres minutos y después no le importan a nadie. Si Redis no está,
     * {@link BloqueoButacasRedis} degrada a "ningún bloqueo" y el sistema sigue vendiendo
     * como antes de que existiera.
     */
    @Bean
    public BloqueoButacas bloqueoButacas(@Value("${cine.redis.host}") String host,
                                         @Value("${cine.redis.puerto}") int puerto) {
        return new BloqueoButacasRedis(host, puerto);
    }
}
