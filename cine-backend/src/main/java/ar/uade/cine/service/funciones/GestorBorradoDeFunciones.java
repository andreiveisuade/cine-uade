package ar.uade.cine.service.funciones;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.validacion.Regla;

// Borra varias funciones en un pedido; cada una por GestorFunciones.eliminar, con su R12 y su 404.
// Bean aparte y sin transacción compartida, como extenderActivas: el catch la dejaría rollback-only.
@Service
@RequiredArgsConstructor
@Slf4j
public class GestorBorradoDeFunciones {

    public static final int MAXIMO_POR_PEDIDO = 500;

    private final GestorFunciones funciones;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Resultado borrar(List<Integer> ids) {
        Regla.lista(ids).noVacia("Hay que elegir al menos una función")
                .sinNulos("Falta el id de la función")
                .hasta(MAXIMO_POR_PEDIDO, "Un borrado tiene que tener como máximo " + MAXIMO_POR_PEDIDO + " funciones");
        List<Integer> borradas = new ArrayList<>();
        List<Rechazada> rechazadas = new ArrayList<>();
        for (int id : ids.stream().distinct().toList()) {
            try {
                funciones.eliminar(id);
                borradas.add(id);
            } catch (Rechazo e) {
                rechazadas.add(new Rechazada(id, e.getMessage()));
            }
        }
        log.info("borrado en lote · {} funciones borradas · {} rechazadas", borradas.size(), rechazadas.size());
        return new Resultado(borradas, rechazadas);
    }

    public record Resultado(List<Integer> borradas, List<Rechazada> rechazadas) {
    }

    public record Rechazada(int id, String motivo) {
    }
}
