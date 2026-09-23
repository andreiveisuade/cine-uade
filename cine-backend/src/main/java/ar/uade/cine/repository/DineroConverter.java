package ar.uade.cine.repository;

import java.math.BigDecimal;

import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Único cruce entre {@link Dinero} (centavos) y las columnas DECIMAL(10,2), que se leen
 * bien en Adminer. {@code autoApply}: ninguna entidad tiene que acordarse de anotarlo.
 */
@Converter(autoApply = true)
public class DineroConverter implements AttributeConverter<Dinero, BigDecimal> {

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
