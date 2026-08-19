package ar.uade.cine.model.usuarios;

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

/**
 * Base de las personas que el sistema conoce. Los subtipos no se diferencian solo por una
 * etiqueta: el administrador tiene credenciales y el cliente no, porque el cliente compra
 * sin registrarse con contraseña.
 *
 * <p>Las dos van a la misma tabla —{@code SINGLE_TABLE}, que es lo que ya hacía el schema—
 * y el discriminador es la columna {@code rol}. Tiene una vuelta: {@code rol} lleva tres
 * valores y las clases son dos, porque ADMINISTRADOR y ACOMODADOR son los dos empleados. Un
 * {@code @DiscriminatorValue} solo sabe comparar contra un valor, así que el discriminador
 * se calcula con una fórmula que agrupa a los dos. La alternativa era sumarle a la tabla una
 * columna que dijera lo mismo que rol pero más gruesa, y eso es un dato duplicado que un día
 * no coincide.
 */
@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("case when rol = 'CLIENTE' then 'CLIENTE' else 'EMPLEADO' end")
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nombre;

    @Column(unique = true)
    private String email;

    /**
     * Se mapea como una columna común y no como el discriminador de JPA porque es las dos
     * cosas a la vez: distingue la clase y además es un dato que el negocio lee —el front
     * muestra si el que entró es administrador o acomodador.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Rol rol;

    protected Usuario() {
    }

    protected Usuario(String nombre, String email, Rol rol) {
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    /** Qué puede hacer, y a la vez de qué clase es la fila. */
    public Rol getRol() {
        return rol;
    }
}
