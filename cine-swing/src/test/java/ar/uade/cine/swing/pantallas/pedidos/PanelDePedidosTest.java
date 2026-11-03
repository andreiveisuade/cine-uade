package ar.uade.cine.swing.pantallas.pedidos;

import ar.uade.cine.swing.api.RegistroDePedidos;
import ar.uade.cine.swing.api.RegistroDePedidos.Pedido;
import ar.uade.cine.swing.api.RegistroDePedidos.Resultado;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelDePedidosTest {

    private final RegistroDePedidos registro = new RegistroDePedidos();
    private final PanelDePedidos panel = new PanelDePedidos(registro);

    private Pedido empezar(String metodo, String ruta) {
        return registro.empezar(metodo, ruta, "http://cine/api" + ruta, 0);
    }

    private void esperarAlEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    @Test
    void elBotonMuestraYOcultaElPanel() {
        assertFalse(panel.isVisible());
        panel.boton().doClick();
        assertTrue(panel.isVisible());
        panel.boton().doClick();
        assertFalse(panel.isVisible());
    }

    @Test
    void muestraLoQueCargaYComoTerminaCadaPedido() throws Exception {
        Pedido bien = empezar("GET", "/peliculas");
        Pedido mal = empezar("DELETE", "/peliculas/3");
        empezar("GET", "/salas");
        esperarAlEdt();
        assertEquals("Pedidos · 3 cargando", panel.boton().getText());

        registro.terminar(bien, new Resultado("200", 2048, "application/json", null));
        registro.terminar(mal, new Resultado("409", 40, "application/json", "La película tiene funciones"));
        esperarAlEdt();

        assertEquals("Pedidos · 1 cargando", panel.boton().getText());
        assertEquals("1 cargando · 1 bien · 1 con error", panel.resumen());
        assertEquals(3, panel.tabla().tabla().getRowCount());
        assertEquals("/salas", panel.tabla().tabla().getValueAt(0, 2));
        assertEquals("Cargando", panel.tabla().tabla().getValueAt(0, 3));
        assertEquals("409", panel.tabla().tabla().getValueAt(1, 3));
    }

    @Test
    void elDetalleDelElegidoSobreviveAlRefrescoYTraeElError() throws Exception {
        Pedido mal = empezar("DELETE", "/peliculas/3");
        esperarAlEdt();
        panel.tabla().tabla().setRowSelectionInterval(0, 0);

        registro.terminar(mal, new Resultado("409", 2048, "application/json", "La película tiene funciones"));
        empezar("GET", "/salas");
        esperarAlEdt();

        assertEquals(1, panel.tabla().tabla().getSelectedRow(), "el pedido nuevo entra arriba y la selección lo sigue");
        assertTrue(panel.detalle().contains("DELETE http://cine/api/peliculas/3"), panel.detalle());
        assertTrue(panel.detalle().contains("Estado: 409"), panel.detalle());
        assertTrue(panel.detalle().contains("Recibido: 2,0 KB"), panel.detalle());
        assertTrue(panel.detalle().contains("Error: La película tiene funciones"), panel.detalle());
    }

    @Test
    void noGuardaLosValoresDeLaQueryNiMasDeDoscientosPedidos() {
        empezar("GET", "/clientes?email=alguien@mail.com&activo=true");
        assertEquals("/clientes?email=…&activo=…", registro.recientes().getFirst().ruta());
        assertFalse(registro.recientes().getFirst().url().contains("alguien"));

        for (int i = 0; i < 250; i++) empezar("GET", "/salas");
        assertEquals(200, registro.recientes().size());
    }
}
