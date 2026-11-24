package cl.induztek.protocolos.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    @DisplayName("La documentación de la API muestra título, versión y seguridad bearer")
    void documentacionTieneTituloYVersion() {
        OpenAPI api = new OpenApiConfig().documentacion();
        assertThat(api.getInfo().getTitle()).isEqualTo("Induztek - Protocolos");
        assertThat(api.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(api.getComponents().getSecuritySchemes()).containsKey("bearer");
        assertThat(api.getSecurity()).hasSize(1);
    }
}
