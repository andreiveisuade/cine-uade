package ar.uade.cine.swing.pantallas;

// Cómo una pantalla lleva a otra; las crea quien tiene todas las Api, y cada una recibe solo las suyas.
public interface Navegacion {

    /** Va a una entrada del menú, y la marca. */
    void ir(Destino destino);

    /** El borderó y el informe de una función. No está en el menú: se llega desde Funciones o desde la Agenda. */
    void abrirInforme(int funcionId);

    /** El cobro de una reserva, desde Reservas. */
    void abrirCobro(int reservaId);

    /** El mapa de una sala, para marcar butacas fuera de servicio, desde Salas. */
    void abrirMapa(int salaId);
}
