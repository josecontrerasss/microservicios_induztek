package cl.induztek.instalaciones.model;

import lombok.Data;

@Data
public class InstalacionDTO {

    private Long idInstalacion;
    private Long idUsuarioCliente;
    private String codigo;
    private String nombre;
    private String ubicacion;
    private String observacionesAcceso;
    private String ultimaActividad;
}
