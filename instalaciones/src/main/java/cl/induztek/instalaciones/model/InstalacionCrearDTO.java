package cl.induztek.instalaciones.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InstalacionCrearDTO {

    @NotNull(message = "La empresa es obligatoria")
    private Long idCliente;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 30)
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150)
    private String nombre;

    @NotBlank(message = "La ubicación es obligatoria")
    @Size(max = 255)
    private String ubicacion;

    @Size(max = 255)
    private String observacionesAcceso;
}
