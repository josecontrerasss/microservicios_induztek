package cl.induztek.autenticacion.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    @DisplayName("La documentación expone el título y esquema bearer de Induztek Autenticación")
    void documentacionExponeTituloYEsquema() {
        OpenAPI openAPI = new OpenApiConfig().documentacion();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Induztek - Autenticación");
        assertThat(openAPI.getInfo().getDescription()).contains("Registro", "inicio de sesión");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearer");

        SecurityScheme esquema = openAPI.getComponents().getSecuritySchemes().get("bearer");
        assertThat(esquema.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(esquema.getScheme()).isEqualTo("bearer");
        assertThat(esquema.getBearerFormat()).isEqualTo("JWT");
        assertThat(openAPI.getSecurity()).isNotEmpty();
    }
}
