package ar.uade.cine.model.usuarios;

import java.util.Locale;
import java.util.regex.Pattern;

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

// Cliente o empleado de la tabla usuario; Experto: valida nombre y email, y guarda el email en minúsculas.
// Discriminador por fórmula: tres roles caen en dos clases (ADMINISTRADOR y ACOMODADOR son Empleado).
@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("case when rol = 'CLIENTE' then 'CLIENTE' else 'EMPLEADO' end")
@Getter
public abstract class Usuario {

    private static final int LARGO_MAXIMO = 100;

    // usuario@dominio.algo, la misma regla que Swing. Es la única del backend: POST /api/reservas da
    // de alta al cliente sin pasar por el DTO, y el @Email de Bean Validation aceptaba «a@b».
    private static final Pattern FORMA_DEL_EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

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
            throw new DatoInvalido("El nombre no puede estar vacío");
        }
        String emailNormalizado = normalizarEmail(email);
        if (emailNormalizado.isEmpty()) {
            throw new DatoInvalido("Falta el email");
        }
        if (!FORMA_DEL_EMAIL.matcher(emailNormalizado).matches()) {
            throw new DatoInvalido("El email tiene que tener la forma usuario@dominio.com");
        }
        this.nombre = recortado(nombre, "nombre");
        this.email = recortado(emailNormalizado, "email");
        this.rol = rol;
    }

    // Como se guarda, y por eso como se busca (clientes, reservas por email, login): API.md promete
    // emails sin distinguir mayúsculas, y comparando el texto exacto «BETO@x.com» era otro cliente.
    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    // Se guarda y se mide sin los espacios de más: con ellos, " ana@mail.com" sería otro email y
    // el chequeo del repetido no lo vería.
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
