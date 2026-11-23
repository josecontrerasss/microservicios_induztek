package cl.induztek.instalaciones.controller;

import cl.induztek.instalaciones.model.EquipoDTO;
import cl.induztek.instalaciones.model.InstalacionDTO;
import cl.induztek.instalaciones.model.TipoEquipo;
import cl.induztek.instalaciones.model.VisitaDTO;
import cl.induztek.instalaciones.security.JwtFiltro;
import cl.induztek.instalaciones.security.UsuarioToken;
import cl.induztek.instalaciones.service.InstalacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import cl.induztek.instalaciones.model.EquipoCrearDTO;
import cl.induztek.instalaciones.model.InstalacionCrearDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Instalaciones", description = "Instalaciones, equipos y visitas")
public class InstalacionController {

    private final InstalacionService service;

    public InstalacionController(InstalacionService service) {
        this.service = service;
    }

    @Operation(summary = "Registrar instalación", description = "El técnico registra en terreno una instalación de una empresa a la que el supervisor lo asignó. Queda con una visita (OT) a su nombre.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Instalación creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No asignado a la empresa"),
            @ApiResponse(responseCode = "409", description = "Código repetido")
    })
    @PostMapping("/instalaciones")
    public ResponseEntity<InstalacionDTO> crearInstalacion(
            @Valid @RequestBody InstalacionCrearDTO dto,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearInstalacion(dto, usuario));
    }

    @Operation(summary = "Registrar equipo", description = "El técnico registra un equipo en una instalación a la que tiene acceso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Equipo creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin acceso a la instalación"),
            @ApiResponse(responseCode = "409", description = "TAG repetido")
    })
    @PostMapping("/instalaciones/{id}/equipos")
    public ResponseEntity<EquipoDTO> crearEquipo(
            @PathVariable Long id,
            @Valid @RequestBody EquipoCrearDTO dto,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearEquipo(id, dto, usuario));
    }

    @Operation(summary = "Listar instalaciones de un cliente", description = "Obtiene las instalaciones asociadas a un cliente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @GetMapping("/clientes/{id}/instalaciones")
    public ResponseEntity<List<InstalacionDTO>> instalacionesDeCliente(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.instalacionesDeCliente(id, usuario));
    }

    @Operation(summary = "Listar instalaciones de un tecnico", description = "Obtiene las instalaciones asignadas a un tecnico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @GetMapping("/tecnicos/{id}/instalaciones")
    public ResponseEntity<List<InstalacionDTO>> instalacionesDeTecnico(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.instalacionesDeTecnico(id, usuario));
    }

    @Operation(summary = "Listar visitas de un tecnico", description = "Obtiene las visitas registradas para un tecnico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @GetMapping("/tecnicos/{id}/visitas")
    public ResponseEntity<List<VisitaDTO>> visitasDeTecnico(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.visitasDeTecnico(id, usuario));
    }

    @Operation(summary = "Listar visitas de un cliente", description = "Obtiene las visitas realizadas en las instalaciones del cliente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @GetMapping("/clientes/{id}/visitas")
    public ResponseEntity<List<VisitaDTO>> visitasDeCliente(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.visitasDeCliente(id, usuario));
    }

    @Operation(summary = "Listar equipos de un tecnico", description = "Obtiene los equipos asociados a las instalaciones de un tecnico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @GetMapping("/tecnicos/{id}/equipos")
    public ResponseEntity<List<EquipoDTO>> equiposDeTecnico(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.equiposDeTecnico(id, usuario));
    }

    @Operation(summary = "Obtener instalacion", description = "Obtiene el detalle de una instalacion por su identificador.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Instalacion obtenida"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Instalacion no encontrada")
    })
    @GetMapping("/instalaciones/{id}")
    public ResponseEntity<InstalacionDTO> obtenerInstalacion(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.obtenerInstalacion(id, usuario));
    }

    @Operation(summary = "Listar equipos de una instalacion", description = "Obtiene los equipos asociados a una instalacion.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Instalacion no encontrada")
    })
    @GetMapping("/instalaciones/{id}/equipos")
    public ResponseEntity<List<EquipoDTO>> equiposDeInstalacion(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.equiposDeInstalacion(id, usuario));
    }

    @Operation(summary = "Obtener equipo", description = "Obtiene el detalle de un equipo por su identificador.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Equipo obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Equipo no encontrado")
    })
    @GetMapping("/equipos/{id}")
    public ResponseEntity<EquipoDTO> obtenerEquipo(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.obtenerEquipo(id, usuario));
    }

    @Operation(summary = "Obtener visita", description = "Obtiene el detalle de una visita por su identificador.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Visita obtenida"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Visita no encontrada")
    })
    @GetMapping("/visitas/{id}")
    public ResponseEntity<VisitaDTO> obtenerVisita(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestAttribute(JwtFiltro.ATRIBUTO_USUARIO) UsuarioToken usuario) {
        return ResponseEntity.ok(service.obtenerVisita(id, usuario));
    }

    @Operation(summary = "Listar tipos de equipo", description = "Obtiene el catalogo de tipos de equipo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @GetMapping("/tipos-equipo")
    public ResponseEntity<List<TipoEquipo>> tiposEquipo() {
        return ResponseEntity.ok(service.tiposEquipo());
    }
}
