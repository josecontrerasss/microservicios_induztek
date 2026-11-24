package cl.induztek.instalaciones.service;

import cl.induztek.instalaciones.exception.AccesoDenegadoException;
import cl.induztek.instalaciones.exception.ConflictoException;
import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.exception.SolicitudInvalidaException;
import cl.induztek.instalaciones.model.Equipo;
import cl.induztek.instalaciones.model.EquipoCrearDTO;
import cl.induztek.instalaciones.model.EquipoDTO;
import cl.induztek.instalaciones.model.Instalacion;
import cl.induztek.instalaciones.model.InstalacionCrearDTO;
import cl.induztek.instalaciones.model.InstalacionDTO;
import cl.induztek.instalaciones.model.TipoEquipo;
import cl.induztek.instalaciones.model.Visita;
import cl.induztek.instalaciones.model.VisitaDTO;
import cl.induztek.instalaciones.repository.AsignacionRepository;
import cl.induztek.instalaciones.repository.EquipoRepository;
import cl.induztek.instalaciones.repository.InstalacionRepository;
import cl.induztek.instalaciones.repository.TipoEquipoRepository;
import cl.induztek.instalaciones.repository.VisitaRepository;
import cl.induztek.instalaciones.security.UsuarioToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstalacionServiceTest {

    @Mock
    private InstalacionRepository instalacionRepository;
    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private VisitaRepository visitaRepository;
    @Mock
    private TipoEquipoRepository tipoEquipoRepository;
    @Mock
    private AsignacionRepository asignacionRepository;
    @InjectMocks
    private InstalacionService service;

    private final UsuarioToken tecnico = new UsuarioToken(2L, "TECNICO");
    private final UsuarioToken cliente = new UsuarioToken(3L, "CLIENTE");

    private Instalacion instalacionDeCliente() {
        Instalacion i = new Instalacion();
        i.setId(100L);
        i.setIdCliente(3L);
        i.setCodigo("COD-1");
        i.setNombre("Sala Andes");
        i.setUbicacion("Santiago");
        i.setObservacionesAcceso("");
        i.setUltimaActividad("15 ene. 2026");
        return i;
    }

    private Visita visitaDeInstalacion() {
        Visita v = new Visita();
        v.setId(30L);
        v.setNumeroOt("OT-2026-AAAA1111");
        v.setIdInstalacion(100L);
        v.setFecha("15-01-2026");
        v.setTipoVisita("PRUEBAS_Y_MANT");
        v.setIdTecnico(2L);
        return v;
    }

    private Equipo equipoDeInstalacion() {
        Equipo e = new Equipo();
        e.setId(50L);
        e.setIdInstalacion(100L);
        e.setTag("TAG1");
        e.setIdTipoEquipo(1L);
        e.setMarca("Marca");
        e.setModelo("Modelo");
        e.setNumeroSerie("SN123");
        e.setAnioFabricacion(2020);
        e.setPotenciaKva(10.5);
        e.setTensionPrimariaKv(1.0);
        e.setTensionSecundariaKv(0.4);
        e.setCorrienteNominalA(20.0);
        e.setObservaciones("");
        return e;
    }

    private TipoEquipo tipoEquipo() {
        TipoEquipo t = new TipoEquipo();
        t.setId(1L);
        t.setNombre("Transformador");
        t.setClasificacion("ELECTRICO");
        return t;
    }

    @Test
    @DisplayName("Cliente lista las instalaciones de su propia empresa")
    void instalacionesDeClienteOk() {
        when(instalacionRepository.findByIdClienteOrderByNombreAsc(3L)).thenReturn(List.of(instalacionDeCliente()));
        List<InstalacionDTO> lista = service.instalacionesDeCliente(3L, cliente);
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getCodigo()).isEqualTo("COD-1");
    }

    @Test
    @DisplayName("Cliente no puede ver instalaciones de otra empresa")
    void instalacionesDeOtroCliente() {
        UsuarioToken otro = new UsuarioToken(99L, "CLIENTE");
        assertThatThrownBy(() -> service.instalacionesDeCliente(3L, otro))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("Solo puedes consultar tu propia información");
    }

    @Test
    @DisplayName("Técnico lista las instalaciones que tiene asignadas")
    void instalacionesDeTecnicoOk() {
        when(instalacionRepository.findAsignadasATecnico(2L)).thenReturn(List.of(instalacionDeCliente()));
        List<InstalacionDTO> lista = service.instalacionesDeTecnico(2L, tecnico);
        assertThat(lista).hasSize(1);
    }

    @Test
    @DisplayName("Técnico no puede ver instalaciones de otro técnico")
    void instalacionesDeOtroTecnico() {
        UsuarioToken otro = new UsuarioToken(99L, "TECNICO");
        assertThatThrownBy(() -> service.instalacionesDeTecnico(2L, otro))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Cliente lista las visitas de sus instalaciones")
    void visitasDeClienteOk() {
        when(instalacionRepository.findByIdClienteOrderByNombreAsc(3L)).thenReturn(List.of(instalacionDeCliente()));
        when(visitaRepository.findByIdInstalacionInOrderByIdDesc(List.of(100L))).thenReturn(List.of(visitaDeInstalacion()));
        List<VisitaDTO> lista = service.visitasDeCliente(3L, cliente);
        assertThat(lista).hasSize(1);
    }

    @Test
    @DisplayName("Técnico lista sus visitas asignadas")
    void visitasDeTecnicoOk() {
        when(visitaRepository.findByIdTecnicoOrderByIdDesc(2L)).thenReturn(List.of(visitaDeInstalacion()));
        List<VisitaDTO> lista = service.visitasDeTecnico(2L, tecnico);
        assertThat(lista).hasSize(1);
    }

    @Test
    @DisplayName("Técnico lista los equipos de sus instalaciones")
    void equiposDeTecnicoOk() {
        when(instalacionRepository.findAsignadasATecnico(2L)).thenReturn(List.of(instalacionDeCliente()));
        when(equipoRepository.findByIdInstalacionInOrderByTagAsc(List.of(100L))).thenReturn(List.of(equipoDeInstalacion()));
        when(tipoEquipoRepository.findAll()).thenReturn(List.of(tipoEquipo()));
        List<EquipoDTO> lista = service.equiposDeTecnico(2L, tecnico);
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getTipoEquipo()).isEqualTo("Transformador");
    }

    @Test
    @DisplayName("Técnico obtiene una instalación a la que está asignado")
    void obtenerInstalacionVisibleParaTecnico() {
        when(instalacionRepository.findById(100L)).thenReturn(Optional.of(instalacionDeCliente()));
        when(visitaRepository.existsByIdInstalacionAndIdTecnico(100L, 2L)).thenReturn(true);
        InstalacionDTO dto = service.obtenerInstalacion(100L, tecnico);
        assertThat(dto.getIdInstalacion()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Cliente obtiene una instalación de su propia empresa")
    void obtenerInstalacionVisibleParaCliente() {
        when(instalacionRepository.findById(100L)).thenReturn(Optional.of(instalacionDeCliente()));
        InstalacionDTO dto = service.obtenerInstalacion(100L, cliente);
        assertThat(dto.getIdInstalacion()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Obtener instalación inexistente responde no encontrada")
    void obtenerInstalacionInexistente() {
        when(instalacionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtenerInstalacion(999L, tecnico))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Instalación no encontrada");
    }

    @Test
    @DisplayName("Instalación de otro cliente bloquea el acceso")
    void obtenerInstalacionSinAcceso() {
        when(instalacionRepository.findById(100L)).thenReturn(Optional.of(instalacionDeCliente()));
        UsuarioToken otro = new UsuarioToken(77L, "CLIENTE");
        assertThatThrownBy(() -> service.obtenerInstalacion(100L, otro))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("No tienes acceso a esta instalación");
    }

    @Test
    @DisplayName("Técnico sin visita asignada no puede ver la instalación")
    void instalacionNoVisibleParaTecnicoNoAsignado() {
        when(instalacionRepository.findById(100L)).thenReturn(Optional.of(instalacionDeCliente()));
        when(visitaRepository.existsByIdInstalacionAndIdTecnico(100L, 2L)).thenReturn(false);
        assertThatThrownBy(() -> service.obtenerInstalacion(100L, tecnico))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Listar equipos de una instalación propia expone sus equipos")
    void equiposDeInstalacionOk() {
        when(instalacionRepository.findById(100L)).thenReturn(Optional.of(instalacionDeCliente()));
        when(visitaRepository.existsByIdInstalacionAndIdTecnico(100L, 2L)).thenReturn(true);
        when(equipoRepository.findByIdInstalacionOrderByTagAsc(100L)).thenReturn(List.of(equipoDeInstalacion()));
        when(tipoEquipoRepository.findAll()).thenReturn(List.of(tipoEquipo()));
        List<EquipoDTO> lista = service.equiposDeInstalacion(100L, tecnico);
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getTag()).isEqualTo("TAG1");
    }

    @Test
    @DisplayName("Obtener equipo existente de una instalación visible")
    void obtenerEquipoVisible() {
        when(equipoRepository.findById(50L)).thenReturn(Optional.of(equipoDeInstalacion()));
        when(instalacionRepository.findById(100L)).thenReturn(Optional.of(instalacionDeCliente()));
        when(visitaRepository.existsByIdInstalacionAndIdTecnico(100L, 2L)).thenReturn(true);
        when(tipoEquipoRepository.findAll()).thenReturn(List.of(tipoEquipo()));
        EquipoDTO dto = service.obtenerEquipo(50L, tecnico);
        assertThat(dto.getIdEquipo()).isEqualTo(50L);
    }

    @Test
    @DisplayName("Obtener equipo inexistente responde no encontrado")
    void obtenerEquipoInexistente() {
        when(equipoRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtenerEquipo(999L, tecnico))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Equipo no encontrado");
    }

    @Test
    @DisplayName("Obtener visita existente de una instalación visible")
    void obtenerVisitaVisible() {
        when(visitaRepository.findById(30L)).thenReturn(Optional.of(visitaDeInstalacion()));
        when(instalacionRepository.findById(100L)).thenReturn(Optional.of(instalacionDeCliente()));
        when(visitaRepository.existsByIdInstalacionAndIdTecnico(100L, 2L)).thenReturn(true);
        VisitaDTO dto = service.obtenerVisita(30L, tecnico);
        assertThat(dto.getIdVisita()).isEqualTo(30L);
    }

    @Test
    @DisplayName("Obtener visita inexistente responde no encontrada")
    void obtenerVisitaInexistente() {
        when(visitaRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtenerVisita(999L, tecnico))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Visita no encontrada");
    }

    @Test
    @DisplayName("Técnico asignado registra una nueva instalación en terreno")
    void crearInstalacionOk() {
        InstalacionCrearDTO dto = new InstalacionCrearDTO();
        dto.setIdCliente(3L);
        dto.setCodigo(" cod-1 ");
        dto.setNombre("  Sala Andes  ");
        dto.setUbicacion("  Santiago  ");
        dto.setObservacionesAcceso(null);
        when(asignacionRepository.existsByIdClienteAndIdTecnico(3L, 2L)).thenReturn(true);
        when(instalacionRepository.existsByCodigo("COD-1")).thenReturn(false);
        when(instalacionRepository.save(any(Instalacion.class))).thenAnswer(inv -> {
            Instalacion i = inv.getArgument(0);
            i.setId(100L);
            return i;
        });
        when(visitaRepository.existsByIdInstalacionAndIdTecnico(100L, 2L)).thenReturn(false);
        InstalacionDTO dtoResultado = service.crearInstalacion(dto, tecnico);
        assertThat(dtoResultado.getCodigo()).isEqualTo("COD-1");
        assertThat(dtoResultado.getNombre()).isEqualTo("Sala Andes");
        assertThat(dtoResultado.getObservacionesAcceso()).isEmpty();
        verify(visitaRepository).save(any(Visita.class));
    }

    @Test
    @DisplayName("Técnico no asignado no puede registrar instalación")
    void crearInstalacionSinAsignacion() {
        InstalacionCrearDTO dto = new InstalacionCrearDTO();
        dto.setIdCliente(3L);
        dto.setCodigo("COD-1");
        dto.setNombre("Sala Andes");
        dto.setUbicacion("Santiago");
        when(asignacionRepository.existsByIdClienteAndIdTecnico(3L, 2L)).thenReturn(false);
        assertThatThrownBy(() -> service.crearInstalacion(dto, tecnico))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Usuario no técnico no puede registrar instalación")
    void crearInstalacionSinSerTecnico() {
        InstalacionCrearDTO dto = new InstalacionCrearDTO();
        dto.setIdCliente(3L);
        dto.setCodigo("COD-1");
        dto.setNombre("Sala Andes");
        dto.setUbicacion("Santiago");
        assertThatThrownBy(() -> service.crearInstalacion(dto, cliente))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Código de instalación repetido produce conflicto")
    void crearInstalacionConCodigoRepetido() {
        InstalacionCrearDTO dto = new InstalacionCrearDTO();
        dto.setIdCliente(3L);
        dto.setCodigo("COD-1");
        dto.setNombre("Sala Andes");
        dto.setUbicacion("Santiago");
        when(asignacionRepository.existsByIdClienteAndIdTecnico(3L, 2L)).thenReturn(true);
        when(instalacionRepository.existsByCodigo("COD-1")).thenReturn(true);
        assertThatThrownBy(() -> service.crearInstalacion(dto, tecnico))
                .isInstanceOf(ConflictoException.class);
        verify(instalacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Observaciones de acceso no nulas se aplican con trim")
    void crearInstalacionConObservaciones() {
        InstalacionCrearDTO dto = new InstalacionCrearDTO();
        dto.setIdCliente(3L);
        dto.setCodigo("cod-2");
        dto.setNombre("Sala 2");
        dto.setUbicacion("Viña");
        dto.setObservacionesAcceso("  timbre  ");
        when(asignacionRepository.existsByIdClienteAndIdTecnico(3L, 2L)).thenReturn(true);
        when(instalacionRepository.existsByCodigo("COD-2")).thenReturn(false);
        when(instalacionRepository.save(any(Instalacion.class))).thenAnswer(inv -> {
            Instalacion i = inv.getArgument(0);
            i.setId(200L);
            return i;
        });
        when(visitaRepository.existsByIdInstalacionAndIdTecnico(200L, 2L)).thenReturn(false);
        service.crearInstalacion(dto, tecnico);
        ArgumentCaptor<Instalacion> captor = ArgumentCaptor.forClass(Instalacion.class);
        verify(instalacionRepository).save(captor.capture());
        assertThat(captor.getValue().getObservacionesAcceso()).isEqualTo("timbre");
    }

    @Test
    @DisplayName("Técnico registra un equipo válido en su instalación")
    void crearEquipoOk() {
        EquipoCrearDTO dto = new EquipoCrearDTO();
        dto.setTag(" TAG1 ");
        dto.setIdTipoEquipo(1L);
        dto.setMarca("  Marca  ");
        dto.setModelo("  Modelo  ");
        dto.setNumeroSerie("  SN123  ");
        dto.setAnioFabricacion(2020);
        dto.setPotenciaKva(10.5);
        dto.setTensionPrimariaKv(1.0);
        dto.setTensionSecundariaKv(0.4);
        dto.setCorrienteNominalA(20.0);
        dto.setObservaciones(null);
        when(instalacionRepository.findById(100L)).thenReturn(Optional.of(instalacionDeCliente()));
        when(visitaRepository.existsByIdInstalacionAndIdTecnico(100L, 2L)).thenReturn(true);
        when(tipoEquipoRepository.existsById(1L)).thenReturn(true);
        when(equipoRepository.existsByIdInstalacionAndTag(100L, "TAG1")).thenReturn(false);
        when(equipoRepository.save(any(Equipo.class))).thenAnswer(inv -> {
            Equipo e = inv.getArgument(0);
            e.setId(50L);
            return e;
        });
        when(tipoEquipoRepository.findAll()).thenReturn(List.of(tipoEquipo()));
        EquipoDTO resultado = service.crearEquipo(100L, dto, tecnico);
        assertThat(resultado.getTag()).isEqualTo("TAG1");
        assertThat(resultado.getTipoEquipo()).isEqualTo("Transformador");
        assertThat(resultado.getObservaciones()).isEmpty();
    }

    @Test
    @DisplayName("Usuario no técnico no puede registrar equipos")
    void crearEquipoSinSerTecnico() {
        assertThatThrownBy(() -> service.crearEquipo(100L, new EquipoCrearDTO(), cliente))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Instalación inexistente al crear equipo responde no encontrada")
    void crearEquipoConInstalacionInexistente() {
        EquipoCrearDTO dto = new EquipoCrearDTO();
        dto.setIdTipoEquipo(1L);
        dto.setTag("TAG1");
        when(instalacionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.crearEquipo(999L, dto, tecnico))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Técnico sin acceso a la instalación no puede registrar equipos")
    void crearEquipoSinAccesoAInstalacion() {
        EquipoCrearDTO dto = new EquipoCrearDTO();
        dto.setIdTipoEquipo(1L);
        dto.setTag("TAG1");
        when(instalacionRepository.findById(100L)).thenReturn(Optional.of(instalacionDeCliente()));
        when(visitaRepository.existsByIdInstalacionAndIdTecnico(100L, 2L)).thenReturn(false);
        assertThatThrownBy(() -> service.crearEquipo(100L, dto, tecnico))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Tipo de equipo inexistente responde solicitud inválida")
    void crearEquipoConTipoInexistente() {
        EquipoCrearDTO dto = new EquipoCrearDTO();
        dto.setIdTipoEquipo(99L);
        dto.setTag("TAG1");
        when(instalacionRepository.findById(100L)).thenReturn(Optional.of(instalacionDeCliente()));
        when(visitaRepository.existsByIdInstalacionAndIdTecnico(100L, 2L)).thenReturn(true);
        when(tipoEquipoRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.crearEquipo(100L, dto, tecnico))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El tipo de equipo no existe");
    }

    @Test
    @DisplayName("TAG duplicado en la instalación responde conflicto")
    void crearEquipoConTagDuplicado() {
        EquipoCrearDTO dto = new EquipoCrearDTO();
        dto.setIdTipoEquipo(1L);
        dto.setTag("TAG1");
        when(instalacionRepository.findById(100L)).thenReturn(Optional.of(instalacionDeCliente()));
        when(visitaRepository.existsByIdInstalacionAndIdTecnico(100L, 2L)).thenReturn(true);
        when(tipoEquipoRepository.existsById(1L)).thenReturn(true);
        when(equipoRepository.existsByIdInstalacionAndTag(100L, "TAG1")).thenReturn(true);
        assertThatThrownBy(() -> service.crearEquipo(100L, dto, tecnico))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    @DisplayName("Asegurar visita no crea una nueva si ya existe para el técnico")
    void asegurarVisitaNoDuplica() {
        when(visitaRepository.existsByIdInstalacionAndIdTecnico(100L, 2L)).thenReturn(true);
        service.asegurarVisita(100L, 2L);
        verify(visitaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Asegurar visita crea una OT cuando no existe para el técnico")
    void asegurarVisitaCreaNueva() {
        when(visitaRepository.existsByIdInstalacionAndIdTecnico(100L, 2L)).thenReturn(false);
        service.asegurarVisita(100L, 2L);
        ArgumentCaptor<Visita> captor = ArgumentCaptor.forClass(Visita.class);
        verify(visitaRepository).save(captor.capture());
        Visita v = captor.getValue();
        assertThat(v.getNumeroOt()).startsWith("OT-");
        assertThat(v.getIdInstalacion()).isEqualTo(100L);
        assertThat(v.getIdTecnico()).isEqualTo(2L);
        assertThat(v.getTipoVisita()).isEqualTo("PRUEBAS_Y_MANT");
    }

    @Test
    @DisplayName("Catálogo de tipos de equipo responde con los tipos ordenados")
    void listarTiposEquipo() {
        when(tipoEquipoRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(tipoEquipo()));
        List<TipoEquipo> lista = service.tiposEquipo();
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getNombre()).isEqualTo("Transformador");
    }

    @Test
    @DisplayName("Técnico no puede listar visitas de otro técnico")
    void visitasDeOtroTecnico() {
        UsuarioToken otro = new UsuarioToken(99L, "TECNICO");
        assertThatThrownBy(() -> service.visitasDeTecnico(2L, otro))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Cliente no puede listar visitas de otro cliente")
    void visitasDeOtroCliente() {
        UsuarioToken otro = new UsuarioToken(77L, "CLIENTE");
        assertThatThrownBy(() -> service.visitasDeCliente(3L, otro))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Técnico no puede listar equipos de otro técnico")
    void equiposDeOtroTecnico() {
        UsuarioToken otro = new UsuarioToken(77L, "TECNICO");
        assertThatThrownBy(() -> service.equiposDeTecnico(2L, otro))
                .isInstanceOf(AccesoDenegadoException.class);
    }
}
