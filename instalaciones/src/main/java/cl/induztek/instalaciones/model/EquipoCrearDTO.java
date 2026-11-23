package cl.induztek.instalaciones.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EquipoCrearDTO {

    @NotBlank(message = "El TAG es obligatorio")
    @Size(max = 100)
    private String tag;

    @NotNull(message = "El tipo de equipo es obligatorio")
    private Long idTipoEquipo;

    @NotBlank
    @Size(max = 100)
    private String marca;

    @NotBlank
    @Size(max = 100)
    private String modelo;

    @NotBlank(message = "El número de serie es obligatorio")
    @Size(max = 100)
    private String numeroSerie;

    @Min(value = 1950, message = "Año inválido")
    @Max(value = 2100, message = "Año inválido")
    private Integer anioFabricacion;

    @PositiveOrZero(message = "No puede ser negativo")
    private Double potenciaKva;

    @PositiveOrZero(message = "No puede ser negativo")
    private Double tensionPrimariaKv;

    @PositiveOrZero(message = "No puede ser negativo")
    private Double tensionSecundariaKv;

    @PositiveOrZero(message = "No puede ser negativo")
    private Double corrienteNominalA;

    @Size(max = 255)
    private String observaciones;
}
