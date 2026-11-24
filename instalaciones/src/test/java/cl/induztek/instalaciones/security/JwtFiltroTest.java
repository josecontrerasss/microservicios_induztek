package cl.induztek.instalaciones.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtFiltroTest {

    private static final String SECRETO = "induztek-secreto-de-pruebas-unitarias-2026";
    private final JwtService jwtService = new JwtService(SECRETO);
    private final JwtFiltro filtro = new JwtFiltro(jwtService);

    @Test
    @DisplayName("Sin cabecera Authorization responde 401 y no continúa la cadena")
    void sinCabeceraResponde401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filtro.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Token ausente");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("Cabecera con esquema distinto a Bearer responde 401 y no continúa")
    void cabeceraSinBearerResponde401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filtro.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("Token inválido responde 401 y no continúa la cadena")
    void tokenInvalidoResponde401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filtro.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("Token válido deja el usuario en el request y continúa la cadena")
    void tokenValidoContinuaCadena() throws ServletException, IOException {
        String token = Jwts.builder()
                .subject("7")
                .claim("rol", "TECNICO")
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .compact();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filtro.doFilter(request, response, chain);
        UsuarioToken usuario = (UsuarioToken) request.getAttribute(JwtFiltro.ATRIBUTO_USUARIO);
        assertThat(usuario).isNotNull();
        assertThat(usuario.idUsuario()).isEqualTo(7L);
        assertThat(usuario.rol()).isEqualTo("TECNICO");
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
