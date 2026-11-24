package cl.induztek.autenticacion.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class JwtFiltroTest {

    private JwtService jwtService;
    private JwtFiltro filtro;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("induztek-secreto-de-pruebas-unitarias-2026", 8);
        filtro = new JwtFiltro(jwtService);
    }

    @Test
    @DisplayName("Sin cabecera Authorization responde 401 y no continúa la cadena")
    void sinCabeceraResponde401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filtro.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Token ausente");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("Una cabecera que no empieza con Bearer responde 401 y no continúa la cadena")
    void cabeceraSinBearerResponde401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abcdef");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filtro.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("Un token inválido o basura responde 401 y no continúa la cadena")
    void tokenInvalidoResponde401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer basura-que-no-es-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filtro.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("Un token válido de técnico deja el atributo y continúa la cadena")
    void tokenValidoContinuaCadena() throws Exception {
        String token = jwtService.generarToken(5L, "tecnico@induztek.cl", "TECNICO");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filtro.doFilter(request, response, chain);

        UsuarioToken usuario = (UsuarioToken) request.getAttribute(JwtFiltro.ATRIBUTO_USUARIO);
        assertThat(usuario).isNotNull();
        assertThat(usuario.idUsuario()).isEqualTo(5L);
        assertThat(usuario.rol()).isEqualTo("TECNICO");
        assertThat(chain.getRequest()).isNotNull();
    }
}
