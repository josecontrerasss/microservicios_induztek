package cl.induztek.instalaciones.controller;

import cl.induztek.instalaciones.config.ManejadorExcepciones;
import cl.induztek.instalaciones.exception.AccesoDenegadoException;
import cl.induztek.instalaciones.exception.ConflictoException;
import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.exception.SolicitudInvalidaException;
import cl.induztek.instalaciones.model.EquipoDTO;
import cl.induztek.instalaciones.model.InstalacionDTO;
import cl.induztek.instalaciones.model.TipoEquipo;
import cl.induztek.instalaciones.model.VisitaDTO;
import cl.induztek.instalaciones.security.JwtFiltro;
import cl.induztek.instalaciones.security.UsuarioToken;
import cl.induztek.instalaciones.service.InstalacionService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InstalacionControllerTest {

    @Mock
    private InstalacionService service;

    private MockMvc mockMvc;
    private final UsuarioToken tecnico = new UsuarioToken(2L, "TECNICO");
    private final UsuarioToken cliente = new UsuarioToken(3L, "CLIENTE");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InstalacionController(service))
                .setControllerAdvice(new ManejadorExcepciones())
                .build();
    }

    @Test
    @DisplayName("Técnico registra una instalación en terreno y responde 201")
    void crearInstalacionComoTecnico() throws Exception {
        InstalacionDTO dto = new InstalacionDTO();
        dto.setIdInstalacion(100L);
        dto.setCodigo("COD-1");
        dto.setNombre("Sala Andes");
        when(service.crearInstalacion(any(), eq(tecnico))).thenReturn(dto);
        String body = """
                {"idCliente":3,"codigo":"COD-1","nombre":"Sala Andes","ubicacion":"Santiago"}
                """;
        mockMvc.perform(post("/instalaciones")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("COD-1"));
    }

    @Test
    @DisplayName("Instalación con campos faltantes responde 400")
    void crearInstalacionConDatosInvalidos() throws Exception {
        String body = """
                {"idCliente":null,"codigo":"","nombre":"","ubicacion":""}
                """;
        mockMvc.perform(post("/instalaciones")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Técnico no asignado intenta registrar instalación y recibe 403")
    void crearInstalacionSinAsignacion() throws Exception {
        when(service.crearInstalacion(any(), eq(tecnico)))
                .thenThrow(new AccesoDenegadoException("El supervisor no te ha asignado a esta empresa"));
        String body = """
                {"idCliente":3,"codigo":"COD-1","nombre":"Sala Andes","ubicacion":"Santiago"}
                """;
        mockMvc.perform(post("/instalaciones")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Código de instalación repetido responde 409")
    void crearInstalacionConCodigoRepetido() throws Exception {
        when(service.crearInstalacion(any(), eq(tecnico)))
                .thenThrow(new ConflictoException("Ya existe una instalación con el código COD-1"));
        String body = """
                {"idCliente":3,"codigo":"COD-1","nombre":"Sala Andes","ubicacion":"Santiago"}
                """;
        mockMvc.perform(post("/instalaciones")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Técnico registra un equipo en una instalación visible y responde 201")
    void crearEquipoComoTecnico() throws Exception {
        EquipoDTO dto = new EquipoDTO();
        dto.setIdEquipo(50L);
        dto.setTag("TAG1");
        when(service.crearEquipo(eq(100L), any(), eq(tecnico))).thenReturn(dto);
        String body = """
                {"tag":"TAG1","idTipoEquipo":1,"marca":"Marca","modelo":"Modelo","numeroSerie":"SN123"}
                """;
        mockMvc.perform(post("/instalaciones/100/equipos")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tag").value("TAG1"));
    }

    @Test
    @DisplayName("Equipo con TAG vacío responde 400")
    void crearEquipoConDatosInvalidos() throws Exception {
        String body = """
                {"tag":"","idTipoEquipo":null,"marca":"","modelo":"","numeroSerie":""}
                """;
        mockMvc.perform(post("/instalaciones/100/equipos")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Tipo de equipo inexistente responde 400")
    void crearEquipoConTipoInexistente() throws Exception {
        when(service.crearEquipo(eq(100L), any(), eq(tecnico)))
                .thenThrow(new SolicitudInvalidaException("El tipo de equipo no existe"));
        String body = """
                {"tag":"TAG1","idTipoEquipo":99,"marca":"Marca","modelo":"Modelo","numeroSerie":"SN123"}
                """;
        mockMvc.perform(post("/instalaciones/100/equipos")
                        .requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cliente lista instalaciones de su propia empresa y recibe 200")
    void listarInstalacionesDeCliente() throws Exception {
        InstalacionDTO dto = new InstalacionDTO();
        dto.setIdInstalacion(100L);
        when(service.instalacionesDeCliente(3L, cliente)).thenReturn(List.of(dto));
        mockMvc.perform(get("/clientes/3/instalaciones").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idInstalacion").value(100L));
    }

    @Test
    @DisplayName("Cliente ajeno intenta ver instalaciones de otra empresa y recibe 403")
    void listarInstalacionesDeOtroCliente() throws Exception {
        when(service.instalacionesDeCliente(eq(77L), eq(cliente)))
                .thenThrow(new AccesoDenegadoException("Solo puedes consultar tu propia información"));
        mockMvc.perform(get("/clientes/77/instalaciones").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, cliente))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Técnico lista sus instalaciones asignadas")
    void listarInstalacionesDeTecnico() throws Exception {
        when(service.instalacionesDeTecnico(eq(2L), eq(tecnico))).thenReturn(List.of());
        mockMvc.perform(get("/tecnicos/2/instalaciones").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Técnico lista sus visitas asignadas")
    void listarVisitasDeTecnico() throws Exception {
        VisitaDTO dto = new VisitaDTO();
        dto.setIdVisita(30L);
        when(service.visitasDeTecnico(eq(2L), eq(tecnico))).thenReturn(List.of(dto));
        mockMvc.perform(get("/tecnicos/2/visitas").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idVisita").value(30L));
    }

    @Test
    @DisplayName("Cliente lista las visitas realizadas en sus instalaciones")
    void listarVisitasDeCliente() throws Exception {
        when(service.visitasDeCliente(eq(3L), eq(cliente))).thenReturn(List.of());
        mockMvc.perform(get("/clientes/3/visitas").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, cliente))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Técnico lista los equipos de sus instalaciones")
    void listarEquiposDeTecnico() throws Exception {
        when(service.equiposDeTecnico(eq(2L), eq(tecnico))).thenReturn(List.of());
        mockMvc.perform(get("/tecnicos/2/equipos").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Obtener instalación propia responde 200")
    void obtenerInstalacion() throws Exception {
        InstalacionDTO dto = new InstalacionDTO();
        dto.setIdInstalacion(100L);
        when(service.obtenerInstalacion(eq(100L), eq(tecnico))).thenReturn(dto);
        mockMvc.perform(get("/instalaciones/100").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idInstalacion").value(100L));
    }

    @Test
    @DisplayName("Obtener instalación inexistente responde 404")
    void obtenerInstalacionInexistente() throws Exception {
        when(service.obtenerInstalacion(eq(999L), eq(tecnico)))
                .thenThrow(new RecursoNoEncontradoException("Instalación no encontrada"));
        mockMvc.perform(get("/instalaciones/999").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Listar equipos de una instalación propia responde 200")
    void listarEquiposDeInstalacion() throws Exception {
        when(service.equiposDeInstalacion(eq(100L), eq(tecnico))).thenReturn(List.of());
        mockMvc.perform(get("/instalaciones/100/equipos").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Obtener equipo existente responde 200")
    void obtenerEquipo() throws Exception {
        EquipoDTO dto = new EquipoDTO();
        dto.setIdEquipo(50L);
        when(service.obtenerEquipo(eq(50L), eq(tecnico))).thenReturn(dto);
        mockMvc.perform(get("/equipos/50").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEquipo").value(50L));
    }

    @Test
    @DisplayName("Obtener equipo inexistente responde 404")
    void obtenerEquipoInexistente() throws Exception {
        when(service.obtenerEquipo(eq(999L), eq(tecnico)))
                .thenThrow(new RecursoNoEncontradoException("Equipo no encontrado"));
        mockMvc.perform(get("/equipos/999").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Obtener visita existente responde 200")
    void obtenerVisita() throws Exception {
        VisitaDTO dto = new VisitaDTO();
        dto.setIdVisita(30L);
        when(service.obtenerVisita(eq(30L), eq(tecnico))).thenReturn(dto);
        mockMvc.perform(get("/visitas/30").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idVisita").value(30L));
    }

    @Test
    @DisplayName("Obtener visita inexistente responde 404")
    void obtenerVisitaInexistente() throws Exception {
        when(service.obtenerVisita(eq(999L), eq(tecnico)))
                .thenThrow(new RecursoNoEncontradoException("Visita no encontrada"));
        mockMvc.perform(get("/visitas/999").requestAttr(JwtFiltro.ATRIBUTO_USUARIO, tecnico))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Catálogo de tipos de equipo se expone sin requerir usuario")
    void listarTiposEquipo() throws Exception {
        TipoEquipo tipo = new TipoEquipo();
        tipo.setId(1L);
        tipo.setNombre("Transformador");
        when(service.tiposEquipo()).thenReturn(List.of(tipo));
        mockMvc.perform(get("/tipos-equipo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Transformador"));
        verify(service).tiposEquipo();
    }
}
