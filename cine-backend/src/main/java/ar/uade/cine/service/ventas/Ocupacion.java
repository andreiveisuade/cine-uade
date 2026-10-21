package ar.uade.cine.service.ventas;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.ventas.BloqueoButaca;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.SesionDeCompra;
import ar.uade.cine.repository.ventas.BloqueoButacaRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;

// Única definición de butaca ocupada (R4), la del mapa y la venta; reservas vigentes más bloqueos ajenos.
// Alta cohesión (GRASP) / SRP: antes también bloqueaba, exigía R19 y vencía reservas; eso vive en
// BloqueoDeButacas, FuncionesEnVenta y ExpiracionDeReservas, y acá queda una sola razón para cambiar.
@Service
@RequiredArgsConstructor
public class Ocupacion {

    private final ReservaRepository reservaRepository;
    private final BloqueoButacaRepository bloqueos;
    private final ExpiracionDeReservas expiracion;
    private final Reloj reloj;

    // Expira al consultar (R17 sin scheduler): decisión consciente, aunque mezcle consulta y escritura.
    public Set<Integer> asientosOcupados(int funcionId, String sesion) {
        String propia = SesionDeCompra.comoSeGuarda(sesion);
        List<Reserva> reservas = reservaRepository.findByFuncion_Id(funcionId);
        expiracion.expirarVencidas(reservas);
        Set<Integer> ocupados = reservas.stream()
                .filter(Reserva::estaVigente)
                .flatMap(r -> r.getEntradas().stream())
                .map(Entrada::asientoId)
                .collect(Collectors.toCollection(HashSet::new));
        for (BloqueoButaca bloqueo : bloqueos.vigentes(funcionId, reloj.ahora())) {
            if (!bloqueo.sesion().equals(propia)) {
                ocupados.add(bloqueo.asientoId());
            }
        }
        return ocupados;
    }

    public static List<Asiento> libresEntre(List<Asiento> asientos, Set<Integer> ocupados) {
        return asientos.stream()
                .filter(a -> !a.estaFueraDeServicio())
                .filter(a -> !ocupados.contains(a.getId()))
                .toList();
    }
}
