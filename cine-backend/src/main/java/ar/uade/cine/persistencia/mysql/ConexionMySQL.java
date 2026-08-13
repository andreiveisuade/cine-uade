package ar.uade.cine.persistencia.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import ar.uade.cine.persistencia.PersistenciaException;

/**
 * Único lugar donde vive el dato de conexión. Sale del entorno porque el host cambia
 * según dónde corra la app: fuera de Docker la base está en localhost, y adentro el
 * host es el nombre del servicio del compose. Los defaults son los de la máquina de
 * desarrollo, así que sin variables de entorno funciona como siempre.
 */
public class ConexionMySQL {

    private static final String URL = "jdbc:mysql://" + variable("DB_HOST", "localhost")
            + ":" + variable("DB_PORT", "3306") + "/" + variable("DB_NAME", "appsinteractivas");
    private static final String USUARIO = variable("DB_USER", "root");
    private static final String PASSWORD = variable("DB_PASSWORD", "root");

    private static String variable(String nombre, String pordefecto) {
        String valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? pordefecto : valor;
    }

    public static Connection abrir() {
        try {
            return DriverManager.getConnection(URL, USUARIO, PASSWORD);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo conectar a MySQL", e);
        }
    }
}
