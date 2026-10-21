package ar.uade.cine.service.ventas;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.repository.funciones.FuncionRepository;

// R19: una función que ya arrancó no se vende, así que tampoco se le apartan butacas.
// SRP: salió de Ocupacion; cambia solo si cambia R19.
@Service
@RequiredArgsConstructor
public class FuncionesEnVenta {

    // R19 dice lo mismo al bloquear y al vender: los dos pasan por exigir.
    private static final String FUNCION_EMPEZADA = "La función ya empezó: no se pueden reservar butacas";

    private final FuncionRepository funcionRepository;
    private final Reloj reloj;

    // Va antes que cualquier otra regla de la venta porque las anula a todas.
    public Funcion exigir(int funcionId) {
        Funcion funcion = funcionRepository.exigir(funcionId, "la función");
        if (funcion.yaEmpezo(reloj.ahora())) {
            throw new DatoInvalido(FUNCION_EMPEZADA);
        }
        return funcion;
    }
}
