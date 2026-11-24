package cl.induztek.protocolos.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRETO = "induztek-secreto-de-pruebas-unitarias-2026";
    private final SecretKey llave = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));

    @Test
    @DisplayName("Token válido entrega id y rol del técnico")
    void tokenValidoRetornaUsuarioToken() {
        String token = Jwts.builder()
                .subject("42")
                .claim("rol", "TECNICO")
                .signWith(llave)
                .compact();
        JwtService service = new JwtService(SECRETO);
        UsuarioToken usuario = service.validarToken(token);
        assertThat(usuario).isNotNull();
        assertThat(usuario.idUsuario()).isEqualTo(42L);
        assertThat(usuario.rol()).isEqualTo("TECNICO");
    }

    @Test
    @DisplayName("Token basura se rechaza y no entrega usuario")
    void tokenInvalidoRetornaNull() {
        JwtService service = new JwtService(SECRETO);
        assertThat(service.validarToken("basura")).isNull();
    }

    @Test
    @DisplayName("Token alterado se rechaza y no entrega usuario")
    void tokenAlteradoRetornaNull() {
        String token = Jwts.builder().subject("1").claim("rol", "TECNICO").signWith(llave).compact();
        JwtService service = new JwtService(SECRETO);
        assertThat(service.validarToken(token + "x")).isNull();
    }

    @Test
    @DisplayName("Token con id no numérico se rechaza")
    void tokenConSujetoNoNumericoRetornaNull() {
        String token = Jwts.builder().subject("abc").claim("rol", "TECNICO").signWith(llave).compact();
        JwtService service = new JwtService(SECRETO);
        assertThat(service.validarToken(token)).isNull();
    }
}
