package cl.induztek.instalaciones.model;

import lombok.Data;

@Data
public class AsignacionDTO {

    private Long idAsignacion;

    private Long idCliente;

    private String nombreCliente;

    private Long idTecnico;

    private String nombreTecnico;

    private String fechaAsignacion;
}
