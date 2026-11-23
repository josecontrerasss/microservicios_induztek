package cl.induztek.autenticacion.controller;

import cl.induztek.autenticacion.model.AuthRespuestaDTO;
import cl.induztek.autenticacion.model.LoginDTO;
import cl.induztek.autenticacion.model.RecuperarClaveDTO;
import cl.induztek.autenticacion.model.RestablecerClaveDTO;
import cl.induztek.autenticacion.model.UsuarioMostrarDTO;
import cl.induztek.autenticacion.model.UsuarioRegistroDTO;
import cl.induztek.autenticacion.service.RecuperacionClaveService;
import cl.induztek.autenticacion.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Registro, inicio de sesión y recuperación de clave")
public class AuthController {

    private final UsuarioService usuarioService;
    private final RecuperacionClaveService recuperacionService;

    public AuthController(UsuarioService usuarioService, RecuperacionClaveService recuperacionService) {
        this.usuarioService = usuarioService;
        this.recuperacionService = recuperacionService;
    }

    @Operation(summary = "Registrar cliente", description = "Registra un nuevo usuario cliente en el sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "El correo o RUT ya está registrado")
    })
    @PostMapping("/registro")
    public ResponseEntity<UsuarioMostrarDTO> registro(@Valid @RequestBody UsuarioRegistroDTO dto) {
        return ResponseEntity.status(201).body(usuarioService.registrarCliente(dto));
    }

    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario y devuelve un token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthRespuestaDTO> login(@Valid @RequestBody LoginDTO dto) {
        return ResponseEntity.ok(usuarioService.iniciarSesion(dto));
    }

    @Operation(summary = "Recuperar contraseña", description = "Envía un código de 6 dígitos al correo si está registrado. Siempre responde 202 para no revelar qué correos existen.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Solicitud aceptada"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    @PostMapping("/recuperar-clave")
    public ResponseEntity<Void> recuperarClave(@Valid @RequestBody RecuperarClaveDTO dto) {
        recuperacionService.solicitarCodigo(dto);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Restablecer contraseña", description = "Restablece la contraseña usando el código de recuperación")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contraseña restablecida"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    @PostMapping("/restablecer-clave")
    public ResponseEntity<Void> restablecerClave(@Valid @RequestBody RestablecerClaveDTO dto) {
        recuperacionService.restablecerClave(dto);
        return ResponseEntity.noContent().build();
    }
}
