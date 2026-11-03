package ar.uade.cine.swing.pantallas.funciones;

import ar.uade.cine.swing.api.ApiFunciones;
import ar.uade.cine.swing.api.dto.funciones.Borrado;
import ar.uade.cine.swing.api.dto.funciones.FuncionRechazada;
import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.pantallas.acciones.Pregunta;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.hora;

// Borrar las funciones elegidas en un solo pedido: qué preguntar, qué mandar y cómo resumir lo que volvió.
// No es un AccionDeFila: ese Command actúa sobre una fila, y acá son varias con un resultado por cada una.
record BorrarFunciones(ApiFunciones api) {

    // Más que esto en un diálogo no se lee: el resto queda contado.
    private static final int ERRORES_A_LA_VISTA = 10;

    Pregunta pregunta(List<Funcion> elegidas) {
        if (elegidas.size() > 1) return new Pregunta("¿Borrar " + elegidas.size() + " funciones?", "Sí, borrar");
        Funcion f = elegidas.get(0);
        return new Pregunta("¿Borrar la función de " + f.pelicula().titulo() + " del " + dia(f.inicio())
                + " " + hora(f.inicio()) + "?", "Sí, borrar");
    }

    Borrado ejecutar(List<Funcion> elegidas) {
        return api.borrarFunciones(elegidas.stream().map(Funcion::id).toList());
    }

    /** Lo que va a la barra de estado; sin ninguna borrada no hay éxito que avisar. */
    static Optional<String> resumen(Borrado borrado) {
        int borradas = borrado.borradas().size();
        int rechazadas = borrado.rechazadas().size();
        if (borradas == 0) return Optional.empty();
        String hechas = borradas == 1 ? "Se borró 1 función" : "Se borraron " + borradas + " funciones";
        if (rechazadas == 0) return Optional.of(borradas == 1 ? "Función borrada" : hechas);
        return Optional.of(hechas + "; " + rechazadas + (rechazadas == 1 ? " no se pudo borrar y quedó seleccionada"
                : " no se pudieron borrar y quedaron seleccionadas"));
    }

    /** El porqué de cada rechazada, con el texto del backend tal cual. */
    static Optional<String> errores(Borrado borrado) {
        List<FuncionRechazada> rechazadas = borrado.rechazadas();
        if (rechazadas.isEmpty()) return Optional.empty();
        String vistos = rechazadas.stream().limit(ERRORES_A_LA_VISTA).map(FuncionRechazada::error)
                .collect(Collectors.joining("\n"));
        int resto = rechazadas.size() - ERRORES_A_LA_VISTA;
        return Optional.of(resto > 0 ? vistos + "\ny " + resto + " más" : vistos);
    }
}
