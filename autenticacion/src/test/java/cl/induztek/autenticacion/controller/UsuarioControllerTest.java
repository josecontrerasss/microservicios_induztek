package cl.induztek.autenticacion.controller;

import cl.induztek.autenticacion.config.ManejadorExcepciones;
import cl.induztek.autenticacion.exception.ConflictoException;
import cl.induztek.autenticacion.exception.RecursoNoEncontradoException;
import cl.induztek.autenticacion.exception.SolicitudInvalidaException;
import cl.induztek.autenticacion.model.CambiarClaveDTO;
import cl.induztek.autenticacion.model.UsuarioActualizarDTO;
import cl.induztek.autenticacion.model.UsuarioMostrarDTO;
import cl.induztek.autenticacion.security.JwtFiltro;
import cl.induztek.autenticacion.security.UsuarioToken;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UsuarioController(usuarioService))
                .setControllerAdvice(new ManejadorExcepciones())
                .build();
    }

    @Test
    @DisplayName("Un supervisor lista los técnicos y recibe el listado con rol TECNICO")
    void supervisorListaTecnicos() throws Exception {
        UsuarioMostrarDTO tecnico = new UsuarioMostrarDTO();
        tecnico.setIdUsuario(7L);
        tecnico.setNombre("Técnico Uno");
        tecnico.setRol("TECNICO");
        tecnico.setIdRol(1);

        when(usuarioService.listarPorRol("TECNICO")).thenReturn(List.of(tecnico));

        mockMvc.perform(get("/usuarios")
                        .param("rol", "tecnico")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, new UsuarioToken(1L, "SUPERVISOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUsuario").value(7))
                .andExpect(jsonPath("$[0].rol").value("TECNICO"));

        verify(usuarioService).listarPorRol("TECNICO");
    }

    @Test
    @DisplayName("Un técnico no puede listar usuarios y recibe 403")
    void tecnicoNoPuedeListar() throws Exception {
        mockMvc.perform(get("/usuarios")
                        .param("rol", "tecnico")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, new UsuarioToken(3L, "TECNICO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));

        verify(usuarioService, never()).listarPorRol(any());
    }

    @Test
    @DisplayName("Un cliente obtiene los datos de su propio perfil")
    void clienteObtieneSuPerfil() throws Exception {
        UsuarioMostrarDTO perfil = new UsuarioMostrarDTO();
        perfil.setIdUsuario(30L);
        perfil.setNombre("Cliente Uno");
        perfil.setCorreo("cli@induztek.cl");
        perfil.setRol("CLIENTE");
        perfil.setIdRol(4);

        when(usuarioService.obtenerUsuario(30L)).thenReturn(perfil);

        mockMvc.perform(get("/usuarios/30")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, new UsuarioToken(30L, "CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(30))
                .andExpect(jsonPath("$.correo").value("cli@induztek.cl"));
    }

    @Test
    @DisplayName("Un usuario intentando ver el perfil de otro recibe 403")
    void usuarioNoPuedeVerOtroPerfil() throws Exception {
        mockMvc.perform(get("/usuarios/30")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, new UsuarioToken(99L, "CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo puedes acceder a tu propio perfil"));

        verify(usuarioService, never()).obtenerUsuario(any());
    }

    @Test
    @DisplayName("Obtener un usuario inexistente responde 404")
    void obtenerUsuarioInexistente() throws Exception {
        when(usuarioService.obtenerUsuario(55L))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado con ID: 55"));

        mockMvc.perform(get("/usuarios/55")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, new UsuarioToken(55L, "CLIENTE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404));
    }

    @Test
    @DisplayName("Un cliente actualiza su propio perfil con datos válidos y recibe 200")
    void actualizarPerfilExitoso() throws Exception {
        UsuarioMostrarDTO actualizado = new UsuarioMostrarDTO();
        actualizado.setIdUsuario(40L);
        actualizado.setNombre("Cliente Nuevo");
        actualizado.setCorreo("nuevo@induztek.cl");

        when(usuarioService.actualizarPerfil(eq(40L), any(UsuarioActualizarDTO.class))).thenReturn(actualizado);

        mockMvc.perform(put("/usuarios/40")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, new UsuarioToken(40L, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Cliente Nuevo",
                                  "correo": "nuevo@induztek.cl"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("nuevo@induztek.cl"));
    }

    @Test
    @DisplayName("Un usuario no puede actualizar el perfil de otro y recibe 403")
    void actualizarPerfilAjenoResponde403() throws Exception {
        mockMvc.perform(put("/usuarios/40")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, new UsuarioToken(99L, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Cliente Nuevo",
                                  "correo": "nuevo@induztek.cl"
                                }
                                """))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).actualizarPerfil(any(), any());
    }

    @Test
    @DisplayName("Actualizar con un correo ya usado por otro usuario responde 409")
    void actualizarPerfilCorreoEnUso() throws Exception {
        when(usuarioService.actualizarPerfil(eq(40L), any(UsuarioActualizarDTO.class)))
                .thenThrow(new ConflictoException("Ese correo ya está en uso"));

        mockMvc.perform(put("/usuarios/40")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, new UsuarioToken(40L, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Cliente Nuevo",
                                  "correo": "nuevo@induztek.cl"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409));
    }

    @Test
    @DisplayName("Actualizar con un correo mal formado falla la validación y responde 400")
    void actualizarPerfilValidacionInvalida() throws Exception {
        mockMvc.perform(put("/usuarios/40")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, new UsuarioToken(40L, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "",
                                  "correo": "no-es-correo"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Datos de entrada inválidos"));
    }

    @Test
    @DisplayName("Un técnico cambia su clave con datos válidos y recibe 204")
    void cambiarClaveExitosa() throws Exception {
        mockMvc.perform(put("/usuarios/20/clave")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, new UsuarioToken(20L, "TECNICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "claveActual": "Clave123!",
                                  "claveNueva": "Nueva456!"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(usuarioService).cambiarClave(eq(20L), any(CambiarClaveDTO.class));
    }

    @Test
    @DisplayName("Un usuario no puede cambiar la clave de otro y recibe 403")
    void cambiarClaveAjenaResponde403() throws Exception {
        mockMvc.perform(put("/usuarios/20/clave")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, new UsuarioToken(99L, "TECNICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "claveActual": "Clave123!",
                                  "claveNueva": "Nueva456!"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cambiar clave con la actual incorrecta responde 400")
    void cambiarClaveActualIncorrecta() throws Exception {
        doThrow(new SolicitudInvalidaException("La contraseña actual es incorrecta"))
                .when(usuarioService).cambiarClave(eq(20L), any(CambiarClaveDTO.class));

        mockMvc.perform(put("/usuarios/20/clave")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, new UsuarioToken(20L, "TECNICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "claveActual": "mala",
                                  "claveNueva": "Nueva456!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La contraseña actual es incorrecta"));
    }

    @Test
    @DisplayName("Cambiar clave con una nueva contraseña débil falla la validación y responde 400")
    void cambiarClaveNuevaDebilResponde400() throws Exception {
        mockMvc.perform(put("/usuarios/20/clave")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, new UsuarioToken(20L, "TECNICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "claveActual": "Clave123!",
                                  "claveNueva": "corta"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.claveNueva").exists());
    }
}
