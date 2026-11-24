package cl.induztek.protocolos.config;

import org.springframework.mock.http.MockHttpInputMessage;
import cl.induztek.protocolos.exception.AccesoDenegadoException;
import cl.induztek.protocolos.exception.ConflictoException;
import cl.induztek.protocolos.exception.RecursoNoEncontradoException;
import cl.induztek.protocolos.exception.ServicioNoDisponibleException;
import cl.induztek.protocolos.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ManejadorExcepcionesTest {

    private final ManejadorExcepciones manejador = new ManejadorExcepciones();

    @Test
    @DisplayName("Protocolo no encontrado responde 404 al usuario")
    void recursoNoEncontradoRetorna404() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarRecursoNoEncontrado(new RecursoNoEncontradoException("no existe"));
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).containsEntry("estado", 404).containsEntry("mensaje", "no existe");
    }

    @Test
    @DisplayName("Protocolo duplicado o ya revisado responde 409")
    void conflictoRetorna409() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarConflicto(new ConflictoException("conflicto"));
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).containsEntry("estado", 409).containsEntry("mensaje", "conflicto");
    }

    @Test
    @DisplayName("Acción sin permiso responde 403 al usuario")
    void accesoDenegadoRetorna403() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarAccesoDenegado(new AccesoDenegadoException("prohibido"));
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).containsEntry("estado", 403).containsEntry("mensaje", "prohibido");
    }

    @Test
    @DisplayName("Solicitud inválida del técnico responde 400")
    void solicitudInvalidaRetorna400() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarSolicitudInvalida(new SolicitudInvalidaException("invalido"));
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("estado", 400).containsEntry("mensaje", "invalido");
    }

    @Test
    @DisplayName("Servicio de instalaciones caído responde 503")
    void servicioNoDisponibleRetorna503() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarServicioNoDisponible(new ServicioNoDisponibleException("caido"));
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(respuesta.getBody()).containsEntry("estado", 503).containsEntry("mensaje", "caido");
    }

    @Test
    @DisplayName("Datos inválidos del protocolo responden 400 con errores por campo")
    void validacionRetorna400ConErrores() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "objeto");
        bindingResult.addError(new FieldError("objeto", "campo", "mensaje de error"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarValidacion(ex);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("estado", 400).containsEntry("mensaje", "Datos de entrada inválidos");
        @SuppressWarnings("unchecked")
        Map<String, String> errores = (Map<String, String>) respuesta.getBody().get("errores");
        assertThat(errores).containsEntry("campo", "mensaje de error");
    }

    @Test
    @DisplayName("Cuerpo JSON mal formado responde 400")
    void cuerpoIlegibleRetorna400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("cuerpo malo", new MockHttpInputMessage(new byte[0]));
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarCuerpoIlegible(ex);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("estado", 400).containsEntry("mensaje", "El cuerpo de la solicitud no es válido");
    }
}
