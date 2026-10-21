package ar.uade.cine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

// Arranque del sistema; el component scan arma los beans, y SpringBootServletInitializer permite correr como WAR.
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
