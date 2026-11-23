package cl.induztek.instalaciones.model;

import lombok.Data;

@Data
public class VisitaDTO {

    private Long idVisita;
    private String numeroOt;
    private Long idInstalacion;
    private String fecha;
    private String tipoVisita;
    private Long idTecnico;
}
