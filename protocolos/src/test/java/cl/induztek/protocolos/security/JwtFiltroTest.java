package cl.induztek.protocolos.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtFiltroTest {

    private JwtService jwtService;
    private JwtFiltro filtro;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        filtro = new JwtFiltro(jwtService);
    }

    @Test
    @DisplayName("Petición sin token no accede y recibe 401")
    void sinCabeceraRetorna401YNoContinua() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filtro.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Token ausente");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("Cabecera de autorización mal formada recibe 401")
    void cabeceraSinBearerRetorna401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Token abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filtro.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Token ausente");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("Token inválido no accede y recibe 401")
    void tokenInvalidoRetorna401() throws ServletException, IOException {
        when(jwtService.validarToken("abc")).thenReturn(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filtro.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("Token válido identifica al técnico y deja continuar")
    void tokenValidoColocaAtributoYContinua() throws ServletException, IOException {
        UsuarioToken usuario = new UsuarioToken(1L, "TECNICO");
        when(jwtService.validarToken("abc")).thenReturn(usuario);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filtro.doFilter(request, response, chain);
        assertThat(request.getAttribute(JwtFiltro.ATRIBUTO_USUARIO)).isEqualTo(usuario);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
