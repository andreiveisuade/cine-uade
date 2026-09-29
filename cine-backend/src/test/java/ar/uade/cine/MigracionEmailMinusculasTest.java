package ar.uade.cine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import ar.uade.cine.service.usuarios.GestorClientes;

// La migración corre sobre el H2 de los tests, en modo MySQL, como se aplicaría a mano en una base ya creada.
class MigracionEmailMinusculasTest extends PruebaDeIntegracion {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource base;

    @Autowired
    private GestorClientes clientes;

    // Un email guardado antes de normalizarlos no lo encontraba nadie: se busca ya en minúsculas.
    @Test
    void dejaLosEmailsViejosComoLosGuardaUsuarioYAsiSeEncuentran() throws SQLException {
        jdbc.update("INSERT INTO usuario (nombre, email, rol) VALUES (?, ?, ?)", "Ana", " Ana@Mail.COM ", "CLIENTE");
        assertFalse(clientes.buscarPorEmail("ana@mail.com").isPresent());

        try (Connection conexion = base.getConnection()) {
            ScriptUtils.executeSqlScript(conexion, new ClassPathResource("migracion-email-minusculas.sql"));
        }

        assertEquals("ana@mail.com", jdbc.queryForObject("SELECT email FROM usuario WHERE nombre = 'Ana'", String.class));
        assertTrue(clientes.buscarPorEmail("ANA@mail.com").isPresent());
    }
}
