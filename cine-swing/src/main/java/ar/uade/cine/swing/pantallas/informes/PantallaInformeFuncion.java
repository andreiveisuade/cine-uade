package ar.uade.cine.swing.pantallas.informes;

import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiFunciones;
import ar.uade.cine.swing.api.ApiInformes;
import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.api.dto.informes.Bordero;
import ar.uade.cine.swing.api.dto.informes.InformeFuncion;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Pantalla;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.hora;
import static ar.uade.cine.swing.comun.Formato.precio;

// Borderó e informe de una función, lado a lado: juntos porque los dos se piden por funcionId.
public final class PantallaInformeFuncion extends Pantalla {

    private record Datos(Funcion funcion, Bordero bordero, InformeFuncion informe) {
    }

    private final ApiCatalogos apiCatalogos;
    private final ApiInformes apiInformes;
    private final int funcionId;
    private final JPanel cuerpo = new JPanel(new GridLayout(1, 2, 16, 0));
    private final JLabel subtitulo = new JLabel(" ");

    public PantallaInformeFuncion(ApiCatalogos apiCatalogos, ApiFunciones apiFunciones, ApiInformes apiInformes,
                                  Navegacion navegacion, int funcionId) {
        super("Borderó e informe", null);
        this.apiCatalogos = apiCatalogos;
        this.apiInformes = apiInformes;
        this.funcionId = funcionId;

        JButton volver = new JButton("← Funciones");
        volver.addActionListener(e -> navegacion.ir(Destino.FUNCIONES));
        JPanel norte = new JPanel(new BorderLayout(0, 6));
        norte.add(volver, BorderLayout.WEST);
        norte.add(subtitulo, BorderLayout.SOUTH);
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(norte, BorderLayout.NORTH);
        arriba.add(cuerpo, BorderLayout.CENTER);
        add(arriba, BorderLayout.CENTER);

        cargar(() -> new Datos(apiFunciones.obtenerFuncion(funcionId), apiInformes.obtenerBordero(funcionId),
                apiInformes.obtenerInformeDeFuncion(funcionId)), this::pintar);
    }

    private void pintar(Datos datos) {
        Funcion f = datos.funcion();
        subtitulo.setText("<html><span style='font-size:18pt'><b>" + f.pelicula().titulo() + "</b></span><br>"
                + dia(f.inicio()) + " " + hora(f.inicio()) + " · " + f.sala().nombre() + " ("
                + etiqueta(f.sala().tipo()) + ") · " + etiqueta(f.proyeccion()) + " · " + etiqueta(f.idioma())
                + " · precio base " + precio(f.precio()) + " · " + (f.libres() == null ? "?" : f.libres())
                + " de " + f.sala().capacidadSala() + " butacas libres</html>");
        cuerpo.removeAll();
        cuerpo.add(new PanelBordero(apiCatalogos, apiInformes, funcionId, datos.bordero()));
        cuerpo.add(new PanelInforme(datos.informe()));
        cuerpo.revalidate();
        cuerpo.repaint();
    }
}
