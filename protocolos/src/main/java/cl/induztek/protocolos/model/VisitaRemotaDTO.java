package cl.induztek.protocolos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VisitaRemotaDTO {

    private Long idVisita;

    private Long idInstalacion;

    private Long idTecnico;
}
