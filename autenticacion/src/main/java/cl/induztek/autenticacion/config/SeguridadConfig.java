package cl.induztek.autenticacion.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import cl.induztek.autenticacion.security.JwtFiltro;
import cl.induztek.autenticacion.security.JwtService;

@Configuration
public class SeguridadConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<JwtFiltro> jwtFiltro(JwtService jwtService) {
        FilterRegistrationBean<JwtFiltro> registro = new FilterRegistrationBean<>(new JwtFiltro(jwtService));
        registro.addUrlPatterns("/usuarios/*");
        return registro;
    }
}
