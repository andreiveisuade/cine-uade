package ar.uade.cine.controller.http.cuerpo;

import java.util.Optional;

import org.springframework.http.converter.HttpMessageNotReadableException;

// Un eslabón que reconoce un tipo de cuerpo ilegible y dice qué corregir.
// Patrón Chain of Responsibility: el orden de los chequeos importaba; cada eslabón contesta lo suyo o devuelve vacío.
public interface LecturaDelCuerpo {

    Optional<String> mensaje(HttpMessageNotReadableException e);
}
