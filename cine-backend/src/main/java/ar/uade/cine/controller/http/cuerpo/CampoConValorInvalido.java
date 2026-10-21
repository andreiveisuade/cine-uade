package ar.uade.cine.controller.http.cuerpo;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.converter.HttpMessageNotReadableException;

import com.fasterxml.jackson.core.exc.InputCoercionException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

// Eslabón del JSON bien formado con un tipo equivocado ("precio": "abc"): no es "JSON inválido", y el mensaje
// nombra el campo, que es lo que el usuario puede corregir.
public class CampoConValorInvalido implements LecturaDelCuerpo {

    private static final Pattern FUERA_DE_RANGO = Pattern.compile("^Numeric value \\((.+?)\\) out of range");

    @Override
    public Optional<String> mensaje(HttpMessageNotReadableException e) {
        if (!(e.getCause() instanceof JsonMappingException mapeo) || mapeo.getPath().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("El campo " + campo(mapeo) + " tiene un valor inválido" + valorDe(mapeo));
    }

    private static String campo(JsonMappingException mapeo) {
        return mapeo.getPath().stream()
                .map(r -> r.getFieldName() != null ? r.getFieldName() : String.valueOf(r.getIndex()))
                .collect(Collectors.joining("."));
    }

    // Un número que no entra en un int ({"funcionId": 99999999999}) lo rechaza el parser, que ya
    // cerró cuando llega acá: el valor sobrevive solo en su mensaje. Si Jackson lo cambiara, el
    // texto pierde el valor pero sigue nombrando el campo.
    private static String valorDe(JsonMappingException e) {
        if (e instanceof InvalidFormatException formato) {
            return ": " + formato.getValue();
        }
        if (e.getCause() instanceof InputCoercionException desborde) {
            Matcher valor = FUERA_DE_RANGO.matcher(desborde.getOriginalMessage());
            return valor.find() ? ": " + valor.group(1) : "";
        }
        return "";
    }
}
