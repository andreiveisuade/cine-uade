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

// Cliente o empleado de la tabla usuario; nombre lo valida ValidadorUsuario, email lo valida Email.
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

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Rol rol;

    // Acá y no en Empleado: el cliente con cuenta también entra con clave. Null si compró sin registrarse.
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    protected Usuario() {
    }

    protected Usuario(String nombre, String email, Rol rol) {
        this.nombre = ValidadorUsuario.nombre(nombre);
        this.email = new Email(email).valor();
        this.rol = rol;
    }

    // R23: el nombre cambia con las reglas del alta; el email no, es con lo que se entra y se busca.
    public void renombrar(String nombre) {
        this.nombre = ValidadorUsuario.nombre(nombre);
    }

    protected void guardarPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
