package cl.induztek.protocolos.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import cl.induztek.protocolos.exception.AccesoDenegadoException;
import cl.induztek.protocolos.exception.ServicioNoDisponibleException;
import cl.induztek.protocolos.exception.SolicitudInvalidaException;
import cl.induztek.protocolos.model.EquipoRemotoDTO;
import cl.induztek.protocolos.model.InstalacionRemotaDTO;
import cl.induztek.protocolos.model.VisitaRemotaDTO;

@Component
public class InstalacionesCliente {
    private final RestClient restClient;

    public InstalacionesCliente(@Value("${servicios.instalaciones.url}") String url) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }

    public VisitaRemotaDTO obtenerVisita(Long id, String autorizacion) {
        return obtener("/visitas/{id}", id, autorizacion, VisitaRemotaDTO.class, "La visita " + id + " no existe");
    }

    public EquipoRemotoDTO obtenerEquipo(Long id, String autorizacion) {
        return obtener("/equipos/{id}", id, autorizacion, EquipoRemotoDTO.class, "El equipo " + id + " no existe");
    }

    public InstalacionRemotaDTO obtenerInstalacion(Long id, String autorizacion) {
        return obtener("/instalaciones/{id}", id, autorizacion, InstalacionRemotaDTO.class, "La instalación " + id + " no existe");
    }

    private <T> T obtener(String ruta, Long id, String autorizacion, Class<T> tipo, String mensajeNoExiste) {
        try {
            return restClient.get().uri(ruta, id).header(HttpHeaders.AUTHORIZATION, autorizacion).retrieve().body(tipo);
        } catch (HttpClientErrorException.NotFound e) {
            throw new SolicitudInvalidaException(mensajeNoExiste);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new AccesoDenegadoException("No estás asignado a esa visita o instalación");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("El servicio de instalaciones no está disponible");
        }
    }
}
