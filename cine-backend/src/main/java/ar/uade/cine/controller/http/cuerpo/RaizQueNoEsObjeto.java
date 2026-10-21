package ar.uade.cine.controller.http.cuerpo;

import java.util.Optional;

import org.springframework.http.converter.HttpMessageNotReadableException;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;

// Eslabón de la raíz que no encaja: un array, un número o un texto son JSON válido, pero no un pedido.
// Va después de CampoConValorInvalido: un tipo equivocado con camino es de un campo, no de la raíz.
public class RaizQueNoEsObjeto implements LecturaDelCuerpo {

    @Override
    public Optional<String> mensaje(HttpMessageNotReadableException e) {
        return e.getCause() instanceof MismatchedInputException
                ? Optional.of("El cuerpo del pedido tiene que ser un objeto JSON")
                : Optional.empty();
    }
}
