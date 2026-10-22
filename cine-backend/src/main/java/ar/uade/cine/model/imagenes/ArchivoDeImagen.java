package ar.uade.cine.model.imagenes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

// Los bytes de una imagen subida, guardados con su nombre; la valida Imagen antes de llegar acá.
@Entity
@Table(name = "imagen")
@Getter
public class ArchivoDeImagen {

    @Id
    private String nombre;

    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] contenido;

    protected ArchivoDeImagen() {
    }

    public ArchivoDeImagen(NombreDeImagen nombre, byte[] contenido) {
        this.nombre = nombre.valor();
        this.contenido = contenido;
    }
}
