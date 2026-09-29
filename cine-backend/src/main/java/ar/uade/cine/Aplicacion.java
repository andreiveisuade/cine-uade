package ar.uade.cine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

// Arranque del sistema del cine; el component scan de Spring arma y conecta todos los beans, nada a mano.
// Hereda de SpringBootServletInitializer para correr también como WAR en un Tomcat externo.
// El scan registra los records Propiedades*: cada uno vive al lado de la clase que lo usa.
@SpringBootApplication
@ConfigurationPropertiesScan
public class Aplicacion extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Aplicacion.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(Aplicacion.class, args);
    }
}
