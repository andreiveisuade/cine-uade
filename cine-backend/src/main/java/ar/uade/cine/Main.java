package ar.uade.cine;

import ar.uade.cine.interfaces.PeliculaDAO;
import ar.uade.cine.persistencia.PeliculaDAOMySQL;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.ui.MenuConsola;

public class Main {

    public static void main(String[] args) {
        // Acá se elige la implementación. Es la ÚNICA línea que cambia
        // para pasar de MySQL a memoria (o mañana a un DAO de archivos TXT).
        PeliculaDAO dao = new PeliculaDAOMySQL();
        // PeliculaDAO dao = new PeliculaDAOMemoria();

        GestorCartelera gestor = new GestorCartelera(dao);
        new MenuConsola(gestor).iniciar();
    }
}
