package cl.induztek.autenticacion.model;

import lombok.Data;

@Data
public class UsuarioMostrarDTO {

    private Long idUsuario;
    private Integer idRol;
    private String rut;
    private String nombre;
    private String correo;
    private String telefono;
    private String rol;
    private Boolean activo;
    private String giro;
    private String contactoNombre;
    private String contactoCargo;
}
