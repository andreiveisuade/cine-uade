package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.ErrorApi;
import ar.uade.cine.swing.api.dto.DeclaracionJurada;
import ar.uade.cine.swing.api.dto.FuncionDeclarada;
import ar.uade.cine.swing.api.dto.PeliculaDeclarada;
import ar.uade.cine.swing.api.dto.Tarifa;
import ar.uade.cine.swing.api.dto.Total;
import ar.uade.cine.swing.api.dto.TotalDeclarado;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Fechas;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.informes.DeclaracionJuradaCsv;
import com.toedter.calendar.JDateChooser;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.fechaHora;
import static ar.uade.cine.swing.comun.Formato.precio;

/**
 * La declaración jurada semanal al INCAA. Los números los da el backend; el archivo lo escribe esta PC con
 * {@link DeclaracionJuradaCsv}. Sin fechas, el backend devuelve la última semana cinematográfica (jueves a miércoles).
 */
final class PantallaDeclaracionJurada extends Pantalla {

    private record Datos(DeclaracionJurada declaracion, List<String> tarifas) {
    }

    private final JDateChooser desde = Fechas.selector(null);
    private final JDateChooser hasta = Fechas.selector(null);
    private final JLabel encabezado = new JLabel(" ");
    private final JLabel total = new JLabel(" ");
    private final JLabel error = Componentes.texto(" ");
    private final JButton exportar = new JButton("Exportar CSV");
    private final Tabla<FuncionDeclarada> funciones = new Tabla<>(
            Columna.<FuncionDeclarada>de("Función", f -> fechaHora(f.inicio())).ancho(120),
            Columna.<FuncionDeclarada>de("Sala", FuncionDeclarada::sala),
            Columna.<FuncionDeclarada>de("Película", FuncionDeclarada::pelicula).ancho(200),
            Columna.<FuncionDeclarada>de("Edad", f -> etiqueta(f.clasificacion())),
            Columna.<FuncionDeclarada>de("Formato", f -> etiqueta(f.proyeccion()) + " · " + etiqueta(f.idioma())),
            Columna.<FuncionDeclarada>de("Entradas", f -> porTarifa(f.porTarifa())).ancho(160),
            Columna.<FuncionDeclarada>numero("Espectadores", FuncionDeclarada::espectadores),
            Columna.<FuncionDeclarada>numero("Bruta", f -> precio(f.recaudacionBruta())),
            Columna.<FuncionDeclarada>numero("Descuentos", f -> precio(f.descuentos())),
            Columna.<FuncionDeclarada>numero("Neta", f -> precio(f.recaudacionNeta())));
    private final Tabla<PeliculaDeclarada> peliculas = new Tabla<>(
            Columna.<PeliculaDeclarada>de("Película", PeliculaDeclarada::titulo).ancho(220),
            Columna.<PeliculaDeclarada>de("Edad", p -> etiqueta(p.clasificacion())),
            Columna.<PeliculaDeclarada>numero("Funciones", PeliculaDeclarada::funciones),
            Columna.<PeliculaDeclarada>de("Entradas", p -> entradas(p.entradasPorTarifa())).ancho(160),
            Columna.<PeliculaDeclarada>numero("Espectadores", PeliculaDeclarada::espectadores),
            Columna.<PeliculaDeclarada>numero("Bruta", p -> precio(p.recaudacionBruta())),
            Columna.<PeliculaDeclarada>numero("Descuentos", p -> precio(p.descuentos())),
            Columna.<PeliculaDeclarada>numero("Neta", p -> precio(p.recaudacionNeta())));
    private Datos actual;

    PantallaDeclaracionJurada(ApiHttp api) {
        super(api, "Declaración jurada", "Lo cobrado en la semana cinematográfica, de jueves a miércoles, por función "
                + "y por película. Sin fechas se muestra la última semana cerrada.");
        JButton consultar = new JButton("Consultar");
        consultar.addActionListener(e -> consultar());
        exportar.addActionListener(e -> exportar());
        exportar.setEnabled(false);
        JPanel barra = new JPanel(new FlujoConSalto());
        barra.add(new JLabel("Desde"));
        barra.add(desde);
        barra.add(new JLabel("Hasta"));
        barra.add(hasta);
        barra.add(consultar);
        barra.add(exportar);

        JPanel norte = new JPanel(new BorderLayout(0, 6));
        norte.add(barra, BorderLayout.NORTH);
        norte.add(error, BorderLayout.CENTER);
        norte.add(encabezado, BorderLayout.SOUTH);
        JPanel arriba = new JPanel(new BorderLayout(0, 4));
        arriba.add(Componentes.subtitulo("Por función"), BorderLayout.NORTH);
        arriba.add(funciones.conScroll());
        JPanel abajo = new JPanel(new BorderLayout(0, 4));
        abajo.add(Componentes.subtitulo("Por película"), BorderLayout.NORTH);
        abajo.add(peliculas.conScroll());
        JSplitPane tablas = new JSplitPane(JSplitPane.VERTICAL_SPLIT, arriba, abajo);
        tablas.setResizeWeight(0.6);
        tablas.setBorder(null);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(norte, BorderLayout.NORTH);
        centro.add(tablas, BorderLayout.CENTER);
        centro.add(total, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
        consultar();
    }

    // Las dos fechas o ninguna: con una sola el pedido está incompleto, y la que falta es obligatoria. Que el período
    // sea válido (desde antes que hasta, hasta 31 días) lo dice el backend.
    private void consultar() {
        boolean hayDesde = Fechas.leer(desde) != null;
        boolean hayHasta = Fechas.leer(hasta) != null;
        Validacion v = new Validacion(error);
        String inicio = v.fecha(desde, "Desde", hayHasta);
        String fin = v.fecha(hasta, "Hasta", hayDesde);
        v.alMencionar("período", desde);
        if (!v.ok()) return;
        exportar.setEnabled(false);
        Tarea.ejecutar(this, () -> new Datos(api.obtenerDeclaracionJurada(inicio, fin),
                api.obtenerTarifas().stream().map(Tarifa::nombre).toList()), this::pintar, v::mostrarError);
    }

    private void pintar(Datos datos) {
        actual = datos;
        DeclaracionJurada d = datos.declaracion();
        // Lo que eligió el backend queda a la vista: sin fechas, es él quien decide qué semana es "la última".
        Fechas.poner(desde, LocalDate.parse(d.desde()));
        Fechas.poner(hasta, LocalDate.parse(d.hasta()));
        encabezado.setText(d.exhibidor().razonSocial() + " · CUIT " + d.exhibidor().cuit() + " · exhibidor "
                + d.exhibidor().numeroExhibidor() + " · generada " + fechaHora(d.generadaEn()));
        funciones.mostrar(d.funciones());
        peliculas.mostrar(d.peliculas());
        TotalDeclarado t = d.total();
        total.setText("<html><b>Total:</b> " + t.funciones() + " funciones · " + t.espectadores() + " espectadores · "
                + entradas(t.entradasPorTarifa()) + " · bruta " + precio(t.recaudacionBruta()) + " · descuentos "
                + precio(t.descuentos()) + " · <b>neta " + precio(t.recaudacionNeta()) + "</b></html>");
        exportar.setEnabled(true);
    }

    private void exportar() {
        Datos datos = actual;
        if (datos == null) return;
        JFileChooser elegir = new JFileChooser();
        elegir.setSelectedFile(new File(DeclaracionJuradaCsv.nombreArchivo(datos.declaracion())));
        if (elegir.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File destino = elegir.getSelectedFile();
        try {
            Files.writeString(destino.toPath(), DeclaracionJuradaCsv.escribir(datos.declaracion(), datos.tarifas()),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            Tarea.mostrarError(this, new ErrorApi(0, "No se pudo guardar el archivo: " + e.getMessage()));
            return;
        }
        avisar("Declaración jurada guardada en " + destino.getAbsolutePath());
    }

    private static String porTarifa(Map<String, Total> porTarifa) {
        return porTarifa.entrySet().stream().map(e -> etiqueta(e.getKey()) + " " + e.getValue().cantidad())
                .collect(Collectors.joining(" · "));
    }

    private static String entradas(Map<String, Integer> porTarifa) {
        return porTarifa.entrySet().stream().map(e -> etiqueta(e.getKey()) + " " + e.getValue())
                .collect(Collectors.joining(" · "));
    }
}
