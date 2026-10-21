package ar.uade.cine.service.cartelera;

import ar.uade.cine.model.cartelera.ResumenDeCorrida;

// Acumula lo que va pasando en una corrida del importador y entrega el resumen; dueña del formato del detalle.
// Experto: antes GestorImportaciones llevaba tres contadores y armaba los renglones (Feature Envy sobre el resumen).
// Un renglón por película: «+ [id] título» si entró, «✗ título: motivo» si falló; las salteadas solo se cuentan.
class Corrida {

    private final StringBuilder detalle = new StringBuilder();
    private int nuevas;
    private int salteadas;
    private int fallidas;

    void nueva(int id, String titulo) {
        detalle.append("+ [").append(id).append("] ").append(titulo).append('\n');
        nuevas++;
    }

    void salteada() {
        salteadas++;
    }

    void fallida(String nombre, String motivo) {
        detalle.append("✗ ").append(nombre).append(": ").append(motivo).append('\n');
        fallidas++;
    }

    ResumenDeCorrida resumen() {
        return new ResumenDeCorrida(nuevas, salteadas, fallidas,
                detalle.isEmpty() ? null : detalle.toString().strip());
    }
}
