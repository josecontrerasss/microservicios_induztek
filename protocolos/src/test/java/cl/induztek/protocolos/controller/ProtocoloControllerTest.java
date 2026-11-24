package cl.induztek.protocolos.controller;

import cl.induztek.protocolos.config.ManejadorExcepciones;
import cl.induztek.protocolos.model.ProtocoloConMedicionesDTO;
import cl.induztek.protocolos.model.ProtocoloDTO;
import cl.induztek.protocolos.model.SyncRespuestaDTO;
import cl.induztek.protocolos.security.JwtFiltro;
import cl.induztek.protocolos.security.UsuarioToken;
import cl.induztek.protocolos.service.ProtocoloService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProtocoloControllerTest {

    @Mock
    private ProtocoloService service;

    private MockMvc mockMvc;

    private final UsuarioToken usuarioTecnico = new UsuarioToken(1L, "TECNICO");
    private final UsuarioToken usuarioSupervisor = new UsuarioToken(2L, "SUPERVISOR");
    private final UsuarioToken usuarioCliente = new UsuarioToken(3L, "CLIENTE");

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ProtocoloController(service))
                .setControllerAdvice(new ManejadorExcepciones())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("Técnico crea protocolo de terreno y recibe 201")
    void crearProtocoloExitoso() throws Exception {
        ProtocoloConMedicionesDTO respuesta = new ProtocoloConMedicionesDTO();
        ProtocoloDTO protocolo = new ProtocoloDTO();
        protocolo.setNumero("P-001");
        respuesta.setProtocolo(protocolo);
        when(service.crear(any(), any(), any())).thenReturn(respuesta);

        String json = """
                {
                  "protocolo": {
                    "numero": "P-001",
                    "idVisita": 1,
                    "idEquipo": 2,
                    "fechaHoraPrueba": "2026-01-01T10:00:00",
                    "resultado": "APROBADO"
                  },
                  "mediciones": []
                }
                """;

        mockMvc.perform(post("/protocolos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioTecnico)
                .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocolo.numero").value("P-001"));
    }

    @Test
    @DisplayName("Técnico envía protocolo incompleto y recibe 400")
    void crearProtocoloConDatosInvalidosRetorna400() throws Exception {
        String json = """
                {
                  "protocolo": {
                    "numero": "",
                    "idVisita": null,
                    "idEquipo": null,
                    "fechaHoraPrueba": "",
                    "resultado": ""
                  },
                  "mediciones": []
                }
                """;

        mockMvc.perform(post("/protocolos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioTecnico)
                .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Datos de entrada inválidos"));
    }

    @Test
    @DisplayName("Técnico crea protocolo con número ya usado y recibe 409")
    void crearProtocoloDuplicadoRetorna409() throws Exception {
        when(service.crear(any(), any(), any()))
                .thenThrow(new cl.induztek.protocolos.exception.ConflictoException("Ya existe un protocolo con el número P-001"));

        String json = """
                {
                  "protocolo": {
                    "numero": "P-001",
                    "idVisita": 1,
                    "idEquipo": 2,
                    "fechaHoraPrueba": "2026-01-01T10:00:00",
                    "resultado": "APROBADO"
                  },
                  "mediciones": []
                }
                """;

        mockMvc.perform(post("/protocolos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioTecnico)
                .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un protocolo con el número P-001"));
    }

    @Test
    @DisplayName("Cliente intenta crear protocolo y se le deniega por rol")
    void crearProtocoloConRolIncorrectoRetorna403() throws Exception {
        when(service.crear(any(), any(), any()))
                .thenThrow(new cl.induztek.protocolos.exception.AccesoDenegadoException("Esta acción es solo para el rol TECNICO"));

        String json = """
                {
                  "protocolo": {
                    "numero": "P-001",
                    "idVisita": 1,
                    "idEquipo": 2,
                    "fechaHoraPrueba": "2026-01-01T10:00:00",
                    "resultado": "APROBADO"
                  },
                  "mediciones": []
                }
                """;

        mockMvc.perform(post("/protocolos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioCliente)
                .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Esta acción es solo para el rol TECNICO"));
    }

    @Test
    @DisplayName("Técnico sincroniza protocolos de terreno y recibe aceptados")
    void sincronizarProtocolosExitoso() throws Exception {
        SyncRespuestaDTO respuesta = new SyncRespuestaDTO(1, List.of("P-001"), List.of());
        when(service.sincronizar(any(), any(), any())).thenReturn(respuesta);

        String json = """
                [
                  {
                    "protocolo": {
                      "numero": "P-001",
                      "idVisita": 1,
                      "idEquipo": 2,
                      "fechaHoraPrueba": "2026-01-01T10:00:00",
                      "resultado": "APROBADO"
                    },
                    "mediciones": []
                  }
                ]
                """;

        mockMvc.perform(post("/protocolos/sincronizar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioTecnico)
                .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sincronizados").value(1))
                .andExpect(jsonPath("$.aceptados[0]").value("P-001"));
    }

    @Test
    @DisplayName("Técnico obtiene un protocolo propio y ve sus datos")
    void obtenerProtocoloExitoso() throws Exception {
        ProtocoloConMedicionesDTO respuesta = new ProtocoloConMedicionesDTO();
        ProtocoloDTO protocolo = new ProtocoloDTO();
        protocolo.setNumero("P-001");
        respuesta.setProtocolo(protocolo);
        when(service.obtener(1L, usuarioTecnico)).thenReturn(respuesta);

        mockMvc.perform(get("/protocolos/1")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioTecnico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolo.numero").value("P-001"));
    }

    @Test
    @DisplayName("Técnico busca protocolo inexistente y recibe 404")
    void obtenerProtocoloNoEncontradoRetorna404() throws Exception {
        when(service.obtener(1L, usuarioTecnico))
                .thenThrow(new cl.induztek.protocolos.exception.RecursoNoEncontradoException("Protocolo no encontrado"));

        mockMvc.perform(get("/protocolos/1")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioTecnico))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Protocolo no encontrado"));
    }

    @Test
    @DisplayName("Técnico intenta ver protocolo ajeno y recibe 403")
    void obtenerProtocoloSinAccesoRetorna403() throws Exception {
        when(service.obtener(1L, usuarioTecnico))
                .thenThrow(new cl.induztek.protocolos.exception.AccesoDenegadoException("No tienes acceso a este protocolo"));

        mockMvc.perform(get("/protocolos/1")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioTecnico))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tienes acceso a este protocolo"));
    }

    @Test
    @DisplayName("Cliente consulta sus protocolos y ve la lista")
    void protocolosDeClienteExitoso() throws Exception {
        ProtocoloDTO dto = new ProtocoloDTO();
        dto.setNumero("P-001");
        when(service.protocolosDeCliente(eq(5L), any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/clientes/5/protocolos")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("P-001"));
    }

    @Test
    @DisplayName("Cliente intenta ver protocolos ajenos y recibe 403")
    void protocolosDeClienteProhibidoRetorna403() throws Exception {
        when(service.protocolosDeCliente(eq(5L), any()))
                .thenThrow(new cl.induztek.protocolos.exception.AccesoDenegadoException("Solo puedes consultar tu propia información"));

        mockMvc.perform(get("/clientes/5/protocolos")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioCliente))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo puedes consultar tu propia información"));
    }

    @Test
    @DisplayName("Técnico consulta sus protocolos y ve la lista")
    void protocolosDeTecnicoExitoso() throws Exception {
        ProtocoloDTO dto = new ProtocoloDTO();
        dto.setNumero("P-001");
        when(service.protocolosDeTecnico(eq(1L), any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/tecnicos/1/protocolos")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioTecnico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("P-001"));
    }

    @Test
    @DisplayName("Técnico intenta ver protocolos de otro técnico y recibe 403")
    void protocolosDeTecnicoProhibidoRetorna403() throws Exception {
        when(service.protocolosDeTecnico(eq(2L), any()))
                .thenThrow(new cl.induztek.protocolos.exception.AccesoDenegadoException("Solo puedes consultar tu propia información"));

        mockMvc.perform(get("/tecnicos/2/protocolos")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioTecnico))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Supervisor lista protocolos pendientes de revisión")
    void pendientesDeRevisionExitoso() throws Exception {
        ProtocoloConMedicionesDTO respuesta = new ProtocoloConMedicionesDTO();
        ProtocoloDTO protocolo = new ProtocoloDTO();
        protocolo.setNumero("P-001");
        respuesta.setProtocolo(protocolo);
        when(service.pendientesDeRevision(usuarioSupervisor)).thenReturn(List.of(respuesta));

        mockMvc.perform(get("/protocolos/pendientes")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioSupervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].protocolo.numero").value("P-001"));
    }

    @Test
    @DisplayName("Técnico intenta ver pendientes de revisión y recibe 403")
    void pendientesDeRevisionProhibidoRetorna403() throws Exception {
        when(service.pendientesDeRevision(usuarioTecnico))
                .thenThrow(new cl.induztek.protocolos.exception.AccesoDenegadoException("Esta acción es solo para el rol SUPERVISOR"));

        mockMvc.perform(get("/protocolos/pendientes")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioTecnico))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Supervisor aprueba protocolo de terreno y queda formalizado")
    void aprobarProtocoloExitoso() throws Exception {
        ProtocoloDTO dto = new ProtocoloDTO();
        dto.setNumero("P-001");
        dto.setEstado("FORMALIZADO");
        when(service.cambiarEstado(1L, true, usuarioSupervisor)).thenReturn(dto);

        mockMvc.perform(put("/protocolos/1/aprobar")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioSupervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FORMALIZADO"));
    }

    @Test
    @DisplayName("Supervisor aprueba protocolo inexistente y recibe 404")
    void aprobarProtocoloNoEncontradoRetorna404() throws Exception {
        when(service.cambiarEstado(1L, true, usuarioSupervisor))
                .thenThrow(new cl.induztek.protocolos.exception.RecursoNoEncontradoException("Protocolo no encontrado"));

        mockMvc.perform(put("/protocolos/1/aprobar")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioSupervisor))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Supervisor aprueba protocolo ya revisado y recibe 409")
    void aprobarProtocoloYaRevisadoRetorna409() throws Exception {
        when(service.cambiarEstado(1L, true, usuarioSupervisor))
                .thenThrow(new cl.induztek.protocolos.exception.ConflictoException("El protocolo ya fue revisado"));

        mockMvc.perform(put("/protocolos/1/aprobar")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioSupervisor))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Técnico intenta aprobar protocolo y recibe 403")
    void aprobarProtocoloConRolIncorrectoRetorna403() throws Exception {
        when(service.cambiarEstado(1L, true, usuarioTecnico))
                .thenThrow(new cl.induztek.protocolos.exception.AccesoDenegadoException("Esta acción es solo para el rol SUPERVISOR"));

        mockMvc.perform(put("/protocolos/1/aprobar")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioTecnico))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Supervisor rechaza protocolo de terreno y queda anulado")
    void rechazarProtocoloExitoso() throws Exception {
        ProtocoloDTO dto = new ProtocoloDTO();
        dto.setNumero("P-001");
        dto.setEstado("ANULADO");
        when(service.cambiarEstado(1L, false, usuarioSupervisor)).thenReturn(dto);

        mockMvc.perform(put("/protocolos/1/rechazar")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioSupervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADO"));
    }

    @Test
    @DisplayName("Supervisor rechaza protocolo inexistente y recibe 404")
    void rechazarProtocoloNoEncontradoRetorna404() throws Exception {
        when(service.cambiarEstado(1L, false, usuarioSupervisor))
                .thenThrow(new cl.induztek.protocolos.exception.RecursoNoEncontradoException("Protocolo no encontrado"));

        mockMvc.perform(put("/protocolos/1/rechazar")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioSupervisor))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Supervisor rechaza protocolo ya revisado y recibe 409")
    void rechazarProtocoloYaRevisadoRetorna409() throws Exception {
        when(service.cambiarEstado(1L, false, usuarioSupervisor))
                .thenThrow(new cl.induztek.protocolos.exception.ConflictoException("El protocolo ya fue revisado"));

        mockMvc.perform(put("/protocolos/1/rechazar")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioSupervisor))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Técnico intenta rechazar protocolo y recibe 403")
    void rechazarProtocoloConRolIncorrectoRetorna403() throws Exception {
        when(service.cambiarEstado(1L, false, usuarioTecnico))
                .thenThrow(new cl.induztek.protocolos.exception.AccesoDenegadoException("Esta acción es solo para el rol SUPERVISOR"));

        mockMvc.perform(put("/protocolos/1/rechazar")
                .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, usuarioTecnico))
                .andExpect(status().isForbidden());
    }
}
