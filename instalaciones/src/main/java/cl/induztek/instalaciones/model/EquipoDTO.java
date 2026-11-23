package cl.induztek.instalaciones.model;

import lombok.Data;

@Data
public class EquipoDTO {

    private Long idEquipo;
    private Long idInstalacion;
    private String tag;
    private Long idTipoEquipo;
    private String tipoEquipo;
    private String marca;
    private String modelo;
    private String numeroSerie;
    private Integer anioFabricacion;
    private Double potenciaKva;
    private Double tensionPrimariaKv;
    private Double tensionSecundariaKv;
    private Double corrienteNominalA;
    private String observaciones;
}
