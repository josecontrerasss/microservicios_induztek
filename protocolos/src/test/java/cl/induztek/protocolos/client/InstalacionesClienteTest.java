package cl.induztek.protocolos.client;

import cl.induztek.protocolos.exception.AccesoDenegadoException;
import cl.induztek.protocolos.exception.ServicioNoDisponibleException;
import cl.induztek.protocolos.exception.SolicitudInvalidaException;
import cl.induztek.protocolos.model.EquipoRemotoDTO;
import cl.induztek.protocolos.model.InstalacionRemotaDTO;
import cl.induztek.protocolos.model.VisitaRemotaDTO;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstalacionesClienteTest {

    @Mock
    private InstalacionesApi api;

    private InstalacionesCliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new InstalacionesCliente(api);
    }

    @Test
    @DisplayName("Técnico consulta una visita de terreno y recibe sus datos")
    void obtenerVisitaRetornaVisita() {
        VisitaRemotaDTO visita = new VisitaRemotaDTO();
        visita.setIdVisita(1L);
        when(api.obtenerVisita(1L, "Bearer token")).thenReturn(visita);
        VisitaRemotaDTO resultado = cliente.obtenerVisita(1L, "Bearer token");
        assertThat(resultado).isSameAs(visita);
    }

    @Test
    @DisplayName("Técnico pide una visita inexistente y se le avisa que no existe")
    void obtenerVisitaNoEncontradaLanzaSolicitudInvalida() {
        Request req = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, null);
        when(api.obtenerVisita(1L, "Bearer token"))
                .thenThrow(new FeignException.NotFound("no existe", req, null, null));
        assertThatThrownBy(() -> cliente.obtenerVisita(1L, "Bearer token"))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("La visita 1 no existe");
    }

    @Test
    @DisplayName("Técnico intenta ver una visita ajena y se le deniega el acceso")
    void obtenerVisitaProhibidaLanzaAccesoDenegado() {
        Request req = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, null);
        when(api.obtenerVisita(1L, "Bearer token"))
                .thenThrow(new FeignException.Forbidden("prohibido", req, null, null));
        assertThatThrownBy(() -> cliente.obtenerVisita(1L, "Bearer token"))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("No estás asignado a esa visita o instalación");
    }

    @Test
    @DisplayName("Si el servicio de instalaciones está caído, se avisa que no está disponible")
    void obtenerVisitaServicioCaidoLanzaServicioNoDisponible() {
        Request req = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, null);
        when(api.obtenerVisita(1L, "Bearer token"))
                .thenThrow(new FeignException.ServiceUnavailable("caido", req, null, null));
        assertThatThrownBy(() -> cliente.obtenerVisita(1L, "Bearer token"))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage("El servicio de instalaciones no está disponible");
    }

    @Test
    @DisplayName("Técnico consulta un equipo en terreno y recibe sus datos")
    void obtenerEquipoRetornaEquipo() {
        EquipoRemotoDTO equipo = new EquipoRemotoDTO();
        equipo.setIdEquipo(2L);
        when(api.obtenerEquipo(2L, "Bearer token")).thenReturn(equipo);
        EquipoRemotoDTO resultado = cliente.obtenerEquipo(2L, "Bearer token");
        assertThat(resultado).isSameAs(equipo);
    }

    @Test
    @DisplayName("Técnico pide un equipo inexistente y se le avisa que no existe")
    void obtenerEquipoNoEncontradoLanzaSolicitudInvalida() {
        Request req = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, null);
        when(api.obtenerEquipo(2L, "Bearer token"))
                .thenThrow(new FeignException.NotFound("no existe", req, null, null));
        assertThatThrownBy(() -> cliente.obtenerEquipo(2L, "Bearer token"))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El equipo 2 no existe");
    }

    @Test
    @DisplayName("Técnico consulta la instalación asociada y recibe sus datos")
    void obtenerInstalacionRetornaInstalacion() {
        InstalacionRemotaDTO instalacion = new InstalacionRemotaDTO();
        instalacion.setIdInstalacion(3L);
        when(api.obtenerInstalacion(3L, "Bearer token")).thenReturn(instalacion);
        InstalacionRemotaDTO resultado = cliente.obtenerInstalacion(3L, "Bearer token");
        assertThat(resultado).isSameAs(instalacion);
    }

    @Test
    @DisplayName("Técnico pide una instalación inexistente y se le avisa que no existe")
    void obtenerInstalacionNoEncontradaLanzaSolicitudInvalida() {
        Request req = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, null);
        when(api.obtenerInstalacion(3L, "Bearer token"))
                .thenThrow(new FeignException.NotFound("no existe", req, null, null));
        assertThatThrownBy(() -> cliente.obtenerInstalacion(3L, "Bearer token"))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("La instalación 3 no existe");
    }

    @Test
    @DisplayName("Si el servicio falla al consultar un equipo, se avisa que no está disponible")
    void obtenerEquipoServicioCaidoLanzaServicioNoDisponible() {
        Request req = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, null);
        when(api.obtenerEquipo(2L, "Bearer token"))
                .thenThrow(new FeignException.InternalServerError("error", req, null, null));
        assertThatThrownBy(() -> cliente.obtenerEquipo(2L, "Bearer token"))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage("El servicio de instalaciones no está disponible");
    }
}
