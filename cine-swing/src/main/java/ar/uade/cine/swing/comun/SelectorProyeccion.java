package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.catalogos.TipoSala;
import ar.uade.cine.swing.api.dto.salas.Sala;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// El combo de proyección de quien programa: lo que admite la sala y en lo que se estrena la película (R8, R31, R35).
// Swing no sabe de reglas: cruza dos listas de la API, y el backend sigue siendo quien rechaza.
public final class SelectorProyeccion {

    private final JComboBox<Opcion<Integer>> pelicula;
    private final JComboBox<Opcion<Integer>> sala;
    private final JComboBox<Opcion<String>> proyeccion;
    private final JLabel aviso = Componentes.nota("Esta sala no proyecta ninguna versión de esta película");
    private final Map<Integer, List<String>> formatos = new HashMap<>();
    private final Map<Integer, List<String>> admitidas = new HashMap<>();

    public SelectorProyeccion(JComboBox<Opcion<Integer>> pelicula, JComboBox<Opcion<Integer>> sala,
                              JComboBox<Opcion<String>> proyeccion) {
        this.pelicula = pelicula;
        this.sala = sala;
        this.proyeccion = proyeccion;
        aviso.setVisible(false);
        pelicula.addActionListener(e -> actualizar());
        sala.addActionListener(e -> actualizar());
    }

    public JLabel aviso() {
        return aviso;
    }

    // Antes de llenar los combos de película y sala: cada opción que entra ya pregunta qué ofrecer.
    public void conocer(List<Pelicula> peliculas, List<Sala> salas, List<TipoSala> tipos) {
        Map<String, List<String>> porTipo = new HashMap<>();
        tipos.forEach(t -> porTipo.put(t.nombre(), t.proyecciones()));
        peliculas.forEach(p -> formatos.put(p.id(), p.formatos()));
        salas.forEach(s -> admitidas.put(s.id(), porTipo.getOrDefault(s.tipo(), List.of())));
    }

    // Conserva la elegida si sigue valiendo, así cambiar de sala no pisa lo que ya se eligió.
    private void actualizar() {
        if (!(pelicula.getSelectedItem() instanceof Opcion<?> p) || !(sala.getSelectedItem() instanceof Opcion<?> s)) {
            return;
        }
        List<String> posibles = admitidas.getOrDefault(s.valor(), List.of()).stream()
                .filter(formatos.getOrDefault(p.valor(), List.of())::contains)
                .toList();
        Object elegida = proyeccion.getSelectedItem();
        Campos.llenar(proyeccion, Opciones.etiquetadas(posibles));
        if (elegida instanceof Opcion<?> anterior && anterior.valor() instanceof String valor) {
            Campos.elegir(proyeccion, valor);
        }
        aviso.setVisible(posibles.isEmpty());
    }
}
