package ar.uade.cine.model.cartelera;

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

import ar.uade.cine.model.cartelera.validacion.ValidadorImportacion;

// Registro de una corrida del importador de TMDB; Experto: sabe si terminó, falló o quedó colgada.
@Entity
@Getter
public class Importacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int paginas;

    private LocalDateTime pedidaEn;

    @Enumerated(EnumType.STRING)
    private EstadoImportacion estado = EstadoImportacion.EN_CURSO;

    private LocalDateTime terminoEn;

    // Arranca en cero: las columnas de los contadores son NOT NULL y una corrida en curso todavía no contó nada.
    @Embedded
    private ResumenDeCorrida resumen = ResumenDeCorrida.deFalla(null);

    protected Importacion() {
    }

    // Sin páginas se trae una: «traeme cartelera» ya es un pedido completo.
    public Importacion(Integer paginas, LocalDateTime pedidaEn) {
        this.paginas = ValidadorImportacion.paginas(paginas == null ? 1 : paginas);
        this.pedidaEn = pedidaEn;
    }

    // State: el estado decide si la transición vale.
    public void terminar(ResumenDeCorrida resumen, LocalDateTime cuando) {
        this.estado = estado.terminar();
        this.resumen = resumen;
        this.terminoEn = cuando;
    }

    public void fallar(String motivo, LocalDateTime cuando) {
        this.estado = estado.fallar();
        this.resumen = ResumenDeCorrida.deFalla(motivo);
        this.terminoEn = cuando;
    }

    public boolean estaEnCurso() {
        return estado.estaEnCurso();
    }

    // La vista y el historial leen los datos sueltos
    public int getNuevas() {
        return resumen.nuevas();
    }

    public int getSalteadas() {
        return resumen.salteadas();
    }

    public int getFallidas() {
        return resumen.fallidas();
    }

    public String getDetalle() {
        return resumen.detalle();
    }

    public boolean quedoColgada(Duration plazo, LocalDateTime ahora) {
        return estaEnCurso() && pedidaEn.plus(plazo).isBefore(ahora);
    }

    public boolean terminoHaceMenosDe(Duration espera, LocalDateTime ahora) {
        return terminoEn != null && terminoEn.plus(espera).isAfter(ahora);
    }
}
