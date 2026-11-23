package cl.induztek.protocolos.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtFiltro extends OncePerRequestFilter {
    public static final String ATRIBUTO_USUARIO = "usuarioToken";

    private final JwtService jwtService;

    public JwtFiltro(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        UsuarioToken usuario = null;
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            usuario = jwtService.validarToken(cabecera.substring(7));
        }
        if (usuario == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"estado\":401,\"mensaje\":\"Token ausente, inválido o expirado\"}");
            return;
        }
        request.setAttribute(ATRIBUTO_USUARIO, usuario);
        chain.doFilter(request, response);
    }
}
