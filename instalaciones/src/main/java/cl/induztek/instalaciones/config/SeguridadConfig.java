package cl.induztek.instalaciones.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cl.induztek.instalaciones.security.JwtFiltro;
import cl.induztek.instalaciones.security.JwtService;

@Configuration
public class SeguridadConfig {

    @Bean
    public FilterRegistrationBean<JwtFiltro> jwtFiltro(JwtService jwtService) {
        FilterRegistrationBean<JwtFiltro> registro = new FilterRegistrationBean<>(new JwtFiltro(jwtService));
        registro.addUrlPatterns("/clientes/*", "/tecnicos/*", "/instalaciones/*", "/equipos/*", "/visitas/*", "/tipos-equipo", "/asignaciones", "/asignaciones/*");
        return registro;
    }
}
