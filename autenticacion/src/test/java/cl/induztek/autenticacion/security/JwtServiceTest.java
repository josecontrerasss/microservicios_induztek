package cl.induztek.autenticacion.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("induztek-secreto-de-pruebas-unitarias-2026", 8);
    }

    @Test
    @DisplayName("Genera un token válido con el id, correo y rol del técnico autenticado")
    void generaYValidaToken() {
        String token = jwtService.generarToken(7L, "tecnico@induztek.cl", "TECNICO");

        assertThat(token).isNotBlank();
        UsuarioToken usuario = jwtService.validarToken(token);
        assertThat(usuario).isNotNull();
        assertThat(usuario.idUsuario()).isEqualTo(7L);
        assertThat(usuario.rol()).isEqualTo("TECNICO");
    }

    @Test
    @DisplayName("Un token alterado retorna null y no permite acceder a los recursos")
    void tokenAlteradoRetornaNull() {
        String token = jwtService.generarToken(7L, "tecnico@induztek.cl", "TECNICO");
        String alterado = token.substring(0, token.length() - 2) + "XX";

        assertThat(jwtService.validarToken(alterado)).isNull();
    }

    @Test
    @DisplayName("Un token basura retorna null")
    void tokenBasuraRetornaNull() {
        assertThat(jwtService.validarToken("no-es-un-token-jwt")).isNull();
    }

    @Test
    @DisplayName("Un token null o vacío retorna null sin lanzar excepción")
    void tokenNullOVacioRetornaNull() {
        assertThat(jwtService.validarToken(null)).isNull();
        assertThat(jwtService.validarToken("")).isNull();
    }
}
