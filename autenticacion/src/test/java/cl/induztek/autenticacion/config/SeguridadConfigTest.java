package cl.induztek.autenticacion.config;

import cl.induztek.autenticacion.security.JwtFiltro;
import cl.induztek.autenticacion.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SeguridadConfigTest {

    private final SeguridadConfig config = new SeguridadConfig();

    @Test
    @DisplayName("El encoder BCrypt valida y encripta las contraseñas de los técnicos")
    void passwordEncoderBcrypt() {
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        String hash = encoder.encode("Clave123!");
        assertThat(encoder.matches("Clave123!", hash)).isTrue();
        assertThat(encoder.matches("otra", hash)).isFalse();
    }

    @Test
    @DisplayName("El filtro JWT se registra para la ruta /usuarios/*")
    void filtroJwtSeRegistraEnUsuarios() {
        JwtService jwtService = new JwtService("induztek-secreto-de-pruebas-unitarias-2026", 8);

        FilterRegistrationBean<JwtFiltro> registro = config.jwtFiltro(jwtService);

        assertThat(registro.getFilter()).isInstanceOf(JwtFiltro.class);
        assertThat(registro.getUrlPatterns()).contains("/usuarios/*");
    }
}
