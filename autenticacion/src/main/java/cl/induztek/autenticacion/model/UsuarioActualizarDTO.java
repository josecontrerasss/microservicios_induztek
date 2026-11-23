package cl.induztek.autenticacion.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsuarioActualizarDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no debe superar 150 caracteres")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe ser válido")
    @Size(max = 255, message = "El correo no debe superar 255 caracteres")
    private String correo;

    @Size(max = 20, message = "El teléfono no debe superar 20 caracteres")
    private String telefono;

    @Size(max = 150, message = "El giro no debe superar 150 caracteres")
    private String giro;

    @Size(max = 150, message = "El nombre de contacto no debe superar 150 caracteres")
    private String contactoNombre;

    @Size(max = 150, message = "El cargo de contacto no debe superar 150 caracteres")
    private String contactoCargo;
}
