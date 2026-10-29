package ar.uade.cine.service.funciones;

import java.time.LocalDateTime;

import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.repository.cartelera.PeliculaRepository;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.salas.SalaRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;

// Dos pedidos a la vez: ninguno ve la función del otro al mirar R3 y los dos llegan a guardar.
public class GestorFuncionesQueNoVeLaCarrera extends GestorFunciones {

    public GestorFuncionesQueNoVeLaCarrera(FuncionRepository funcionRepository,
                                           PeliculaRepository peliculaRepository, SalaRepository salaRepository,
                                           ReservaRepository reservaRepository, Reloj reloj) {
        super(funcionRepository, peliculaRepository, salaRepository, reservaRepository, reloj);
    }

    @Override
    public AgendaDeSala agendaDe(int salaId, LocalDateTime desde, LocalDateTime hasta) {
        return AgendaDeSala.vacia();
    }
}
