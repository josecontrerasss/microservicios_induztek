package cl.induztek.instalaciones.controller;

import cl.induztek.instalaciones.model.AsignacionCrearDTO;
import cl.induztek.instalaciones.model.AsignacionDTO;
import cl.induztek.instalaciones.security.JwtFiltro;
import cl.induztek.instalaciones.security.UsuarioToken;
import cl.induztek.instalaciones.service.AsignacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Asignaciones", description = "El supervisor asigna técnicos a empresas")
public class AsignacionController {

    private final AsignacionService service;

    public AsignacionController(AsignacionService service) {
        this.service = service;
    }

    @Operation(
            summary = "Crear asignación",
            description = "Asigna un técnico a una empresa. El técnico queda con una visita (OT) en cada instalación existente de esa empresa y puede registrar instalaciones nuevas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asignación creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos"),
            @ApiResponse(responseCode = "409", description = "Asignación duplicada")
    })
    @PostMapping("/asignaciones")
    public ResponseEntity<AsignacionDTO> crear(
            @Valid @RequestBody AsignacionCrearDTO dto,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto, usuario));
    }

    @Operation(
            summary = "Listar asignaciones",
            description = "Obtiene las asignaciones visibles para el usuario autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @GetMapping("/asignaciones")
    public ResponseEntity<List<AsignacionDTO>> listar(
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.listar(usuario));
    }

    @Operation(
            summary = "Eliminar asignación",
            description = "Elimina la asignación de un técnico a una empresa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Asignación eliminada"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos"),
            @ApiResponse(responseCode = "404", description = "Asignación no encontrada")
    })
    @DeleteMapping("/asignaciones/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        service.eliminar(id, usuario);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Empresas del técnico",
            description = "Empresas a las que el supervisor asignó al técnico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @GetMapping("/tecnicos/{id}/empresas")
    public ResponseEntity<List<AsignacionDTO>> empresasDeTecnico(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.empresasDeTecnico(id, usuario));
    }

    @Operation(summary = "Técnicos asignados a un cliente", description = "La empresa cliente ve qué técnicos le asignó el supervisor.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Solo la propia empresa")
    })
    @GetMapping("/clientes/{id}/tecnicos")
    public ResponseEntity<List<AsignacionDTO>> tecnicosDeCliente(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.tecnicosDeCliente(id, usuario));
    }
}
