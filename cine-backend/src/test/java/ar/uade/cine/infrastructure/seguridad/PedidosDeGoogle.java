package ar.uade.cine.infrastructure.seguridad;

import java.time.Instant;
import java.util.Set;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

// Lo que Google devuelve al volver del login, armado en el test: sin userinfo-uri, OidcUserService no sale a la red.
public final class PedidosDeGoogle {

    private static final ClientRegistration GOOGLE = ClientRegistration.withRegistrationId("google")
            .clientId("id-de-prueba")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .scope("openid", "email", "profile")
            .userNameAttributeName(IdTokenClaimNames.SUB)
            .build();

    private PedidosDeGoogle() {
    }

    public static OidcUserRequest de(String email, String nombre, boolean verificado) {
        Instant ahora = Instant.now();
        OidcIdToken token = OidcIdToken.withTokenValue("id-token")
                .subject("google-" + email)
                .claim("email", email)
                .claim("email_verified", verificado)
                .claim("name", nombre)
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(3600))
                .build();
        OAuth2AccessToken acceso = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token",
                ahora, ahora.plusSeconds(3600), Set.of("openid", "email", "profile"));
        return new OidcUserRequest(GOOGLE, acceso, token);
    }
}
