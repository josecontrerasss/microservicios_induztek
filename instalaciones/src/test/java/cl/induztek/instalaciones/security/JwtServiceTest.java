package cl.induztek.instalaciones.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRETO = "induztek-secreto-de-pruebas-unitarias-2026";
    private final JwtService jwtService = new JwtService(SECRETO);

    @Test
    @DisplayName("Token válido retorna el usuario y rol del JWT del técnico")
    void tokenValidoRetornaUsuario() {
        String token = Jwts.builder()
                .subject("5")
                .claim("rol", "TECNICO")
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .compact();
        UsuarioToken usuario = jwtService.validarToken(token);
        assertThat(usuario).isNotNull();
        assertThat(usuario.idUsuario()).isEqualTo(5L);
        assertThat(usuario.rol()).isEqualTo("TECNICO");
    }

    @Test
    @DisplayName("Token basura retorna null")
    void tokenBasuraRetornaNull() {
        assertThat(jwtService.validarToken("esto.no.es.un.jwt")).isNull();
    }

    @Test
    @DisplayName("Token firmado con otro secreto retorna null")
    void tokenOtroSecretoRetornaNull() {
        String token = Jwts.builder()
                .subject("1")
                .claim("rol", "CLIENTE")
                .signWith(Keys.hmacShaKeyFor("otro-secreto-diferente-de-pruebas-2026-abcdefgh".getBytes(StandardCharsets.UTF_8)))
                .compact();
        assertThat(jwtService.validarToken(token)).isNull();
    }

    @Test
    @DisplayName("Token alterado retorna null por firma inválida")
    void tokenAlteradoRetornaNull() {
        String token = Jwts.builder()
                .subject("1")
                .claim("rol", "SUPERVISOR")
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .compact();
        String alterado = token.substring(0, token.length() - 3) + "abc";
        assertThat(jwtService.validarToken(alterado)).isNull();
    }

    @Test
    @DisplayName("Token con subject no numérico retorna null")
    void tokenConSubjectNoNumericoRetornaNull() {
        String token = Jwts.builder()
                .subject("no-numerico")
                .claim("rol", "TECNICO")
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .compact();
        assertThat(jwtService.validarToken(token)).isNull();
    }

    @Test
    @DisplayName("Token vacío retorna null")
    void tokenVacioRetornaNull() {
        assertThat(jwtService.validarToken("")).isNull();
    }
}
