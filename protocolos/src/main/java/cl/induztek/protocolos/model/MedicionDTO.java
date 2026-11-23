package cl.induztek.protocolos.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MedicionDTO {

    private Long idMedicion;

    private Long idProtocolo;

    @NotBlank
    @Size(max = 30)
    private String parametroCodigo;

    @NotBlank
    @Size(max = 100)
    private String parametroNombre;

    @NotBlank
    @Size(max = 30)
    private String valor;

    @NotBlank
    @Size(max = 20)
    private String unidad;

    @Size(max = 20)
    private String estadoAlerta;
}
