package cl.induztek.autenticacion.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRespuestaDTO {

    private String token;
    private String rol;
    private UsuarioMostrarDTO usuario;
}
