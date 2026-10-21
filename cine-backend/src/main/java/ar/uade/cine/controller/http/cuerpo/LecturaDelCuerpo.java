package ar.uade.cine.controller.http.cuerpo;

import java.util.Optional;

import org.springframework.http.converter.HttpMessageNotReadableException;

// Un eslabón que reconoce un tipo de cuerpo ilegible y dice qué corregir.
// Patrón Chain of Responsibility: cuatro if en fila dentro de ManejadorErrores, cuyo orden importaba y no tenía
// nombre, pasaron a ser eslabones; cada uno contesta lo suyo o devuelve vacío para que pruebe el siguiente.
public interface LecturaDelCuerpo {

    Optional<String> mensaje(HttpMessageNotReadableException e);
}
