package ar.uade.cine.model.usuarios;

import lombok.Getter;
import org.hibernate.annotations.DiscriminatorFormula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Cliente o empleado de la tabla usuario; Experto: valida el nombre, y el email lo normaliza y valida Email.
// Discriminador por fórmula: tres roles caen en dos clases (ADMINISTRADOR y ACOMODADOR son Empleado).
@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("case when rol = 'CLIENTE' then 'CLIENTE' else 'EMPLEADO' end")
@Getter
public abstract class Usuario {

    private static final int LARGO_MAXIMO = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nombre;

    // El texto que ya normalizó Email: es con lo que se busca, también desde las reservas del cliente.
    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Rol rol;

    protected Usuario() {
    }

    protected Usuario(String nombre, String email, Rol rol) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatoInvalido("El nombre no puede estar vacío");
        }
        this.email = new Email(email).valor();
        this.nombre = recortado(nombre, "nombre");
        this.rol = rol;
    }

    // Se guarda y se mide sin los espacios de más.
    private static String recortado(String texto, String campo) {
        String limpio = texto.trim();
        // Los VARCHAR(100) de la tabla: pasado, MySQL rechaza el INSERT con un 500.
        if (limpio.length() > LARGO_MAXIMO) {
            throw new DatoInvalido(
                    "El " + campo + " no puede tener más de " + LARGO_MAXIMO + " caracteres");
        }
        return limpio;
    }
}
