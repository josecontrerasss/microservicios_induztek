package cl.induztek.instalaciones.config;

import cl.induztek.instalaciones.security.JwtFiltro;
import cl.induztek.instalaciones.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class SeguridadConfigTest {

    private final JwtService jwtService = new JwtService("induztek-secreto-de-pruebas-unitarias-2026");

    @Test
    @DisplayName("El filtro JWT se registra en las rutas protegidas del microservicio")
    void jwtFiltroRegistraRutasProtegidas() {
        FilterRegistrationBean<JwtFiltro> registro = new SeguridadConfig().jwtFiltro(jwtService);
        assertThat(registro.getFilter()).isInstanceOf(JwtFiltro.class);
        assertThat(registro.getUrlPatterns()).contains(
                "/clientes/*", "/tecnicos/*", "/instalaciones/*", "/equipos/*", "/visitas/*",
                "/tipos-equipo", "/asignaciones", "/asignaciones/*");
    }
}
