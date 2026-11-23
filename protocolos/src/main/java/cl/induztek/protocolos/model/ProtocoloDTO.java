package cl.induztek.protocolos.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProtocoloDTO {

    private Long idProtocolo;

    @NotBlank(message = "El número de protocolo es obligatorio")
    @Size(max = 30)
    private String numero;

    @NotNull(message = "La visita es obligatoria")
    private Long idVisita;

    @NotNull(message = "El equipo es obligatorio")
    private Long idEquipo;

    private Long idUsuarioRegistra;

    private Long idInstalacion;

    private Long idCliente;

    @NotBlank(message = "La fecha de la prueba es obligatoria")
    @Size(max = 40)
    private String fechaHoraPrueba;

    private Double temperaturaC;

    private Double humedadPct;

    @Size(max = 1000)
    private String descripcionMantenimiento;

    @Size(max = 1000)
    private String observaciones;

    @Size(max = 30)
    private String estado;

    @NotBlank(message = "El resultado es obligatorio")
    @Size(max = 30)
    private String resultado;

    private Boolean sincronizado;

    @Size(max = 255)
    private String fotoPath;
}
