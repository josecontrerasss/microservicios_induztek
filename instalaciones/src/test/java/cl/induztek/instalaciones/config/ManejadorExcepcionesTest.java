package cl.induztek.instalaciones.config;

import org.springframework.mock.http.MockHttpInputMessage;
import cl.induztek.instalaciones.exception.AccesoDenegadoException;
import cl.induztek.instalaciones.exception.ConflictoException;
import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManejadorExcepcionesTest {

    private final ManejadorExcepciones manejador = new ManejadorExcepciones();

    @Test
    @DisplayName("Recurso inexistente responde 404 con el mensaje del error")
    void recursoNoEncontradoRetorna404() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarRecursoNoEncontrado(new RecursoNoEncontradoException("Instalación no encontrada"));
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).containsEntry("estado", 404).containsEntry("mensaje", "Instalación no encontrada");
        assertThat(respuesta.getBody()).doesNotContainKey("errores");
    }

    @Test
    @DisplayName("Asignación duplicada responde conflicto 409")
    void conflictoRetorna409() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarConflicto(new ConflictoException("Técnico ya asignado"));
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).containsEntry("mensaje", "Técnico ya asignado");
        assertThat(respuesta.getBody()).doesNotContainKey("errores");
    }

    @Test
    @DisplayName("Técnico no asignado responde 403 prohibido")
    void accesoDenegadoRetorna403() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarAccesoDenegado(new AccesoDenegadoException("Sin permisos"));
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).containsEntry("mensaje", "Sin permisos");
    }

    @Test
    @DisplayName("Solicitud inválida responde 400 con el mensaje del error")
    void solicitudInvalidaRetorna400() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarSolicitudInvalida(new SolicitudInvalidaException("Tipo inexistente"));
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("mensaje", "Tipo inexistente");
    }

    @Test
    @DisplayName("Validación de campos responde 400 con detalle por campo")
    void validacionRetorna400ConErrores() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("dto", "codigo", "El código es obligatorio"),
                new FieldError("dto", "nombre", "El nombre es obligatorio")));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarValidacion(ex);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("estado", 400).containsEntry("mensaje", "Datos de entrada inválidos");
        assertThat(respuesta.getBody().get("errores")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, String> errores = (Map<String, String>) respuesta.getBody().get("errores");
        assertThat(errores).containsEntry("codigo", "El código es obligatorio").containsEntry("nombre", "El nombre es obligatorio");
    }

    @Test
    @DisplayName("Cuerpo JSON ilegible responde 400 indicando cuerpo inválido")
    void cuerpoIlegibleRetorna400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("cuerpo malo", new MockHttpInputMessage(new byte[0]));
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarCuerpoIlegible(ex);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("mensaje", "El cuerpo de la solicitud no es válido");
    }
}
