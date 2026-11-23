package cl.induztek.instalaciones.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AsignacionCrearDTO {

    @NotNull(message = "La empresa es obligatoria")
    private Long idCliente;

    @NotBlank
    @Size(max = 150)
    private String nombreCliente;

    @NotNull(message = "El técnico es obligatorio")
    private Long idTecnico;

    @NotBlank
    @Size(max = 150)
    private String nombreTecnico;
}
