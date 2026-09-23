package ar.uade.cine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

// Hereda de SpringBootServletInitializer para correr también como WAR en un Tomcat externo.
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
