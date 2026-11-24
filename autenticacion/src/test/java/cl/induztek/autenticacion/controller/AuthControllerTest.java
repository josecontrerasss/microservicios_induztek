package cl.induztek.autenticacion.controller;

import cl.induztek.autenticacion.config.ManejadorExcepciones;
import cl.induztek.autenticacion.exception.ConflictoException;
import cl.induztek.autenticacion.exception.CredencialesInvalidasException;
import cl.induztek.autenticacion.exception.SolicitudInvalidaException;
import cl.induztek.autenticacion.model.AuthRespuestaDTO;
import cl.induztek.autenticacion.model.LoginDTO;
import cl.induztek.autenticacion.model.RecuperarClaveDTO;
import cl.induztek.autenticacion.model.RestablecerClaveDTO;
import cl.induztek.autenticacion.model.UsuarioMostrarDTO;
import cl.induztek.autenticacion.model.UsuarioRegistroDTO;
import cl.induztek.autenticacion.service.RecuperacionClaveService;
import cl.induztek.autenticacion.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private RecuperacionClaveService recuperacionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(usuarioService, recuperacionService))
                .setControllerAdvice(new ManejadorExcepciones())
                .build();
    }

    @Test
    @DisplayName("Registrar un cliente válido responde 201 con los datos del usuario creado")
    void registrarClienteResponde201() throws Exception {
        UsuarioMostrarDTO respuesta = new UsuarioMostrarDTO();
        respuesta.setIdUsuario(1L);
        respuesta.setRut("12.345.678-5");
        respuesta.setNombre("Juan Pérez");
        respuesta.setCorreo("juan@induztek.cl");
        respuesta.setRol("CLIENTE");
        respuesta.setIdRol(4);
        respuesta.setActivo(true);

        when(usuarioService.registrarCliente(any(UsuarioRegistroDTO.class))).thenReturn(respuesta);

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rut": "12345678-5",
                                  "nombre": "Juan Pérez",
                                  "correo": "juan@induztek.cl",
                                  "password": "Clave123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.correo").value("juan@induztek.cl"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        verify(usuarioService).registrarCliente(any(UsuarioRegistroDTO.class));
    }

    @Test
    @DisplayName("Registrar con un RUT ya usado responde conflicto 409")
    void registrarClienteDuplicadoResponde409() throws Exception {
        when(usuarioService.registrarCliente(any(UsuarioRegistroDTO.class)))
                .thenThrow(new ConflictoException("El RUT o el correo ya están registrados"));

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rut": "12345678-5",
                                  "nombre": "Juan Pérez",
                                  "correo": "juan@induztek.cl",
                                  "password": "Clave123!"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409));
    }

    @Test
    @DisplayName("Registrar con un RUT inválido responde 400")
    void registrarConRutInvalidoResponde400() throws Exception {
        when(usuarioService.registrarCliente(any(UsuarioRegistroDTO.class)))
                .thenThrow(new SolicitudInvalidaException("RUT inválido"));

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rut": "12345678-9",
                                  "nombre": "Juan Pérez",
                                  "correo": "juan@induztek.cl",
                                  "password": "Clave123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("RUT inválido"));
    }

    @Test
    @DisplayName("Registrar con un correo mal formado falla la validación y responde 400")
    void registrarConCorreoInvalidoResponde400() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rut": "12345678-5",
                                  "nombre": "Juan Pérez",
                                  "correo": "no-es-correo",
                                  "password": "Clave123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Datos de entrada inválidos"))
                .andExpect(jsonPath("$.errores.correo").exists());
    }

    @Test
    @DisplayName("Iniciar sesión con credenciales correctas responde 200 con el JWT")
    void iniciarSesionResponde200() throws Exception {
        UsuarioMostrarDTO usuario = new UsuarioMostrarDTO();
        usuario.setIdUsuario(3L);
        usuario.setCorreo("tecnico@induztek.cl");
        usuario.setRol("TECNICO");
        usuario.setIdRol(1);

        AuthRespuestaDTO respuesta = new AuthRespuestaDTO("jwt-token", "TECNICO", usuario);
        when(usuarioService.iniciarSesion(any(LoginDTO.class))).thenReturn(respuesta);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "tecnico@induztek.cl",
                                  "password": "Clave123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.rol").value("TECNICO"))
                .andExpect(jsonPath("$.usuario.correo").value("tecnico@induztek.cl"));
    }

    @Test
    @DisplayName("Iniciar sesión con contraseña incorrecta responde 401")
    void iniciarSesionCredencialesInvalidasResponde401() throws Exception {
        when(usuarioService.iniciarSesion(any(LoginDTO.class)))
                .thenThrow(new CredencialesInvalidasException("Correo o contraseña incorrectos"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "tecnico@induztek.cl",
                                  "password": "mala"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    @DisplayName("Iniciar sesión con correo inválido falla la validación y responde 400")
    void iniciarSesionCorreoInvalidoResponde400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "no-es-correo",
                                  "password": "Clave123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.correo").exists());
    }

    @Test
    @DisplayName("Recuperar clave responde 202 aunque el correo exista")
    void recuperarClaveResponde202() throws Exception {
        mockMvc.perform(post("/auth/recuperar-clave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "cliente@induztek.cl"
                                }
                                """))
                .andExpect(status().isAccepted());

        verify(recuperacionService).solicitarCodigo(any(RecuperarClaveDTO.class));
    }

    @Test
    @DisplayName("Recuperar clave con un correo mal formado responde 400")
    void recuperarClaveCorreoInvalidoResponde400() throws Exception {
        mockMvc.perform(post("/auth/recuperar-clave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "sin-arroba"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.correo").exists());
    }

    @Test
    @DisplayName("Restablecer clave con datos válidos responde 204")
    void restablecerClaveResponde204() throws Exception {
        mockMvc.perform(post("/auth/restablecer-clave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "cliente@induztek.cl",
                                  "codigo": "123456",
                                  "claveNueva": "Nueva123!"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(recuperacionService).restablecerClave(any(RestablecerClaveDTO.class));
    }

    @Test
    @DisplayName("Restablecer clave con un código inválido responde 400")
    void restablecerClaveCodigoInvalidoResponde400() throws Exception {
        doThrow(new SolicitudInvalidaException("Código inválido o expirado"))
                .when(recuperacionService).restablecerClave(any(RestablecerClaveDTO.class));

        mockMvc.perform(post("/auth/restablecer-clave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "cliente@induztek.cl",
                                  "codigo": "123456",
                                  "claveNueva": "Nueva123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Código inválido o expirado"));
    }

    @Test
    @DisplayName("Restablecer clave con código no numérico falla la validación y responde 400")
    void restablecerClaveCodigoNoNumericoResponde400() throws Exception {
        mockMvc.perform(post("/auth/restablecer-clave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "cliente@induztek.cl",
                                  "codigo": "abc",
                                  "claveNueva": "Nueva123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.codigo").exists());
    }

    @Test
    @DisplayName("Un cuerpo JSON malformado responde 400 con mensaje de cuerpo inválido")
    void cuerpoMalformadoResponde400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es JSON"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El cuerpo de la solicitud no es válido"));
    }
}
