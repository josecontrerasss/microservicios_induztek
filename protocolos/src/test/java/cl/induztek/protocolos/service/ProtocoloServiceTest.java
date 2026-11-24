package cl.induztek.protocolos.service;

import cl.induztek.protocolos.client.InstalacionesCliente;
import cl.induztek.protocolos.exception.AccesoDenegadoException;
import cl.induztek.protocolos.exception.ConflictoException;
import cl.induztek.protocolos.exception.RecursoNoEncontradoException;
import cl.induztek.protocolos.exception.SolicitudInvalidaException;
import cl.induztek.protocolos.model.EquipoRemotoDTO;
import cl.induztek.protocolos.model.InstalacionRemotaDTO;
import cl.induztek.protocolos.model.Medicion;
import cl.induztek.protocolos.model.MedicionDTO;
import cl.induztek.protocolos.model.Protocolo;
import cl.induztek.protocolos.model.ProtocoloConMedicionesDTO;
import cl.induztek.protocolos.model.ProtocoloDTO;
import cl.induztek.protocolos.model.SyncRespuestaDTO;
import cl.induztek.protocolos.model.VisitaRemotaDTO;
import cl.induztek.protocolos.repository.ProtocoloRepository;
import cl.induztek.protocolos.security.UsuarioToken;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProtocoloServiceTest {

    @Mock
    private ProtocoloRepository repository;

    @Mock
    private InstalacionesCliente instalaciones;

    @Mock
    private Validator validator;

    @InjectMocks
    private ProtocoloService service;

    private final UsuarioToken tecnico = new UsuarioToken(1L, "TECNICO");
    private final UsuarioToken otroTecnico = new UsuarioToken(9L, "TECNICO");
    private final UsuarioToken supervisor = new UsuarioToken(2L, "SUPERVISOR");
    private final UsuarioToken cliente = new UsuarioToken(3L, "CLIENTE");
    private final String autorizacion = "Bearer token";

    @Test
    @DisplayName("Cliente intenta crear protocolo y se le deniega")
    void crearConRolIncorrectoLanzaAccesoDenegado() {
        ProtocoloConMedicionesDTO dto = crearDTO("P-001", 1L, 2L);
        assertThatThrownBy(() -> service.crear(dto, cliente, autorizacion))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("Esta acción es solo para el rol TECNICO");
        verify(repository, never()).findByNumero(anyString());
    }

    @Test
    @DisplayName("Técnico crea protocolo con número ya existente y hay conflicto")
    void crearConNumeroDuplicadoLanzaConflicto() {
        ProtocoloConMedicionesDTO dto = crearDTO("P-001", 1L, 2L);
        when(repository.findByNumero("P-001")).thenReturn(Optional.of(new Protocolo()));
        assertThatThrownBy(() -> service.crear(dto, tecnico, autorizacion))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("Ya existe un protocolo con el número P-001");
    }

    @Test
    @DisplayName("Técnico crea protocolo con mediciones y queda registrado en terreno")
    void crearExitosoConMediciones() {
        ProtocoloConMedicionesDTO dto = crearDTO("P-001", 1L, 2L);
        dto.getMediciones().add(crearMedicionDTO("COD", "Nombre", "10", "mg", null));
        dto.getMediciones().add(crearMedicionDTO("COD2", "Nombre2", "20", "mg", " "));
        when(repository.findByNumero("P-001")).thenReturn(Optional.empty());
        mockInstalaciones(1L, 2L, 10L, 3L);
        when(repository.save(any(Protocolo.class))).thenAnswer(inv -> {
            Protocolo p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        ProtocoloConMedicionesDTO resultado = service.crear(dto, tecnico, autorizacion);

        assertThat(resultado.getProtocolo().getNumero()).isEqualTo("P-001");
        assertThat(resultado.getProtocolo().getIdCliente()).isEqualTo(3L);
        assertThat(resultado.getProtocolo().getEstado()).isEqualTo("REGISTRADO_TERRENO");
        assertThat(resultado.getMediciones()).hasSize(2);
        assertThat(resultado.getMediciones().get(0).getEstadoAlerta()).isEqualTo("OK");
        assertThat(resultado.getMediciones().get(1).getEstadoAlerta()).isEqualTo("OK");
    }

    @Test
    @DisplayName("Técnico crea protocolo sin mediciones y queda registrado")
    void crearExitosoSinMediciones() {
        ProtocoloConMedicionesDTO dto = crearDTO("P-001", 1L, 2L);
        dto.setMediciones(null);
        when(repository.findByNumero("P-001")).thenReturn(Optional.empty());
        mockInstalaciones(1L, 2L, 10L, 3L);
        when(repository.save(any(Protocolo.class))).thenAnswer(inv -> {
            Protocolo p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        ProtocoloConMedicionesDTO resultado = service.crear(dto, tecnico, autorizacion);

        assertThat(resultado.getMediciones()).isEmpty();
    }

    @Test
    @DisplayName("Técnico intenta registrar en visita ajena y se le deniega")
    void crearConVisitaNoAsignadaLanzaAccesoDenegado() {
        ProtocoloConMedicionesDTO dto = crearDTO("P-001", 1L, 2L);
        when(repository.findByNumero("P-001")).thenReturn(Optional.empty());
        VisitaRemotaDTO visita = new VisitaRemotaDTO();
        visita.setIdVisita(1L);
        visita.setIdInstalacion(10L);
        visita.setIdTecnico(99L);
        when(instalaciones.obtenerVisita(1L, autorizacion)).thenReturn(visita);

        assertThatThrownBy(() -> service.crear(dto, tecnico, autorizacion))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("La visita no está asignada a este técnico");
    }

    @Test
    @DisplayName("Equipo no pertenece a la instalación de la visita y se rechaza")
    void crearConEquipoDeOtraInstalacionLanzaSolicitudInvalida() {
        ProtocoloConMedicionesDTO dto = crearDTO("P-001", 1L, 2L);
        when(repository.findByNumero("P-001")).thenReturn(Optional.empty());
        VisitaRemotaDTO visita = new VisitaRemotaDTO();
        visita.setIdVisita(1L);
        visita.setIdInstalacion(10L);
        visita.setIdTecnico(1L);
        when(instalaciones.obtenerVisita(1L, autorizacion)).thenReturn(visita);
        EquipoRemotoDTO equipo = new EquipoRemotoDTO();
        equipo.setIdEquipo(2L);
        equipo.setIdInstalacion(20L);
        when(instalaciones.obtenerEquipo(2L, autorizacion)).thenReturn(equipo);

        assertThatThrownBy(() -> service.crear(dto, tecnico, autorizacion))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El equipo no pertenece a la instalación de la visita");
    }

    @Test
    @DisplayName("Cliente intenta sincronizar protocolos y se le deniega")
    void sincronizarConRolIncorrectoLanzaAccesoDenegado() {
        assertThatThrownBy(() -> service.sincronizar(List.of(), cliente, autorizacion))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("Esta acción es solo para el rol TECNICO");
    }

    @Test
    @DisplayName("Técnico sincroniza sin protocolos y no hay cambios")
    void sincronizarConListaVaciaRetornaCero() {
        SyncRespuestaDTO resultado = service.sincronizar(List.of(), tecnico, autorizacion);
        assertThat(resultado.getSincronizados()).isZero();
        assertThat(resultado.getAceptados()).isEmpty();
        assertThat(resultado.getRechazados()).isEmpty();
    }

    @Test
    @DisplayName("Sincronización con ítem nulo rechaza por datos incompletos")
    void sincronizarItemNuloEsRechazado() {
        List<ProtocoloConMedicionesDTO> lista = java.util.Collections.singletonList(null);
        SyncRespuestaDTO resultado = service.sincronizar(lista, tecnico, autorizacion);
        assertThat(resultado.getSincronizados()).isZero();
        assertThat(resultado.getRechazados()).containsExactly("Protocolo sin número: datos incompletos o inválidos");
    }

    @Test
    @DisplayName("Sincronización con protocolo nulo rechaza por datos incompletos")
    void sincronizarItemConProtocoloNuloEsRechazado() {
        ProtocoloConMedicionesDTO item = new ProtocoloConMedicionesDTO();
        item.setProtocolo(null);
        SyncRespuestaDTO resultado = service.sincronizar(List.of(item), tecnico, autorizacion);
        assertThat(resultado.getRechazados()).containsExactly("Protocolo sin número: datos incompletos o inválidos");
    }

    @Test
    @DisplayName("Protocolo sin número válido se rechaza en sincronización")
    void sincronizarItemConNumeroEnBlancoEsRechazado() {
        ProtocoloConMedicionesDTO item = new ProtocoloConMedicionesDTO();
        ProtocoloDTO p = new ProtocoloDTO();
        p.setNumero("   ");
        item.setProtocolo(p);
        ConstraintViolation<ProtocoloConMedicionesDTO> violation = mock(ConstraintViolation.class);
        when(validator.validate(item)).thenReturn(Set.of(violation));
        SyncRespuestaDTO resultado = service.sincronizar(List.of(item), tecnico, autorizacion);
        assertThat(resultado.getRechazados()).containsExactly("Protocolo sin número: datos incompletos o inválidos");
    }

    @Test
    @DisplayName("Protocolo con número nulo se rechaza en sincronización")
    void sincronizarItemConNumeroNuloEsRechazado() {
        ProtocoloConMedicionesDTO item = new ProtocoloConMedicionesDTO();
        ProtocoloDTO p = new ProtocoloDTO();
        p.setNumero(null);
        item.setProtocolo(p);
        ConstraintViolation<ProtocoloConMedicionesDTO> violation = mock(ConstraintViolation.class);
        when(validator.validate(item)).thenReturn(Set.of(violation));
        SyncRespuestaDTO resultado = service.sincronizar(List.of(item), tecnico, autorizacion);
        assertThat(resultado.getRechazados()).containsExactly("Protocolo sin número: datos incompletos o inválidos");
    }

    @Test
    @DisplayName("Protocolo inválido se rechaza en sincronización con su número")
    void sincronizarItemInvalidoPorValidatorEsRechazado() {
        ProtocoloConMedicionesDTO item = crearDTO("P-001", 1L, 2L);
        ConstraintViolation<ProtocoloConMedicionesDTO> violation = mock(ConstraintViolation.class);
        when(validator.validate(item)).thenReturn(Set.of(violation));
        SyncRespuestaDTO resultado = service.sincronizar(List.of(item), tecnico, autorizacion);
        assertThat(resultado.getRechazados()).containsExactly("P-001: datos incompletos o inválidos");
    }

    @Test
    @DisplayName("Técnico resincroniza su protocolo existente y se acepta")
    void sincronizarItemExistenteMismoTecnicoEsAceptado() {
        ProtocoloConMedicionesDTO item = crearDTO("P-001", 1L, 2L);
        when(validator.validate(item)).thenReturn(Collections.emptySet());
        Protocolo existente = new Protocolo();
        existente.setId(100L);
        existente.setNumero("P-001");
        existente.setIdTecnico(1L);
        when(repository.findByNumero("P-001")).thenReturn(Optional.of(existente));
        SyncRespuestaDTO resultado = service.sincronizar(List.of(item), tecnico, autorizacion);
        assertThat(resultado.getSincronizados()).isEqualTo(1);
        assertThat(resultado.getAceptados()).containsExactly("P-001");
        assertThat(resultado.getRechazados()).isEmpty();
    }

    @Test
    @DisplayName("Técnico intenta resincronizar protocolo ajeno y se rechaza")
    void sincronizarItemExistenteOtroTecnicoEsRechazado() {
        ProtocoloConMedicionesDTO item = crearDTO("P-001", 1L, 2L);
        when(validator.validate(item)).thenReturn(Collections.emptySet());
        Protocolo existente = new Protocolo();
        existente.setId(100L);
        existente.setNumero("P-001");
        existente.setIdTecnico(99L);
        when(repository.findByNumero("P-001")).thenReturn(Optional.of(existente));
        SyncRespuestaDTO resultado = service.sincronizar(List.of(item), tecnico, autorizacion);
        assertThat(resultado.getSincronizados()).isZero();
        assertThat(resultado.getRechazados()).containsExactly("P-001: el número ya pertenece a otro técnico");
    }

    @Test
    @DisplayName("Técnico sincroniza protocolo nuevo y se acepta")
    void sincronizarItemNuevoExitosoEsAceptado() {
        ProtocoloConMedicionesDTO item = crearDTO("P-001", 1L, 2L);
        when(validator.validate(item)).thenReturn(Collections.emptySet());
        when(repository.findByNumero("P-001")).thenReturn(Optional.empty());
        mockInstalaciones(1L, 2L, 10L, 3L);
        when(repository.save(any(Protocolo.class))).thenAnswer(inv -> {
            Protocolo p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });
        SyncRespuestaDTO resultado = service.sincronizar(List.of(item), tecnico, autorizacion);
        assertThat(resultado.getSincronizados()).isEqualTo(1);
        assertThat(resultado.getAceptados()).containsExactly("P-001");
    }

    @Test
    @DisplayName("Protocolo nuevo con visita inexistente se rechaza en sincronización")
    void sincronizarItemNuevoConSolicitudInvalidaEsRechazado() {
        ProtocoloConMedicionesDTO item = crearDTO("P-001", 1L, 2L);
        when(validator.validate(item)).thenReturn(Collections.emptySet());
        when(repository.findByNumero("P-001")).thenReturn(Optional.empty());
        when(instalaciones.obtenerVisita(1L, autorizacion))
                .thenThrow(new SolicitudInvalidaException("La visita 1 no existe"));
        SyncRespuestaDTO resultado = service.sincronizar(List.of(item), tecnico, autorizacion);
        assertThat(resultado.getSincronizados()).isZero();
        assertThat(resultado.getRechazados()).containsExactly("P-001: La visita 1 no existe");
    }

    @Test
    @DisplayName("Protocolo nuevo en visita ajena se rechaza en sincronización")
    void sincronizarItemNuevoConAccesoDenegadoEsRechazado() {
        ProtocoloConMedicionesDTO item = crearDTO("P-001", 1L, 2L);
        when(validator.validate(item)).thenReturn(Collections.emptySet());
        when(repository.findByNumero("P-001")).thenReturn(Optional.empty());
        VisitaRemotaDTO visita = new VisitaRemotaDTO();
        visita.setIdVisita(1L);
        visita.setIdInstalacion(10L);
        visita.setIdTecnico(99L);
        when(instalaciones.obtenerVisita(1L, autorizacion)).thenReturn(visita);
        SyncRespuestaDTO resultado = service.sincronizar(List.of(item), tecnico, autorizacion);
        assertThat(resultado.getSincronizados()).isZero();
        assertThat(resultado.getRechazados()).containsExactly("P-001: La visita no está asignada a este técnico");
    }

    @Test
    @DisplayName("Protocolo inexistente no se encuentra al consultarlo")
    void obtenerNoEncontradoLanzaRecursoNoEncontrado() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtener(1L, tecnico))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Protocolo no encontrado");
    }

    @Test
    @DisplayName("Cliente consulta un protocolo propio y lo ve")
    void obtenerComoClienteCorrectoRetornaProtocolo() {
        Protocolo p = crearProtocolo(1L, 3L, 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        ProtocoloConMedicionesDTO resultado = service.obtener(1L, cliente);
        assertThat(resultado.getProtocolo().getNumero()).isEqualTo("P-001");
    }

    @Test
    @DisplayName("Técnico consulta un protocolo propio y lo ve")
    void obtenerComoTecnicoCorrectoRetornaProtocolo() {
        Protocolo p = crearProtocolo(1L, 3L, 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        ProtocoloConMedicionesDTO resultado = service.obtener(1L, tecnico);
        assertThat(resultado.getProtocolo().getNumero()).isEqualTo("P-001");
    }

    @Test
    @DisplayName("Cliente consulta protocolo ajeno y se le deniega")
    void obtenerComoClienteIncorrectoLanzaAccesoDenegado() {
        Protocolo p = crearProtocolo(1L, 99L, 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.obtener(1L, cliente))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("No tienes acceso a este protocolo");
    }

    @Test
    @DisplayName("Técnico consulta protocolo ajeno y se le deniega")
    void obtenerComoTecnicoIncorrectoLanzaAccesoDenegado() {
        Protocolo p = crearProtocolo(1L, 3L, 99L);
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.obtener(1L, tecnico))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("No tienes acceso a este protocolo");
    }

    @Test
    @DisplayName("Supervisor intenta consultar protocolo en vista de técnico y se le deniega")
    void obtenerComoSupersivorLanzaAccesoDenegado() {
        Protocolo p = crearProtocolo(1L, 3L, 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.obtener(1L, supervisor))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("No tienes acceso a este protocolo");
    }

    @Test
    @DisplayName("Cliente consulta sus protocolos y recibe su lista")
    void protocolosDeClienteCorrectoRetornaLista() {
        Protocolo p = crearProtocolo(1L, 3L, 1L);
        when(repository.findByIdClienteOrderByIdDesc(3L)).thenReturn(List.of(p));
        List<ProtocoloDTO> resultado = service.protocolosDeCliente(3L, cliente);
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNumero()).isEqualTo("P-001");
    }

    @Test
    @DisplayName("Técnico intenta consultar protocolos de cliente y se le deniega")
    void protocolosDeClienteConRolIncorrectoLanzaAccesoDenegado() {
        assertThatThrownBy(() -> service.protocolosDeCliente(3L, tecnico))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("Solo puedes consultar tu propia información");
    }

    @Test
    @DisplayName("Cliente intenta ver protocolos de otro cliente y se le deniega")
    void protocolosDeClienteConIdDiferenteLanzaAccesoDenegado() {
        assertThatThrownBy(() -> service.protocolosDeCliente(99L, cliente))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("Solo puedes consultar tu propia información");
    }

    @Test
    @DisplayName("Técnico consulta sus protocolos y recibe su lista")
    void protocolosDeTecnicoCorrectoRetornaLista() {
        Protocolo p = crearProtocolo(1L, 3L, 1L);
        when(repository.findByIdTecnicoOrderByIdDesc(1L)).thenReturn(List.of(p));
        List<ProtocoloDTO> resultado = service.protocolosDeTecnico(1L, tecnico);
        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("Cliente intenta consultar protocolos de técnico y se le deniega")
    void protocolosDeTecnicoConRolIncorrectoLanzaAccesoDenegado() {
        assertThatThrownBy(() -> service.protocolosDeTecnico(1L, cliente))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Supervisor obtiene protocolos pendientes de revisión")
    void pendientesDeRevisionCorrectoRetornaLista() {
        Protocolo p = crearProtocolo(1L, 3L, 1L);
        p.setEstado("REGISTRADO_TERRENO");
        when(repository.findByEstadoOrderByIdDesc("REGISTRADO_TERRENO")).thenReturn(List.of(p));
        List<ProtocoloConMedicionesDTO> resultado = service.pendientesDeRevision(supervisor);
        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("Técnico intenta ver pendientes de revisión y se le deniega")
    void pendientesDeRevisionConRolIncorrectoLanzaAccesoDenegado() {
        assertThatThrownBy(() -> service.pendientesDeRevision(tecnico))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("Esta acción es solo para el rol SUPERVISOR");
    }

    @Test
    @DisplayName("Técnico intenta aprobar o rechazar protocolo y se le deniega")
    void cambiarEstadoConRolIncorrectoLanzaAccesoDenegado() {
        assertThatThrownBy(() -> service.cambiarEstado(1L, true, tecnico))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("Supervisor revisa protocolo inexistente y no se encuentra")
    void cambiarEstadoNoEncontradoLanzaRecursoNoEncontrado() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cambiarEstado(1L, true, supervisor))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Protocolo no encontrado");
    }

    @Test
    @DisplayName("Supervisor revisa protocolo ya revisado y hay conflicto")
    void cambiarEstadoYaRevisadoLanzaConflicto() {
        Protocolo p = crearProtocolo(1L, 3L, 1L);
        p.setEstado("FORMALIZADO");
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.cambiarEstado(1L, true, supervisor))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("El protocolo ya fue revisado");
    }

    @Test
    @DisplayName("Supervisor aprueba protocolo y queda formalizado")
    void cambiarEstadoAprobarRetornaFormalizado() {
        Protocolo p = crearProtocolo(1L, 3L, 1L);
        p.setEstado("REGISTRADO_TERRENO");
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        when(repository.save(any(Protocolo.class))).thenAnswer(inv -> inv.getArgument(0));
        ProtocoloDTO resultado = service.cambiarEstado(1L, true, supervisor);
        assertThat(resultado.getEstado()).isEqualTo("FORMALIZADO");
    }

    @Test
    @DisplayName("Supervisor rechaza protocolo y queda anulado")
    void cambiarEstadoRechazarRetornaAnulado() {
        Protocolo p = crearProtocolo(1L, 3L, 1L);
        p.setEstado("REGISTRADO_TERRENO");
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        when(repository.save(any(Protocolo.class))).thenAnswer(inv -> inv.getArgument(0));
        ProtocoloDTO resultado = service.cambiarEstado(1L, false, supervisor);
        assertThat(resultado.getEstado()).isEqualTo("ANULADO");
    }

    private ProtocoloConMedicionesDTO crearDTO(String numero, Long idVisita, Long idEquipo) {
        ProtocoloConMedicionesDTO dto = new ProtocoloConMedicionesDTO();
        ProtocoloDTO p = new ProtocoloDTO();
        p.setNumero(numero);
        p.setIdVisita(idVisita);
        p.setIdEquipo(idEquipo);
        p.setFechaHoraPrueba("2026-01-01T10:00:00");
        p.setResultado("APROBADO");
        dto.setProtocolo(p);
        return dto;
    }

    private MedicionDTO crearMedicionDTO(String codigo, String nombre, String valor, String unidad, String estadoAlerta) {
        MedicionDTO m = new MedicionDTO();
        m.setParametroCodigo(codigo);
        m.setParametroNombre(nombre);
        m.setValor(valor);
        m.setUnidad(unidad);
        m.setEstadoAlerta(estadoAlerta);
        return m;
    }

    private void mockInstalaciones(Long idVisita, Long idEquipo, Long idInstalacion, Long idCliente) {
        VisitaRemotaDTO visita = new VisitaRemotaDTO();
        visita.setIdVisita(idVisita);
        visita.setIdInstalacion(idInstalacion);
        visita.setIdTecnico(1L);
        when(instalaciones.obtenerVisita(idVisita, autorizacion)).thenReturn(visita);
        EquipoRemotoDTO equipo = new EquipoRemotoDTO();
        equipo.setIdEquipo(idEquipo);
        equipo.setIdInstalacion(idInstalacion);
        when(instalaciones.obtenerEquipo(idEquipo, autorizacion)).thenReturn(equipo);
        InstalacionRemotaDTO instalacion = new InstalacionRemotaDTO();
        instalacion.setIdInstalacion(idInstalacion);
        instalacion.setIdUsuarioCliente(idCliente);
        when(instalaciones.obtenerInstalacion(idInstalacion, autorizacion)).thenReturn(instalacion);
    }

    private Protocolo crearProtocolo(Long id, Long idCliente, Long idTecnico) {
        Protocolo p = new Protocolo();
        p.setId(id);
        p.setNumero("P-001");
        p.setIdVisita(1L);
        p.setIdEquipo(2L);
        p.setIdInstalacion(10L);
        p.setIdCliente(idCliente);
        p.setIdTecnico(idTecnico);
        p.setFechaHoraPrueba("2026-01-01T10:00:00");
        p.setEstado("REGISTRADO_TERRENO");
        p.setResultado("APROBADO");
        Medicion m = new Medicion();
        m.setId(1L);
        m.setParametroCodigo("COD");
        m.setParametroNombre("Nombre");
        m.setValor("10");
        m.setUnidad("mg");
        m.setEstadoAlerta("OK");
        p.agregarMedicion(m);
        return p;
    }
}
