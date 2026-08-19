package ar.uade.cine.servicio.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import ar.uade.cine.dominio.promociones.Promocion;
import ar.uade.cine.dominio.promociones.PromocionMontoFijo;
import ar.uade.cine.dominio.promociones.PromocionNxM;
import ar.uade.cine.dominio.promociones.PromocionPorcentaje;
import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.persistencia.PromocionDAO;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * La carta de promociones y, sobre todo, la regla que elige cuál se aplica.
 *
 * <p>Las promociones <strong>no se acumulan</strong> (R15): se evalúan todas las que
 * corren para esa función y ese medio de pago, y gana la que más plata le ahorra al
 * cliente. Aplicar varias en cadena obligaría a definir un orden —que cambia el
 * resultado— y a poner un piso para que el precio no llegue a cero.
 */
@Service
public class GestorPromociones implements PoliticaPromociones {

    private final PromocionDAO dao;

    public GestorPromociones(PromocionDAO dao) {
        this.dao = dao;
    }

    public Promocion crearPorcentaje(String nombre, double porcentaje, LocalDate desde, LocalDate hasta,
                                     Set<DayOfWeek> dias, LocalTime horaDesde, LocalTime horaHasta,
                                     Set<MedioPago> medios) {
        if (porcentaje <= 0 || porcentaje >= 100) {
            throw new IllegalArgumentException("El porcentaje tiene que estar entre 1 y 99");
        }
        return guardar(new PromocionPorcentaje(nombre, porcentaje, desde, hasta, dias,
                horaDesde, horaHasta, medios), nombre, desde, hasta);
    }

    public Promocion crearMontoFijo(String nombre, Dinero monto, LocalDate desde, LocalDate hasta,
                                    Set<DayOfWeek> dias, LocalTime horaDesde, LocalTime horaHasta,
                                    Set<MedioPago> medios) {
        if (monto == null || !monto.esMayorQue(Dinero.CERO)) {
            throw new IllegalArgumentException("El monto del descuento debe ser mayor a cero");
        }
        return guardar(new PromocionMontoFijo(nombre, monto, desde, hasta, dias,
                horaDesde, horaHasta, medios), nombre, desde, hasta);
    }

    public Promocion crearNxM(String nombre, int lleva, int paga, LocalDate desde, LocalDate hasta,
                              Set<DayOfWeek> dias, LocalTime horaDesde, LocalTime horaHasta,
                              Set<MedioPago> medios) {
        // Un 2x2 no descuenta nada y un 2x3 cobraría de más: sin esto, la promoción
        // existiría en la carta sin hacer nada, o haciendo lo contrario.
        if (lleva <= paga || paga <= 0) {
            throw new IllegalArgumentException("En un NxM hay que llevar más de lo que se paga");
        }
        return guardar(new PromocionNxM(nombre, lleva, paga, desde, hasta, dias,
                horaDesde, horaHasta, medios), nombre, desde, hasta);
    }

    private Promocion guardar(Promocion promocion, String nombre, LocalDate desde, LocalDate hasta) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("La promoción necesita un nombre");
        }
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La vigencia tiene que empezar antes de terminar");
        }
        if (dao.listar().stream().anyMatch(p -> p.getNombre().equalsIgnoreCase(nombre.trim()))) {
            throw new IllegalArgumentException("Ya hay una promoción llamada " + nombre);
        }
        dao.guardar(promocion);
        return promocion;
    }

    /**
     * La promoción que más descuenta para esa reserva, o vacío si no corre ninguna.
     *
     * <p>R16: las entradas de tarifa reducida quedan afuera del cálculo. Un jubilado ya
     * tiene su precio especial y no entra además al 2x1, que es como funciona en
     * cualquier cine. Por eso lo que se le pasa a la promoción es el subconjunto de
     * entradas generales, no la reserva entera.
     *
     * <p>Empate: gana la de menor id. Es arbitrario pero determinístico, que es lo que
     * hace falta para poder testearlo y para que dos cobros iguales den lo mismo.
     */
    public Optional<Promocion> mejorPara(List<Entrada> entradas, LocalDateTime inicioFuncion,
                                         MedioPago medio) {
        List<Entrada> alcanzadas = entradas.stream()
                .filter(e -> e.tarifa() == TipoTarifa.GENERAL)
                .toList();
        if (alcanzadas.isEmpty()) {
            return Optional.empty();
        }
        return dao.listarActivas().stream()
                .filter(p -> p.aplicaA(inicioFuncion, medio))
                .filter(p -> p.calcularDescuento(alcanzadas).esMayorQue(Dinero.CERO))
                // R15: gana la que mas descuenta. Comparar Dinero es exacto, asi que dos
                // promociones que descuentan lo mismo empatan de verdad y desempata el id.
                .max(Comparator.comparing((Promocion p) -> p.calcularDescuento(alcanzadas))
                        .thenComparing(Comparator.comparingInt(Promocion::getId).reversed()));
    }

    /**
     * Lo que ve quien cobra: el monto a descontar y de qué promoción salió. Que se elija
     * la que más descuenta, y que las tarifas reducidas queden afuera, es asunto de acá.
     */
    @Override
    public Descuento calcularPara(List<Entrada> entradas, LocalDateTime inicioFuncion,
                                  MedioPago medio) {
        return mejorPara(entradas, inicioFuncion, medio)
                .map(promocion -> new Descuento(promocion.getId(), descuentoDe(promocion, entradas)))
                .orElseGet(Descuento::ninguno);
    }

    /** Cuánto descuenta esa promoción sobre esas entradas, respetando R16. */
    public Dinero descuentoDe(Promocion promocion, List<Entrada> entradas) {
        return promocion.calcularDescuento(entradas.stream()
                .filter(e -> e.tarifa() == TipoTarifa.GENERAL)
                .toList());
    }

    /**
     * Desactivar reemplaza al borrado, igual que en el candy: una promoción ya usada en
     * un cobro tiene que seguir existiendo para poder explicar por qué se cobró eso.
     */
    public void desactivar(int id) {
        Promocion promocion = buscarOFallar(id);
        promocion.setActiva(false);
        dao.actualizar(promocion);
    }

    public void activar(int id) {
        Promocion promocion = buscarOFallar(id);
        promocion.setActiva(true);
        dao.actualizar(promocion);
    }

    public List<Promocion> listar() {
        return dao.listar();
    }

    public Optional<Promocion> buscar(int id) {
        return dao.buscarPorId(id);
    }

    private Promocion buscarOFallar(int id) {
        return dao.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la promoción " + id));
    }
}
