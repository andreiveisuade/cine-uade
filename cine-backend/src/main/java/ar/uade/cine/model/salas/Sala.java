package ar.uade.cine.model.salas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

// Sala de proyección (R2); Experto en sus datos y Creador de sus butacas, que genera solo en el alta.
@Entity
@Getter
public class Sala {

    public static final int LIMPIEZA_POR_DEFECTO = 15;

    private static final int LARGO_MAXIMO_DEL_NOMBRE = 50;

    // Cada fila se nombra con una letra: ver Asiento.codigoDe.
    private static final int MAX_FILAS = 26;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nombre;

    @Enumerated(EnumType.STRING)
    private TipoSala tipo;

    private int minutosLimpieza;

    protected Sala() {
    }

    public Sala(String nombre, TipoSala tipo, int minutosLimpieza) {
        editar(nombre, tipo, minutosLimpieza);
    }

    // No toca las butacas: rehacerlas dejaría entradas vendidas apuntando a asientos inexistentes.
    // Valida lo mismo que el alta: el gestor solo chequea lo que necesita la base (nombre
    // repetido, funciones programadas).
    public void editar(String nombre, TipoSala tipo, int minutosLimpieza) {
        String nuevoNombre = nombreValido(nombre);
        if (tipo == null) {
            throw new IllegalArgumentException("Falta el tipo de sala");
        }
        if (minutosLimpieza < 0) {
            throw new IllegalArgumentException("Los minutos de limpieza no pueden ser negativos");
        }
        this.nombre = nuevoNombre;
        this.tipo = tipo;
        this.minutosLimpieza = minutosLimpieza;
    }

    // Como se guarda: el gestor busca el nombre repetido con este valor, antes de editar.
    public static String normalizarNombre(String nombre) {
        return nombre == null ? "" : nombre.trim();
    }

    private static String nombreValido(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        String limpio = normalizarNombre(nombre);
        // El VARCHAR(50) de la tabla: pasado, MySQL rechaza el INSERT con un 500.
        if (limpio.length() > LARGO_MAXIMO_DEL_NOMBRE) {
            throw new IllegalArgumentException("El nombre no puede tener más de 50 caracteres");
        }
        return limpio;
    }

    // Creadora de sus butacas porque las contiene, y por eso dueña de la distribución. Solo en el
    // alta: editar no las toca. Un especial que no cae en la distribución se ignora.
    public List<Asiento> generarAsientos(List<Integer> butacasPorFila, Map<String, TipoAsiento> especiales) {
        if (butacasPorFila == null || butacasPorFila.isEmpty()) {
            throw new IllegalArgumentException("La sala necesita al menos una fila");
        }
        if (butacasPorFila.size() > MAX_FILAS) {
            throw new IllegalArgumentException("Máximo " + MAX_FILAS + " filas: se identifican con una letra");
        }
        if (butacasPorFila.stream().anyMatch(b -> b == null || b <= 0)) {
            throw new IllegalArgumentException("Cada fila debe tener al menos una butaca");
        }
        Map<String, TipoAsiento> porCodigo = porCodigoNormalizado(especiales);
        List<Asiento> asientos = new ArrayList<>();
        for (int fila = 1; fila <= butacasPorFila.size(); fila++) {
            for (int numero = 1; numero <= butacasPorFila.get(fila - 1); numero++) {
                TipoAsiento tipo = porCodigo.getOrDefault(Asiento.codigoDe(fila, numero), TipoAsiento.ESTANDAR);
                asientos.add(new Asiento(this, fila, numero, tipo));
            }
        }
        return asientos;
    }

    // Los códigos llegan como los tipeó el encargado (" a1"): se llevan a la forma que arma
    // Asiento.codigoDe, que es contra la que se buscan. Uno vacío no nombra ninguna butaca.
    private static Map<String, TipoAsiento> porCodigoNormalizado(Map<String, TipoAsiento> especiales) {
        Map<String, TipoAsiento> porCodigo = new HashMap<>();
        especiales.forEach((codigo, tipo) -> {
            String normalizado = Asiento.normalizarCodigo(codigo);
            if (!normalizado.isEmpty()) {
                porCodigo.put(normalizado, tipo);
            }
        });
        return porCodigo;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nombre + " - " + tipo;
    }
}
