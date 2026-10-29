package ar.uade.cine.swing.pantallas.informes;

import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiInformes;
import ar.uade.cine.swing.api.dto.catalogos.Tarifa;
import ar.uade.cine.swing.api.dto.informes.DeclaracionJurada;
import ar.uade.cine.swing.api.dto.informes.FuncionDeclarada;
import ar.uade.cine.swing.api.dto.informes.PeliculaDeclarada;
import ar.uade.cine.swing.api.dto.informes.Total;
import ar.uade.cine.swing.api.dto.informes.TotalDeclarado;
import ar.uade.cine.swing.comun.Archivos;
import ar.uade.cine.swing.comun.CampoFecha;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.FlujoConSalto;
import ar.uade.cine.swing.comun.Pila;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.informes.DeclaracionJuradaCsv;
import ar.uade.cine.swing.pantallas.Pantalla;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.cantidad;
import static ar.uade.cine.swing.comun.Formato.fechaHora;
import static ar.uade.cine.swing.comun.Formato.precio;

// La declaración jurada semanal al INCAA: los números los da el backend y el CSV se escribe en esta PC.
public final class PantallaDeclaracionJurada extends Pantalla {

    private record Datos(DeclaracionJurada declaracion, List<String> tarifas) {
    }

    private final ApiCatalogos apiCatalogos;
    private final ApiInformes apiInformes;
    private final CampoFecha desde = new CampoFecha(null);
    private final CampoFecha hasta = new CampoFecha(null);
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
            Columna.<FuncionDeclarada>numero("Neta", f -> precio(f.recaudacionNeta())))
            .vacia("No hubo funciones con entradas cobradas en ese período");
    private final Tabla<PeliculaDeclarada> peliculas = new Tabla<>(
            Columna.<PeliculaDeclarada>de("Película", PeliculaDeclarada::titulo).ancho(220),
            Columna.<PeliculaDeclarada>de("Edad", p -> etiqueta(p.clasificacion())),
            Columna.<PeliculaDeclarada>numero("Funciones", PeliculaDeclarada::funciones),
            Columna.<PeliculaDeclarada>de("Entradas", p -> entradas(p.entradasPorTarifa())).ancho(160),
            Columna.<PeliculaDeclarada>numero("Espectadores", PeliculaDeclarada::espectadores),
            Columna.<PeliculaDeclarada>numero("Bruta", p -> precio(p.recaudacionBruta())),
            Columna.<PeliculaDeclarada>numero("Descuentos", p -> precio(p.descuentos())),
            Columna.<PeliculaDeclarada>numero("Neta", p -> precio(p.recaudacionNeta())))
            .vacia("Sin películas para declarar en ese período");
    private Datos actual;
    private static final int MAX_FILAS = 8;

    public PantallaDeclaracionJurada(ApiCatalogos apiCatalogos, ApiInformes apiInformes) {
        super("Declaración jurada", "La semana cinematográfica, de jueves a miércoles, por función y por película. "
                + "Sin fechas, la última semana cerrada.");
        this.apiCatalogos = apiCatalogos;
        this.apiInformes = apiInformes;

        JButton consultar = Componentes.primario(new JButton("Consultar"));
        consultar.addActionListener(e -> consultar());
        exportar.addActionListener(e -> exportar());
        exportar.setEnabled(false);
        JPanel barra = new JPanel(new FlujoConSalto());
        barra.add(new JLabel("Desde"));
        barra.add(desde);
        barra.add(new JLabel("Hasta"));
        barra.add(hasta);
        barra.add(consultar);
        accionDeLaPantalla(exportar);

        encabezado.setForeground(Colores.secundario());
        Pila contenido = new Pila();
        contenido.agregar(barra, error, encabezado).espacio(Componentes.APARTE)
                .agregar(Componentes.subtitulo("Por función")).espacio(Componentes.JUNTO)
                .agregar(funciones.conScrollAjustado(MAX_FILAS)).espacio(Componentes.BLOQUE)
                .agregar(Componentes.subtitulo("Por película")).espacio(Componentes.JUNTO)
                .agregar(peliculas.conScrollAjustado(MAX_FILAS)).espacio(Componentes.APARTE)
                .agregar(total);
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(contenido, BorderLayout.NORTH);
        add(arriba, BorderLayout.CENTER);
        consultar();
    }

    // Las dos fechas o ninguna; que el período sea válido lo dice el backend.
    private void consultar() {
        boolean hayDesde = desde.valor() != null;
        boolean hayHasta = hasta.valor() != null;
        Validacion v = new Validacion(error);
        String inicio = v.fecha(desde, "Desde", hayHasta);
        String fin = v.fecha(hasta, "Hasta", hayDesde);
        v.alMencionar("período", desde);
        if (!v.ok()) return;
        exportar.setEnabled(false);
        Tarea.ejecutar(this, () -> new Datos(apiInformes.obtenerDeclaracionJurada(inicio, fin),
                apiCatalogos.obtenerTarifas().stream().map(Tarifa::nombre).toList()), this::pintar, v::mostrarError);
    }

    private void pintar(Datos datos) {
        actual = datos;
        DeclaracionJurada d = datos.declaracion();
        // Lo que eligió el backend queda a la vista: sin fechas, es él quien decide qué semana es "la última".
        desde.poner(LocalDate.parse(d.desde()));
        hasta.poner(LocalDate.parse(d.hasta()));
        encabezado.setText(d.exhibidor().razonSocial() + " · CUIT " + d.exhibidor().cuit() + " · exhibidor "
                + d.exhibidor().numeroExhibidor() + " · generada " + fechaHora(d.generadaEn()));
        funciones.mostrar(d.funciones());
        peliculas.mostrar(d.peliculas());
        TotalDeclarado t = d.total();
        // Sin entradas por tarifa no queda un «· ·» en el medio.
        String porTarifa = entradas(t.entradasPorTarifa());
        total.setText("<html><b>Total:</b> " + cantidad(t.funciones(), "función", "funciones") + " · "
                + cantidad(t.espectadores(), "espectador", "espectadores") + (porTarifa.isEmpty() ? "" : " · " + porTarifa)
                + " · bruta " + precio(t.recaudacionBruta()) + " · descuentos " + precio(t.descuentos())
                + " · <b>neta " + precio(t.recaudacionNeta()) + "</b></html>");
        exportar.setEnabled(true);
    }

    private void exportar() {
        Datos datos = actual;
        if (datos == null) return;
        Archivos.guardar(this, DeclaracionJuradaCsv.nombreArchivo(datos.declaracion()),
                        DeclaracionJuradaCsv.escribir(datos.declaracion(), datos.tarifas()), "el archivo")
                .ifPresent(destino -> avisar("Declaración jurada guardada en " + destino.toAbsolutePath()));
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
