package cl.induztek.protocolos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import cl.induztek.protocolos.model.EquipoRemotoDTO;
import cl.induztek.protocolos.model.InstalacionRemotaDTO;
import cl.induztek.protocolos.model.VisitaRemotaDTO;

@FeignClient(name = "instalaciones")
public interface InstalacionesApi {

    @GetMapping("/visitas/{id}")
    VisitaRemotaDTO obtenerVisita(@PathVariable("id") Long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion);

    @GetMapping("/equipos/{id}")
    EquipoRemotoDTO obtenerEquipo(@PathVariable("id") Long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion);

    @GetMapping("/instalaciones/{id}")
    InstalacionRemotaDTO obtenerInstalacion(@PathVariable("id") Long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion);
}
