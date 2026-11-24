package cl.induztek.instalaciones.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    @DisplayName("La documentación Swagger expone el título, versión y descripción del microservicio")
    void documentacionTieneTituloVersionYDescripcion() {
        OpenAPI api = config.documentacion();
        assertThat(api.getInfo().getTitle()).isEqualTo("Induztek - Instalaciones");
        assertThat(api.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(api.getInfo().getDescription()).contains("Instalaciones");
    }

    @Test
    @DisplayName("La documentación declara el esquema de seguridad bearer con formato JWT")
    void documentacionIncluyeSeguridadBearer() {
        OpenAPI api = config.documentacion();
        SecurityScheme bearer = api.getComponents().getSecuritySchemes().get("bearer");
        assertThat(bearer).isNotNull();
        assertThat(bearer.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(bearer.getScheme()).isEqualTo("bearer");
        assertThat(bearer.getBearerFormat()).isEqualTo("JWT");
        assertThat(api.getSecurity()).isNotEmpty();
    }
}
