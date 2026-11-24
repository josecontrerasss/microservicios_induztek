package cl.induztek.autenticacion.config;

import org.springframework.mock.http.MockHttpInputMessage;
import cl.induztek.autenticacion.exception.AccesoDenegadoException;
import cl.induztek.autenticacion.exception.ConflictoException;
import cl.induztek.autenticacion.exception.CredencialesInvalidasException;
import cl.induztek.autenticacion.exception.RecursoNoEncontradoException;
import cl.induztek.autenticacion.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManejadorExcepcionesTest {

    private ManejadorExcepciones manejador;

    @BeforeEach
    void setUp() {
        manejador = new ManejadorExcepciones();
    }

    @Test
    @DisplayName("Un usuario inexistente responde 404 con el mensaje de negocio")
    void recursoNoEncontrado() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarRecursoNoEncontrado(
                new RecursoNoEncontradoException("Usuario no encontrado con ID: 9"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).containsEntry("estado", 404);
        assertThat(respuesta.getBody()).containsEntry("mensaje", "Usuario no encontrado con ID: 9");
        assertThat(respuesta.getBody()).doesNotContainKey("errores");
    }

    @Test
    @DisplayName("Registrar con un RUT ya usado responde 409 conflicto")
    void conflicto() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarConflicto(
                new ConflictoException("El RUT o el correo ya están registrados"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).containsEntry("estado", 409);
    }

    @Test
    @DisplayName("Iniciar sesión con credenciales incorrectas responde 401")
    void credencialesInvalidas() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarCredencialesInvalidas(
                new CredencialesInvalidasException("Correo o contraseña incorrectos"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody()).containsEntry("estado", 401);
        assertThat(respuesta.getBody()).containsEntry("mensaje", "Correo o contraseña incorrectos");
    }

    @Test
    @DisplayName("Un cliente intentando ver el perfil de otro recibe 403")
    void accesoDenegado() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarAccesoDenegado(
                new AccesoDenegadoException("Solo puedes acceder a tu propio perfil"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).containsEntry("estado", 403);
    }

    @Test
    @DisplayName("Un RUT inválido al registrarse responde 400")
    void solicitudInvalida() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarSolicitudInvalida(
                new SolicitudInvalidaException("RUT inválido"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("estado", 400);
    }

    @Test
    @DisplayName("Datos inválidos en el body devuelven los errores de validación por campo")
    void manejarValidacion() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("usuarioRegistroDTO", "correo", "El correo debe ser válido"),
                new FieldError("usuarioRegistroDTO", "password", "La contraseña es obligatoria")));
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarValidacion(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("mensaje", "Datos de entrada inválidos");
        Map<String, String> errores = (Map<String, String>) respuesta.getBody().get("errores");
        assertThat(errores).containsEntry("correo", "El correo debe ser válido");
        assertThat(errores).containsEntry("password", "La contraseña es obligatoria");
    }

    @Test
    @DisplayName("Un cuerpo ilegible responde 400 con mensaje claro")
    void cuerpoIlegible() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("cuerpo malo", new MockHttpInputMessage(new byte[0]));
        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarCuerpoIlegible(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("mensaje", "El cuerpo de la solicitud no es válido");
        assertThat(respuesta.getBody()).doesNotContainKey("errores");
    }

    @Test
    @DisplayName("Un fallo al enviar el correo de recuperación responde 503")
    void fallaCorreo() {
        MailException ex = mock(MailException.class);
        when(ex.getMessage()).thenReturn("smtp caído");

        ResponseEntity<Map<String, Object>> respuesta = manejador.manejarCorreo(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(respuesta.getBody()).containsEntry("estado", 503);
        assertThat(respuesta.getBody()).containsEntry("mensaje", "No se pudo enviar el correo, intenta más tarde");
    }
}
