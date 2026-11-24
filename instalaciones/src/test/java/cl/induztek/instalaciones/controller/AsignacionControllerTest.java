package cl.induztek.instalaciones.controller;

import cl.induztek.instalaciones.config.ManejadorExcepciones;
import cl.induztek.instalaciones.exception.AccesoDenegadoException;
import cl.induztek.instalaciones.exception.ConflictoException;
import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.model.AsignacionDTO;
import cl.induztek.instalaciones.security.JwtFiltro;
import cl.induztek.instalaciones.security.UsuarioToken;
import cl.induztek.instalaciones.service.AsignacionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AsignacionControllerTest {

    @Mock
    private AsignacionService service;

    private MockMvc mockMvc;
    private final UsuarioToken supervisor = new UsuarioToken(1L, "SUPERVISOR");
    private final UsuarioToken tecnico = new UsuarioToken(2L, "TECNICO");
    private final UsuarioToken cliente = new UsuarioToken(3L, "CLIENTE");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AsignacionController(service))
                .setControllerAdvice(new ManejadorExcepciones())
                .build();
    }

    @Test
    @DisplayName("Supervisor crea asignación de técnico a empresa y responde 201")
    void crearAsignacionComoSupervisor() throws Exception {
        AsignacionDTO dto = new AsignacionDTO();
        dto.setIdAsignacion(10L);
        dto.setIdCliente(3L);
        dto.setNombreCliente("Constructora Andes");
        dto.setIdTecnico(2L);
        dto.setNombreTecnico("Pedro Soto");
        when(service.crear(any(), eq(supervisor))).thenReturn(dto);
        String body = """
                {"idCliente":3,"nombreCliente":"Constructora Andes","idTecnico":2,"nombreTecnico":"Pedro Soto"}
                """;
        mockMvc.perform(post("/asignaciones")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idAsignacion").value(10L))
                .andExpect(jsonPath("$.nombreCliente").value("Constructora Andes"));
    }

    @Test
    @DisplayName("Asignación con campos vacíos responde 400 por validación")
    void crearAsignacionConDatosInvalidos() throws Exception {
        String body = """
                {"idCliente":null,"nombreCliente":"","idTecnico":null,"nombreTecnico":""}
                """;
        mockMvc.perform(post("/asignaciones")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));
        verify(service, never()).crear(any(), any());
    }

    @Test
    @DisplayName("Técnico intenta crear asignación y recibe 403")
    void crearAsignacionSinSerSupervisor() throws Exception {
        when(service.crear(any(), eq(tecnico)))
                .thenThrow(new AccesoDenegadoException("Esta acción es solo para el supervisor"));
        String body = """
                {"idCliente":3,"nombreCliente":"Constructora Andes","idTecnico":2,"nombreTecnico":"Pedro Soto"}
                """;
        mockMvc.perform(post("/asignaciones")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Esta acción es solo para el supervisor"));
    }

    @Test
    @DisplayName("Asignación duplicada responde 409 conflicto")
    void crearAsignacionDuplicada() throws Exception {
        when(service.crear(any(), eq(supervisor)))
                .thenThrow(new ConflictoException("Ese técnico ya está asignado a esa empresa"));
        String body = """
                {"idCliente":3,"nombreCliente":"Constructora Andes","idTecnico":2,"nombreTecnico":"Pedro Soto"}
                """;
        mockMvc.perform(post("/asignaciones")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ese técnico ya está asignado a esa empresa"));
    }

    @Test
    @DisplayName("Supervisor lista todas las asignaciones y recibe 200")
    void listarAsignacionesComoSupervisor() throws Exception {
        AsignacionDTO dto = new AsignacionDTO();
        dto.setIdAsignacion(1L);
        when(service.listar(supervisor)).thenReturn(List.of(dto));
        mockMvc.perform(get("/asignaciones").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, supervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idAsignacion").value(1L));
    }

    @Test
    @DisplayName("Listar asignaciones sin ser supervisor responde 403")
    void listarAsignacionesSinPermiso() throws Exception {
        when(service.listar(tecnico)).thenThrow(new AccesoDenegadoException("Esta acción es solo para el supervisor"));
        mockMvc.perform(get("/asignaciones").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Eliminar asignación existente responde 204")
    void eliminarAsignacionExistente() throws Exception {
        mockMvc.perform(delete("/asignaciones/5").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, supervisor))
                .andExpect(status().isNoContent());
        verify(service).eliminar(5L, supervisor);
    }

    @Test
    @DisplayName("Eliminar asignación inexistente responde 404")
    void eliminarAsignacionInexistente() throws Exception {
        doThrow(new RecursoNoEncontradoException("Asignación no encontrada"))
                .when(service).eliminar(99L, supervisor);
        mockMvc.perform(delete("/asignaciones/99").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, supervisor))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Técnico consulta sus empresas asignadas y recibe 200")
    void empresasDeTecnicoPropio() throws Exception {
        AsignacionDTO dto = new AsignacionDTO();
        dto.setIdAsignacion(1L);
        when(service.empresasDeTecnico(2L, tecnico)).thenReturn(List.of(dto));
        mockMvc.perform(get("/tecnicos/2/empresas").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idAsignacion").value(1L));
    }

    @Test
    @DisplayName("Técnico consulta empresas de otro técnico y recibe 403")
    void empresasDeOtroTecnico() throws Exception {
        when(service.empresasDeTecnico(eq(9L), eq(tecnico)))
                .thenThrow(new AccesoDenegadoException("Solo puedes consultar tu propia información"));
        mockMvc.perform(get("/tecnicos/9/empresas").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cliente consulta los técnicos asignados a su empresa y recibe 200")
    void tecnicosDeClientePropio() throws Exception {
        AsignacionDTO dto = new AsignacionDTO();
        dto.setIdAsignacion(1L);
        when(service.tecnicosDeCliente(3L, cliente)).thenReturn(List.of(dto));
        mockMvc.perform(get("/clientes/3/tecnicos").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idAsignacion").value(1L));
    }

    @Test
    @DisplayName("Cliente consulta técnicos de otra empresa y recibe 403")
    void tecnicosDeOtroCliente() throws Exception {
        when(service.tecnicosDeCliente(eq(77L), eq(cliente)))
                .thenThrow(new AccesoDenegadoException("Solo puedes consultar tu propia información"));
        mockMvc.perform(get("/clientes/77/tecnicos").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, cliente))
                .andExpect(status().isForbidden());
    }
}
