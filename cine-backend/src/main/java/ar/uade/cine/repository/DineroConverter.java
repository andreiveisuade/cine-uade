package ar.uade.cine.repository;

import java.math.BigDecimal;

import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Cómo viaja la plata entre el dominio y la base: {@link Dinero} de un lado, DECIMAL(10,2)
 * del otro.
 *
 * <p>{@code Dinero} guarda centavos enteros a propósito —un {@code double} no puede
 * representar 0,10 exactamente y las cuentas mienten de a poco— pero las columnas son
 * DECIMAL porque el schema ya está desplegado y porque es lo que cualquiera espera ver al
 * abrir la tabla en Adminer. Esta clase es el único lugar donde se cruza esa frontera, que
 * antes eran cincuenta y nueve {@code rs.getDouble("precio")} repartidos por los DAO.
 *
 * <p>{@code autoApply = true}: se aplica sola a todo campo de tipo Dinero, así que ninguna
 * entidad tiene que acordarse de anotarlo. Una entidad nueva con un importe adentro ya
 * queda bien mapeada sin hacer nada.
 */
@Converter(autoApply = true)
public class DineroConverter implements AttributeConverter<Dinero, BigDecimal> {

    /** Dos posiciones: de centavos a pesos y al reves, sin dividir ni multiplicar. */
    private static final int DECIMALES = 2;

    @Override
    public BigDecimal convertToDatabaseColumn(Dinero dinero) {
        return dinero == null ? null : BigDecimal.valueOf(dinero.centavos()).movePointLeft(DECIMALES);
    }

    @Override
    public Dinero convertToEntityAttribute(BigDecimal pesos) {
        return pesos == null ? null : Dinero.deCentavos(pesos.movePointRight(DECIMALES).longValueExact());
    }
}
