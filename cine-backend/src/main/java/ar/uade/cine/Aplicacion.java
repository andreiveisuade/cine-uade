package ar.uade.cine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * El arranque del sistema. Es lo único que hace: el armado lo hace Spring.
 *
 * <p>Esta clase reemplaza a dos: el {@code Aplicacion} que construía a mano los dieciocho
 * gestores con sus DAO, y el {@code ServidorApi} que después levantaba Javalin encima. Las
 * dos existían por la misma razón —que el sistema se levanta desde más de un lado y todos
 * necesitan exactamente la misma aplicación abajo— y las dos resolvían a mano lo que un
 * contenedor de inversión de control resuelve solo.
 *
 * <p>La anotación hace tres cosas. {@code @SpringBootApplication} incluye un
 * {@code @ComponentScan} de {@code ar.uade.cine} hacia abajo, así que cada {@code @Service}
 * y cada {@code @RestController} se descubren solos y reciben sus colaboradores por
 * constructor; incluye la autoconfiguración, que es la que arma el DataSource y el Tomcat
 * embebido a partir de {@code application.yml}; y marca este paquete como raíz, que es lo
 * que hace que las capas de abajo se escaneen sin nombrarlas.
 *
 * <p>Lo que se ganó al sacar el armado a mano no es brevedad: es que un gestor nuevo ya no
 * se puede olvidar de conectar. Antes había un solo lugar donde se armaba todo justamente
 * para que no pasara —y aun así pasó una vez, el candy quedó afuera de la API cuando cada
 * arranque armaba su copia—. Ahora, si un gestor le pide al contenedor algo que nadie
 * declara, la aplicación <strong>no levanta</strong> y el error dice qué falta: el circuito
 * incompleto pasó de ser un bug de tiempo de uso a un error de arranque.
 *
 * <p>Qué implementación se usa para guardar cada cosa sigue decidiéndose en un solo lugar,
 * pero ahora es un perfil: ver {@link ar.uade.cine.repository.PersistenciaMySQL} y
 * {@link ar.uade.cine.repository.PersistenciaEnMemoria}.
 */
@SpringBootApplication
public class Aplicacion {

    public static void main(String[] args) {
        SpringApplication.run(Aplicacion.class, args);
    }
}
