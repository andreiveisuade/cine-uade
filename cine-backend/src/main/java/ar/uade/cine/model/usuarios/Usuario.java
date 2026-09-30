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

import ar.uade.cine.model.usuarios.validacion.ValidadorUsuario;

// Cliente o empleado de la tabla usuario; el nombre lo valida ValidadorUsuario, y el email, Email.
// Discriminador por fórmula: tres roles caen en dos clases (ADMINISTRADOR y ACOMODADOR son Empleado).
@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("case when rol = 'CLIENTE' then 'CLIENTE' else 'EMPLEADO' end")
@Getter
public abstract class Usuario {

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
        this.nombre = ValidadorUsuario.nombre(nombre);
        this.email = new Email(email).valor();
        this.rol = rol;
    }
}
