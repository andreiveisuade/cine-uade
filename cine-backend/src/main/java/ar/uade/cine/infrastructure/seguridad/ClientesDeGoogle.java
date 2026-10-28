package ar.uade.cine.infrastructure.seguridad;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.service.usuarios.GestorClientes;

// Convierte al usuario que devuelve Google en el cliente del cine (R28); Adapter sobre el OidcUserService de Spring.
// La sesión queda a nombre del email guardado y con solo ROLE_CLIENTE: el panel sigue siendo de Basic.
@Component
@RequiredArgsConstructor
public class ClientesDeGoogle implements OAuth2UserService<OidcUserRequest, OidcUser> {

    // Con este código la web muestra el texto tal cual; otro error de OAuth2 (cancelar en Google) sale genérico.
    static final String RECHAZO = "ingreso_rechazado";

    private final OidcUserService google = new OidcUserService();
    private final GestorClientes clientes;

    @Override
    public OidcUser loadUser(OidcUserRequest pedido) {
        OidcUser usuario = google.loadUser(pedido);
        // Sin verificar, el email es solo lo que alguien escribió: ser ese cliente le daría sus reservas.
        if (!Boolean.TRUE.equals(usuario.getEmailVerified())) {
            throw rechazo("Tu cuenta de Google no tiene el email verificado: verificalo y volvé a intentar");
        }
        Cliente cliente;
        try {
            cliente = clientes.ingresarConGoogle(usuario.getFullName(), usuario.getEmail());
        } catch (Rechazo e) {
            throw rechazo(e.getMessage());
        }
        OidcIdToken token = usuario.getIdToken();
        OidcIdToken comoLoGuarda = OidcIdToken.withTokenValue(token.getTokenValue())
                .claims(claims -> claims.putAll(token.getClaims()))
                .claim(StandardClaimNames.EMAIL, cliente.getEmail())
                .build();
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_" + Rol.CLIENTE.name())),
                comoLoGuarda, StandardClaimNames.EMAIL);
    }

    private static OAuth2AuthenticationException rechazo(String mensaje) {
        return new OAuth2AuthenticationException(new OAuth2Error(RECHAZO, mensaje, null), mensaje);
    }
}
