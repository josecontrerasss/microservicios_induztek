package cl.induztek.autenticacion.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CambiarClaveDTO {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String claveActual;

    @NotBlank(message = "La contraseña nueva es obligatoria")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,}$", message = "La contraseña debe tener mínimo 8 caracteres, una mayúscula, un número y un carácter especial")
    private String claveNueva;
}
