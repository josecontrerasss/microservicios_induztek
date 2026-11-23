package cl.induztek.protocolos.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cl.induztek.protocolos.security.JwtFiltro;
import cl.induztek.protocolos.security.JwtService;

@Configuration
public class SeguridadConfig {

    @Bean
    public FilterRegistrationBean<JwtFiltro> jwtFiltro(JwtService jwtService) {
        FilterRegistrationBean<JwtFiltro> registro = new FilterRegistrationBean<>(new JwtFiltro(jwtService));
        registro.addUrlPatterns("/clientes/*", "/tecnicos/*", "/protocolos/*", "/protocolos");
        return registro;
    }
}
