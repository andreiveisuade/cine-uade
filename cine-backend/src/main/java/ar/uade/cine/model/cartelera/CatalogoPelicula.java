package ar.uade.cine.model.cartelera;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.validacion.Regla;

// Lo que se muestra de una película (director, sinopsis, año, póster, puntaje); Value Object inmutable.
// @Embeddable sobre las columnas de pelicula; cambiar un dato es armar otro con conCambios, ya validado.
// Clase y no record: el tope del año depende de hoy y una fila vieja no debe impedir leer la cartelera.
@Embeddable
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
public class CatalogoPelicula {

    // La primera proyección pública, y un margen para las que se anuncian con años de anticipación.
    private static final int PRIMER_ANIO = 1895;
    private static final int ANIOS_POR_DELANTE = 5;
    // Como lo deja el importador cuando TMDB no trae fecha de estreno.
    private static final int SIN_ANIO = 0;
    // Muy por encima de cualquier película de TMDB, y lejos del int: PuntajeConfiable le suma votos.
    private static final int VOTOS_MAXIMOS = 100_000_000;

    private String director = "";

    private String sinopsis = "";

    private int anio;

    private String idiomaOriginal = "";

    private String posterUrl = "";

    // Sin decirle DECIMAL(3,1), Hibernate espera FLOAT y `validate` corta el arranque.
    @Column(columnDefinition = "DECIMAL(3,1)")
    private double puntaje;

    private int votos;

    // Vacío: el de JPA y el de una película recién dada de alta, que todavía no tiene datos de catálogo.
    protected CatalogoPelicula() {
    }

    // Guardas en el orden de siempre (puntaje antes que año) para no cambiar el primer error. El día lo pasa quien tiene el reloj.
    private CatalogoPelicula(String director, String sinopsis, int anio, String idiomaOriginal,
                             String posterUrl, double puntaje, int votos, LocalDate hoy) {
        this.puntaje = Regla.numero(puntaje).entre(0.0, 10.0, "El puntaje tiene que estar entre 0 y 10")
                .conDecimales(1, "El puntaje tiene que tener como máximo un decimal").valor();
        this.votos = Regla.numero(votos)
                .entre(0, VOTOS_MAXIMOS, "Los votos tienen que estar entre 0 y 100.000.000").valor();
        this.director = Regla.texto(director).recortado().hasta(100, "El director").valor();
        this.sinopsis = Regla.texto(sinopsis).hasta(5000, "La sinopsis").valor();
        this.anio = anioValido(anio, hoy);
        this.idiomaOriginal = Regla.texto(idiomaOriginal).recortado().hasta(40, "El idioma original").valor();
        this.posterUrl = posterValido(posterUrl);
    }

    // Otro catálogo con lo que vino; null es "no lo mandé" y deja lo que había. Este no cambia.
    public CatalogoPelicula conCambios(String director, String sinopsis, Integer anio, String idiomaOriginal,
                                       String posterUrl, Double puntaje, Integer votos, LocalDate hoy) {
        return new CatalogoPelicula(nuevoOActual(director, this.director),
                nuevoOActual(sinopsis, this.sinopsis), nuevoOActual(anio, this.anio),
                nuevoOActual(idiomaOriginal, this.idiomaOriginal), nuevoOActual(posterUrl, this.posterUrl),
                nuevoOActual(puntaje, this.puntaje), nuevoOActual(votos, this.votos), hoy);
    }

    // No Objects.requireNonNullElse, que tira si los dos son null: la sinopsis de una fila vieja puede serlo.
    private static <T> T nuevoOActual(T nuevo, T actual) {
        return nuevo == null ? actual : nuevo;
    }

    private static int anioValido(int anio, LocalDate hoy) {
        int maximo = hoy.getYear() + ANIOS_POR_DELANTE;
        if (anio != SIN_ANIO) {
            Regla.numero(anio).entre(PRIMER_ANIO, maximo,
                    "El año tiene que estar entre " + PRIMER_ANIO + " y " + maximo);
        }
        return anio;
    }

    // Vacío es "sin póster". Lo demás termina en un <img src>: una ruta suelta no carga y javascript: no va.
    private static String posterValido(String url) {
        Regla.texto(url).hasta(255, "La URL del póster");
        if (!url.isEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
            throw new DatoInvalido("La URL del póster tiene que empezar con http:// o https://");
        }
        return url;
    }
}
