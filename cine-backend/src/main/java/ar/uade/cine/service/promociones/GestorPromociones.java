package ar.uade.cine.service.promociones;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.imagenes.ImagenUrl;
import ar.uade.cine.model.promociones.CondicionesPromocion;
import ar.uade.cine.model.promociones.ParametrosPromocion;
import ar.uade.cine.model.promociones.Promocion;
import ar.uade.cine.model.promociones.TipoPromocion;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.repository.promociones.PromocionRepository;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;

// Alta, edición, activación y mejor promoción al cobrar (R15, R16); implementa PoliticaPromociones.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorPromociones implements PoliticaPromociones {

    private final PromocionRepository promocionRepository;
    private final Reloj reloj;

    // Un solo alta para los tres tipos: el tipo crea su subclase (Factory Method en TipoPromocion).
    // El nombre repetido necesita el repositorio: se busca el ya recortado por la entidad.
    public Promocion crear(TipoPromocion tipo, String nombre, ParametrosPromocion parametros,
                           CondicionesPromocion condiciones, ImagenUrl imagen) {
        Promocion promocion = tipo.crear(nombre, parametros, condiciones, reloj.hoy());
        promocion.cambiarImagen(imagen);
        if (promocionRepository.existsByNombreIgnoreCase(promocion.getNombre())) {
            throw new ConflictoDeNegocio("Ya existe una promoción con ese nombre");
        }
        return promocionRepository.save(promocion);
    }

    // Una ya usada en cobros se edita igual: cada Pago congela su descuento al cobrar.
    public Promocion editar(int id, TipoPromocion tipo, String nombre, ParametrosPromocion parametros,
                            CondicionesPromocion condiciones, ImagenUrl imagen) {
        Promocion promocion = obtener(id);
        promocion.editar(tipo, nombre, parametros, condiciones, reloj.hoy());
        promocion.cambiarImagen(imagen);
        if (promocionRepository.existsByNombreIgnoreCaseAndIdNot(promocion.getNombre(), id)) {
            throw new ConflictoDeNegocio("Ya existe una promoción con ese nombre");
        }
        return promocion;
    }

    // R16: solo participan las tarifas que lo dicen (TipoTarifa#participaDePromociones).
    // Empate: gana la de menor id, para que el cobro sea determinístico.
    @Transactional(readOnly = true)
    @Override
    public Descuento calcularPara(List<Entrada> entradas, LocalDateTime inicioFuncion,
                                  MedioPago medio) {
        return mejorDescuento(entradas, inicioFuncion, medio)
                .map(c -> new Descuento(c.promocion().getId(), c.promocion().getNombre(), c.monto()))
                .orElseGet(Descuento::ninguno);
    }

    private record Candidata(Promocion promocion, Dinero monto) {
    }

    private Optional<Candidata> mejorDescuento(List<Entrada> entradas, LocalDateTime inicioFuncion,
                                               MedioPago medio) {
        List<Entrada> alcanzadas = entradas.stream()
                .filter(e -> e.tarifa().participaDePromociones())
                .toList();
        if (alcanzadas.isEmpty()) {
            return Optional.empty();
        }
        return promocionRepository.findByActivaTrue().stream()
                .filter(p -> p.aplicaA(inicioFuncion, medio))
                .map(p -> new Candidata(p, p.calcularDescuento(alcanzadas)))
                .filter(c -> c.monto().esMayorQue(Dinero.CERO))
                .max(Comparator.comparing(Candidata::monto)
                        .thenComparing(c -> c.promocion().getId(), Comparator.reverseOrder()));
    }

    public Promocion desactivar(int id) {
        Promocion promocion = obtener(id);
        promocion.desactivar();
        return promocion;
    }

    public Promocion activar(int id) {
        Promocion promocion = obtener(id);
        promocion.activar();
        return promocion;
    }

    @Transactional(readOnly = true)
    public List<Promocion> listar() {
        return promocionRepository.findAll();
    }

    // Las que se anuncian al público: activas y sin terminar, la próxima a arrancar primero.
    @Transactional(readOnly = true)
    public List<Promocion> vigentes() {
        return promocionRepository.findByActivaTrue().stream()
                .filter(p -> p.sigueVigente(reloj.hoy()))
                .sorted(Comparator.comparing((Promocion p) -> p.getVigencia().desde()).thenComparing(Promocion::getId))
                .toList();
    }

    @Transactional(readOnly = true)
    public Promocion obtener(int id) {
        return promocionRepository.exigir(id, "la promoción");
    }
}
