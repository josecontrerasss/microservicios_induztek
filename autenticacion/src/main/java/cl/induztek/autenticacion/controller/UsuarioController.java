package cl.induztek.autenticacion.controller;

import cl.induztek.autenticacion.exception.AccesoDenegadoException;
import cl.induztek.autenticacion.model.CambiarClaveDTO;
import cl.induztek.autenticacion.model.UsuarioActualizarDTO;
import cl.induztek.autenticacion.model.UsuarioMostrarDTO;
import cl.induztek.autenticacion.security.JwtFiltro;
import cl.induztek.autenticacion.security.UsuarioToken;
import cl.induztek.autenticacion.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuarios", description = "Perfil y cambio de clave del usuario autenticado")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Obtener usuario", description = "Obtiene el perfil del usuario autenticado por su id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado para acceder a este perfil"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioMostrarDTO> obtener(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuarioToken) {
        verificarPropietario(id, usuarioToken);
        return ResponseEntity.ok(usuarioService.obtenerUsuario(id));
    }

    @Operation(summary = "Actualizar perfil", description = "Actualiza los datos del perfil del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado para acceder a este perfil"),
            @ApiResponse(responseCode = "409", description = "El correo ya está en uso")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioMostrarDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioActualizarDTO dto,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuarioToken) {
        verificarPropietario(id, usuarioToken);
        return ResponseEntity.ok(usuarioService.actualizarPerfil(id, dto));
    }

    @Operation(summary = "Cambiar contraseña", description = "Cambia la contraseña del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contraseña cambiada"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado para acceder a este perfil")
    })
    @PutMapping("/{id}/clave")
    public ResponseEntity<Void> cambiarClave(
            @PathVariable Long id,
            @Valid @RequestBody CambiarClaveDTO dto,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuarioToken) {
        verificarPropietario(id, usuarioToken);
        usuarioService.cambiarClave(id, dto);
        return ResponseEntity.noContent().build();
    }

    private void verificarPropietario(Long id, UsuarioToken usuarioToken) {
        if (!usuarioToken.idUsuario().equals(id)) {
            throw new AccesoDenegadoException("Solo puedes acceder a tu propio perfil");
        }
    }
}
