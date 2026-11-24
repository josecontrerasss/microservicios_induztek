package cl.induztek.protocolos.config;

import cl.induztek.protocolos.security.JwtFiltro;
import cl.induztek.protocolos.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SeguridadConfigTest {

    @Test
    @DisplayName("El filtro JWT protege las rutas de clientes, técnicos y protocolos")
    void jwtFiltroRegistraUrlPatterns() {
        JwtService jwtService = mock(JwtService.class);
        FilterRegistrationBean<JwtFiltro> registro = new SeguridadConfig().jwtFiltro(jwtService);
        assertThat(registro.getFilter()).isInstanceOf(JwtFiltro.class);
        assertThat(registro.getUrlPatterns()).containsExactlyInAnyOrder("/clientes/*", "/tecnicos/*", "/protocolos/*", "/protocolos");
    }
}
