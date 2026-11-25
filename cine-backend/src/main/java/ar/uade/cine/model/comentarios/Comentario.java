package ar.uade.cine.model.comentarios;

import java.time.LocalDateTime;

import lombok.Getter;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import ar.uade.cine.model.comentarios.validacion.ValidadorComentario;
import ar.uade.cine.model.usuarios.Cliente;

// Reseña de un cliente sobre el cine, con puntaje de 1 a 5; Experto: nace válida y no cambia.
@Entity
@Table(name = "comentario")
@Getter
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    private int puntaje;

    private String texto;

    private LocalDateTime fecha;

    protected Comentario() {
    }

    public Comentario(Cliente cliente, Integer puntaje, String texto, LocalDateTime fecha) {
        int puntajeValido = ValidadorComentario.puntaje(puntaje);
        String textoValido = ValidadorComentario.texto(texto);
        this.cliente = cliente;
        this.puntaje = puntajeValido;
        this.texto = textoValido;
        this.fecha = fecha;
    }
}
