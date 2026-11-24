package cl.induztek.protocolos.client;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import cl.induztek.protocolos.exception.AccesoDenegadoException;
import cl.induztek.protocolos.exception.ServicioNoDisponibleException;
import cl.induztek.protocolos.exception.SolicitudInvalidaException;
import cl.induztek.protocolos.model.EquipoRemotoDTO;
import cl.induztek.protocolos.model.InstalacionRemotaDTO;
import cl.induztek.protocolos.model.VisitaRemotaDTO;
import feign.FeignException;

@Component
public class InstalacionesCliente {
    private final InstalacionesApi api;

    public InstalacionesCliente(InstalacionesApi api) {
        this.api = api;
    }

    public VisitaRemotaDTO obtenerVisita(Long id, String autorizacion) {
        return obtener(() -> api.obtenerVisita(id, autorizacion), "La visita " + id + " no existe");
    }

    public EquipoRemotoDTO obtenerEquipo(Long id, String autorizacion) {
        return obtener(() -> api.obtenerEquipo(id, autorizacion), "El equipo " + id + " no existe");
    }

    public InstalacionRemotaDTO obtenerInstalacion(Long id, String autorizacion) {
        return obtener(() -> api.obtenerInstalacion(id, autorizacion), "La instalación " + id + " no existe");
    }

    private <T> T obtener(Supplier<T> llamada, String mensajeNoExiste) {
        try {
            return llamada.get();
        } catch (FeignException.NotFound e) {
            throw new SolicitudInvalidaException(mensajeNoExiste);
        } catch (FeignException.Forbidden e) {
            throw new AccesoDenegadoException("No estás asignado a esa visita o instalación");
        } catch (FeignException e) {
            throw new ServicioNoDisponibleException("El servicio de instalaciones no está disponible");
        }
    }
}
