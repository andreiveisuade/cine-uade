package ar.uade.cine.persistencia.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import ar.uade.cine.persistencia.PersistenciaException;

/**
 * Único lugar donde vive el dato de conexión. Si cambia la contraseña, se toca acá
 * y en ningún otro archivo.
 */
public class ConexionMySQL {

    private static final String URL = "jdbc:mysql://localhost:3306/appsinteractivas";
    private static final String USUARIO = "root";
    private static final String PASSWORD = "root";

    public static Connection abrir() {
        try {
            return DriverManager.getConnection(URL, USUARIO, PASSWORD);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo conectar a MySQL", e);
        }
    }
}
