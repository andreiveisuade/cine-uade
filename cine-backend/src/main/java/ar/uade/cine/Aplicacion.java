package ar.uade.cine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * El arranque del sistema; el armado lo hace Spring. Si un gestor pide algo que nadie
 * declara, la aplicación no levanta y el error dice qué falta.
 *
 * <p>Hereda de {@link SpringBootServletInitializer} para correr igual con el Tomcat embebido
 * ({@code main()}) o como WAR en un Tomcat externo ({@code configure()}).
 */
@SpringBootApplication
public class Aplicacion extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Aplicacion.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(Aplicacion.class, args);
    }
}
