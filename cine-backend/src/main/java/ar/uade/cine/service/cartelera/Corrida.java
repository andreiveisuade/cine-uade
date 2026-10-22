package ar.uade.cine.service.cartelera;

import ar.uade.cine.model.cartelera.RenglonDeCorrida;
import ar.uade.cine.model.cartelera.ResumenDeCorrida;

// Acumula lo que va pasando en una corrida del importador y entrega el resumen.
// Un renglón por película, también las que ya estaban: el formato es de RenglonDeCorrida.
class Corrida {

    private final StringBuilder detalle = new StringBuilder();
    private int nuevas;
    private int salteadas;
    private int fallidas;

    void nueva(int id, String titulo) {
        anotar(RenglonDeCorrida.nueva(id, titulo));
        nuevas++;
    }

    void salteada(String titulo) {
        anotar(RenglonDeCorrida.yaEstaba(titulo));
        salteadas++;
    }

    void fallida(String nombre, String motivo) {
        anotar(RenglonDeCorrida.rechazada(nombre, motivo));
        fallidas++;
    }

    private void anotar(RenglonDeCorrida renglon) {
        detalle.append(renglon.texto()).append('\n');
    }

    ResumenDeCorrida resumen() {
        return new ResumenDeCorrida(nuevas, salteadas, fallidas,
                detalle.isEmpty() ? null : detalle.toString().strip());
    }
}
