package cl.induztek.protocolos.controller;

import cl.induztek.protocolos.model.ProtocoloConMedicionesDTO;
import cl.induztek.protocolos.model.ProtocoloDTO;
import cl.induztek.protocolos.model.SyncRespuestaDTO;
import cl.induztek.protocolos.security.JwtFiltro;
import cl.induztek.protocolos.security.UsuarioToken;
import cl.induztek.protocolos.service.ProtocoloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Protocolos", description = "Protocolos de prueba, mediciones y sincronización")
public class ProtocoloController {

    private final ProtocoloService service;

    public ProtocoloController(ProtocoloService service) {
        this.service = service;
    }

    @Operation(summary = "Crear protocolo", description = "Registra un protocolo con sus mediciones y devuelve el protocolo creado.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Protocolo creado"),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos"),
        @ApiResponse(responseCode = "409", description = "Conflicto, número de protocolo duplicado"),
        @ApiResponse(responseCode = "503", description = "Servicio remoto no disponible")
    })
    @PostMapping("/protocolos")
    public ResponseEntity<ProtocoloConMedicionesDTO> crear(
            @Valid @RequestBody ProtocoloConMedicionesDTO dto,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario,
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto, usuario, autorizacion));
    }

    @Operation(summary = "Sincronizar protocolos", description = "Recibe los protocolos registrados sin conexión en la app. Los que ya existen se cuentan como sincronizados y los inválidos se informan en rechazados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sincronización procesada"),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos"),
        @ApiResponse(responseCode = "503", description = "Servicio remoto no disponible")
    })
    @PostMapping("/protocolos/sincronizar")
    public ResponseEntity<SyncRespuestaDTO> sincronizar(
            @RequestBody List<ProtocoloConMedicionesDTO> lista,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario,
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return ResponseEntity.ok(service.sincronizar(lista, usuario, autorizacion));
    }

    @Operation(summary = "Obtener protocolo", description = "Devuelve un protocolo con sus mediciones por identificador.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Protocolo encontrado"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos"),
        @ApiResponse(responseCode = "404", description = "Protocolo no encontrado")
    })
    @GetMapping("/protocolos/{id}")
    public ResponseEntity<ProtocoloConMedicionesDTO> obtener(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.obtener(id, usuario));
    }

    @Operation(summary = "Protocolos por cliente", description = "Lista los protocolos asociados a un cliente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @GetMapping("/clientes/{id}/protocolos")
    public ResponseEntity<List<ProtocoloDTO>> protocolosDeCliente(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.protocolosDeCliente(id, usuario));
    }

    @Operation(summary = "Protocolos por técnico", description = "Lista los protocolos asociados a un técnico.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @GetMapping("/tecnicos/{id}/protocolos")
    public ResponseEntity<List<ProtocoloDTO>> protocolosDeTecnico(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.protocolosDeTecnico(id, usuario));
    }

    @Operation(summary = "Protocolos pendientes de revisión", description = "El supervisor ve los protocolos enviados desde terreno que aún no revisa.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Solo para supervisor")
    })
    @GetMapping("/protocolos/pendientes")
    public ResponseEntity<List<ProtocoloConMedicionesDTO>> pendientes(
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.pendientesDeRevision(usuario));
    }

    @Operation(summary = "Aprobar protocolo", description = "El supervisor formaliza el protocolo y queda visible como oficial para el cliente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Protocolo aprobado"),
        @ApiResponse(responseCode = "403", description = "Solo para supervisor"),
        @ApiResponse(responseCode = "404", description = "Protocolo no encontrado"),
        @ApiResponse(responseCode = "409", description = "Ya revisado")
    })
    @PutMapping("/protocolos/{id}/aprobar")
    public ResponseEntity<ProtocoloDTO> aprobar(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.cambiarEstado(id, true, usuario));
    }

    @Operation(summary = "Rechazar protocolo", description = "El supervisor anula el protocolo y deja de mostrarse al cliente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Protocolo rechazado"),
        @ApiResponse(responseCode = "403", description = "Solo para supervisor"),
        @ApiResponse(responseCode = "404", description = "Protocolo no encontrado"),
        @ApiResponse(responseCode = "409", description = "Ya revisado")
    })
    @PutMapping("/protocolos/{id}/rechazar")
    public ResponseEntity<ProtocoloDTO> rechazar(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.cambiarEstado(id, false, usuario));
    }
}
