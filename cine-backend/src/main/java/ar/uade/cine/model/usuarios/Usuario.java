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

// Cliente o empleado de la tabla usuario; Experto: valida nombre y email y los guarda sin espacios de más.
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

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Rol rol;

    protected Usuario() {
    }

    protected Usuario(String nombre, String email, Rol rol) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("El email no es válido");
        }
        this.nombre = recortado(nombre, "nombre");
        this.email = recortado(email, "email");
        this.rol = rol;
    }

    // Se guarda y se mide sin los espacios de más: con ellos, " ana@mail.com" sería otro email y
    // el chequeo del repetido no lo vería.
    private static String recortado(String texto, String campo) {
        String limpio = texto.trim();
        // Los VARCHAR(100) de la tabla: pasado, MySQL rechaza el INSERT con un 500.
        if (limpio.length() > LARGO_MAXIMO) {
            throw new IllegalArgumentException(
                    "El " + campo + " no puede tener más de " + LARGO_MAXIMO + " caracteres");
        }
        return limpio;
    }
}
