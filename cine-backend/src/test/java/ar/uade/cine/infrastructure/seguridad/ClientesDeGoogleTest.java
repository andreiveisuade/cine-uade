package ar.uade.cine.infrastructure.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.usuarios.GestorEmpleados;

class ClientesDeGoogleTest extends PruebaDeIntegracion {

    @Autowired
    private ClientesDeGoogle google;

    @Autowired
    private GestorClientes clientes;

    @Autowired
    private GestorEmpleados empleados;

    @Test
    @DisplayName("R28: un email nuevo entra como cliente, con el nombre de Google y solo el rol CLIENTE")
    void unEmailNuevoEsUnClienteNuevo() {
        OidcUser usuario = google.loadUser(PedidosDeGoogle.de("ana@mail.com", "Ana Pérez", true));

        assertThat(usuario.getName()).isEqualTo("ana@mail.com");
        assertThat(usuario.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_CLIENTE");
        assertThat(clientes.buscarPorEmail("ana@mail.com")).get().extracting(Cliente::getNombre).isEqualTo("Ana Pérez");
    }

    // Las rutas de la cuenta buscan al cliente por el nombre de la sesión: tiene que ser el email como se guardó.
    @Test
    @DisplayName("R28: la sesión queda a nombre del email como lo guarda el cine")
    void laSesionUsaElEmailNormalizado() {
        clientes.ingresarConGoogle("Ana", "ana@mail.com");

        OidcUser usuario = google.loadUser(PedidosDeGoogle.de("Ana@Mail.com", "Ana Pérez", true));

        assertThat(usuario.getName()).isEqualTo("ana@mail.com");
        assertThat(usuario.getEmail()).isEqualTo("ana@mail.com");
        assertThat(clientes.listar()).hasSize(1);
    }

    @Test
    @DisplayName("R28: con el email sin verificar Google no prueba nada: no entra ni se da de alta")
    void elEmailSinVerificarNoEntra() {
        OAuth2AuthenticationException error = assertThrows(OAuth2AuthenticationException.class,
                () -> google.loadUser(PedidosDeGoogle.de("ana@mail.com", "Ana", false)));

        assertThat(error.getMessage())
                .isEqualTo("Tu cuenta de Google no tiene el email verificado: verificalo y volvé a intentar");
        assertThat(clientes.listar()).isEmpty();
    }

    @Test
    @DisplayName("R28: el email de un empleado no entra por Google: el panel es Swing, con su clave")
    void unEmpleadoNoEntraPorGoogle() {
        empleados.registrar("Encargado", "encargado@cine.com", "clave-del-encargado", Rol.ADMINISTRADOR);

        OAuth2AuthenticationException error = assertThrows(OAuth2AuthenticationException.class,
                () -> google.loadUser(PedidosDeGoogle.de("encargado@cine.com", "Encargado", true)));

        assertThat(error.getMessage())
                .isEqualTo("Ese email es del personal del cine: entrá desde la aplicación de escritorio");
        assertThat(clientes.listar()).isEmpty();
    }

    // El log de Render es público para quien vea el repo: el ingreso se rastrea por id, sin email ni nombre.
    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void elIngresoQuedaEnElLogSinElEmail(CapturedOutput log) {
        google.loadUser(PedidosDeGoogle.de("ana@mail.com", "Ana Pérez", true));
        google.loadUser(PedidosDeGoogle.de("ana@mail.com", "Ana Pérez", true));
        int id = clientes.buscarPorEmail("ana@mail.com").orElseThrow().getId();

        assertThat(log.getOut()).contains("cliente " + id + " entra con google · nuevo")
                .contains("cliente " + id + " entra con google · existente");
        assertThat(log.getAll()).doesNotContain("ana@mail.com").doesNotContain("Ana Pérez");
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void elIngresoRechazadoQuedaEnElLogSinElEmail(CapturedOutput log) {
        empleados.registrar("Encargado", "encargado@cine.com", "clave-del-encargado", Rol.ADMINISTRADOR);

        assertThrows(OAuth2AuthenticationException.class,
                () -> google.loadUser(PedidosDeGoogle.de("ana@mail.com", "Ana", false)));
        assertThrows(OAuth2AuthenticationException.class,
                () -> google.loadUser(PedidosDeGoogle.de("encargado@cine.com", "Encargado", true)));

        assertThat(log.getOut()).contains("ingreso con google rechazado · email sin verificar")
                .contains("ingreso con google rechazado · email del personal");
        assertThat(log.getAll()).doesNotContain("ana@mail.com").doesNotContain("encargado@cine.com");
    }
}
