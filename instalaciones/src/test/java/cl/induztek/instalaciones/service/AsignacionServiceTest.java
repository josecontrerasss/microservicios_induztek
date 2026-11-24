package cl.induztek.instalaciones.service;

import cl.induztek.instalaciones.exception.AccesoDenegadoException;
import cl.induztek.instalaciones.exception.ConflictoException;
import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.model.Asignacion;
import cl.induztek.instalaciones.model.AsignacionCrearDTO;
import cl.induztek.instalaciones.model.AsignacionDTO;
import cl.induztek.instalaciones.model.Instalacion;
import cl.induztek.instalaciones.repository.AsignacionRepository;
import cl.induztek.instalaciones.repository.InstalacionRepository;
import cl.induztek.instalaciones.security.UsuarioToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignacionServiceTest {

    @Mock
    private AsignacionRepository asignacionRepository;
    @Mock
    private InstalacionRepository instalacionRepository;
    @Mock
    private InstalacionService instalacionService;
    @InjectMocks
    private AsignacionService service;

    private final UsuarioToken supervisor = new UsuarioToken(1L, "SUPERVISOR");
    private final UsuarioToken tecnico = new UsuarioToken(2L, "TECNICO");
    private final UsuarioToken cliente = new UsuarioToken(3L, "CLIENTE");

    private AsignacionCrearDTO dtoValido() {
        AsignacionCrearDTO dto = new AsignacionCrearDTO();
        dto.setIdCliente(3L);
        dto.setNombreCliente("  Constructora Andes  ");
        dto.setIdTecnico(2L);
        dto.setNombreTecnico("  Pedro Soto  ");
        return dto;
    }

    private Asignacion asignacionGuardada() {
        Asignacion a = new Asignacion();
        a.setId(10L);
        a.setIdCliente(3L);
        a.setNombreCliente("Constructora Andes");
        a.setIdTecnico(2L);
        a.setNombreTecnico("Pedro Soto");
        a.setFechaAsignacion(LocalDateTime.of(2026, 1, 15, 10, 30));
        return a;
    }

    @Test
    @DisplayName("Supervisor crea asignación y se asegura la visita en cada instalación del cliente")
    void crearAsignacionOk() {
        AsignacionCrearDTO dto = dtoValido();
        when(asignacionRepository.existsByIdClienteAndIdTecnico(3L, 2L)).thenReturn(false);
        when(asignacionRepository.save(any(Asignacion.class))).thenReturn(asignacionGuardada());
        Instalacion instalacion = new Instalacion();
        instalacion.setId(100L);
        when(instalacionRepository.findByIdClienteOrderByNombreAsc(3L)).thenReturn(List.of(instalacion));
        AsignacionDTO resultado = service.crear(dto, supervisor);
        assertThat(resultado.getIdAsignacion()).isEqualTo(10L);
        assertThat(resultado.getFechaAsignacion()).isEqualTo("15-01-2026 10:30");
        verify(instalacionService).asegurarVisita(100L, 2L);
    }

    @Test
    @DisplayName("Asignación con técnico ya asignado a la empresa responde conflicto")
    void crearAsignacionDuplicada() {
        AsignacionCrearDTO dto = dtoValido();
        when(asignacionRepository.existsByIdClienteAndIdTecnico(3L, 2L)).thenReturn(true);
        assertThatThrownBy(() -> service.crear(dto, supervisor))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("Ese técnico ya está asignado a esa empresa");
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Técnico no puede crear asignaciones")
    void crearAsignacionSinSerSupervisor() {
        assertThatThrownBy(() -> service.crear(dtoValido(), tecnico))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("Esta acción es solo para el supervisor");
    }

    @Test
    @DisplayName("Supervisor lista todas las asignaciones")
    void listarAsignacionesOk() {
        when(asignacionRepository.findAllByOrderByIdDesc()).thenReturn(List.of(asignacionGuardada()));
        List<AsignacionDTO> lista = service.listar(supervisor);
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getIdAsignacion()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Cliente no puede listar todas las asignaciones")
    void listarAsignacionesSinPermiso() {
        assertThatThrownBy(() -> service.listar(cliente))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Supervisor elimina una asignación existente")
    void eliminarAsignacionExistente() {
        Asignacion a = asignacionGuardada();
        when(asignacionRepository.findById(10L)).thenReturn(Optional.of(a));
        service.eliminar(10L, supervisor);
        verify(asignacionRepository).delete(a);
    }

    @Test
    @DisplayName("Eliminar asignación inexistente responde no encontrado")
    void eliminarAsignacionInexistente() {
        when(asignacionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.eliminar(99L, supervisor))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Asignación no encontrada");
    }

    @Test
    @DisplayName("Técnico no puede eliminar asignaciones")
    void eliminarAsignacionSinPermiso() {
        assertThatThrownBy(() -> service.eliminar(10L, tecnico))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Técnico consulta sus propias empresas asignadas")
    void empresasDeTecnicoPropias() {
        when(asignacionRepository.findByIdTecnicoOrderByNombreClienteAsc(2L)).thenReturn(List.of(asignacionGuardada()));
        List<AsignacionDTO> lista = service.empresasDeTecnico(2L, tecnico);
        assertThat(lista).hasSize(1);
    }

    @Test
    @DisplayName("Técnico no puede consultar las empresas de otro técnico")
    void empresasDeOtroTecnico() {
        UsuarioToken otro = new UsuarioToken(99L, "TECNICO");
        assertThatThrownBy(() -> service.empresasDeTecnico(2L, otro))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Cliente no puede consultar empresas de un técnico")
    void empresasDeTecnicoConRolCliente() {
        assertThatThrownBy(() -> service.empresasDeTecnico(2L, cliente))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Cliente consulta los técnicos asignados a su propia empresa")
    void tecnicosDeClientePropio() {
        when(asignacionRepository.findByIdClienteOrderByNombreTecnicoAsc(3L)).thenReturn(List.of(asignacionGuardada()));
        List<AsignacionDTO> lista = service.tecnicosDeCliente(3L, cliente);
        assertThat(lista).hasSize(1);
    }

    @Test
    @DisplayName("Cliente no puede consultar los técnicos de otra empresa")
    void tecnicosDeOtroCliente() {
        UsuarioToken otro = new UsuarioToken(77L, "CLIENTE");
        assertThatThrownBy(() -> service.tecnicosDeCliente(3L, otro))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Técnico no puede consultar los técnicos de una empresa")
    void tecnicosDeClienteConRolTecnico() {
        assertThatThrownBy(() -> service.tecnicosDeCliente(3L, tecnico))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("La asignación guardada aplica trim a nombres de cliente y técnico")
    void crearAsignacionHaceTrimDeNombres() {
        AsignacionCrearDTO dto = dtoValido();
        when(asignacionRepository.existsByIdClienteAndIdTecnico(3L, 2L)).thenReturn(false);
        when(asignacionRepository.save(any(Asignacion.class))).thenAnswer(inv -> {
            Asignacion a = inv.getArgument(0);
            a.setId(10L);
            a.setFechaAsignacion(LocalDateTime.of(2026, 1, 15, 10, 30));
            return a;
        });
        when(instalacionRepository.findByIdClienteOrderByNombreAsc(3L)).thenReturn(List.of());
        service.crear(dto, supervisor);
        ArgumentCaptor<Asignacion> captor = ArgumentCaptor.forClass(Asignacion.class);
        verify(asignacionRepository).save(captor.capture());
        assertThat(captor.getValue().getNombreCliente()).isEqualTo("Constructora Andes");
        assertThat(captor.getValue().getNombreTecnico()).isEqualTo("Pedro Soto");
    }
}
