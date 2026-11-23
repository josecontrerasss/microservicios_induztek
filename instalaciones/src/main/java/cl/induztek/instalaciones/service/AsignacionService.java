package cl.induztek.instalaciones.service;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.instalaciones.exception.AccesoDenegadoException;
import cl.induztek.instalaciones.exception.ConflictoException;
import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.model.Asignacion;
import cl.induztek.instalaciones.model.AsignacionCrearDTO;
import cl.induztek.instalaciones.model.AsignacionDTO;
import cl.induztek.instalaciones.repository.AsignacionRepository;
import cl.induztek.instalaciones.repository.InstalacionRepository;
import cl.induztek.instalaciones.security.UsuarioToken;

@Service
public class AsignacionService {
    private static final String ROL_SUPERVISOR = "SUPERVISOR";
    private static final String ROL_TECNICO = "TECNICO";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final AsignacionRepository asignacionRepository;
    private final InstalacionRepository instalacionRepository;
    private final InstalacionService instalacionService;

    public AsignacionService(AsignacionRepository asignacionRepository, InstalacionRepository instalacionRepository,
            InstalacionService instalacionService) {
        this.asignacionRepository = asignacionRepository;
        this.instalacionRepository = instalacionRepository;
        this.instalacionService = instalacionService;
    }

    @Transactional
    public AsignacionDTO crear(AsignacionCrearDTO dto, UsuarioToken usuario) {
        exigirSupervisor(usuario);
        if (asignacionRepository.existsByIdClienteAndIdTecnico(dto.getIdCliente(), dto.getIdTecnico())) {
            throw new ConflictoException("Ese técnico ya está asignado a esa empresa");
        }
        Asignacion asignacion = new Asignacion();
        asignacion.setIdCliente(dto.getIdCliente());
        asignacion.setNombreCliente(dto.getNombreCliente().trim());
        asignacion.setIdTecnico(dto.getIdTecnico());
        asignacion.setNombreTecnico(dto.getNombreTecnico().trim());
        Asignacion guardada = asignacionRepository.save(asignacion);
        instalacionRepository.findByIdClienteOrderByNombreAsc(dto.getIdCliente())
                .forEach(instalacion -> instalacionService.asegurarVisita(instalacion.getId(), dto.getIdTecnico()));
        return convertir(guardada);
    }

    @Transactional(readOnly = true)
    public List<AsignacionDTO> listar(UsuarioToken usuario) {
        exigirSupervisor(usuario);
        return asignacionRepository.findAllByOrderByIdDesc().stream().map(this::convertir).toList();
    }

    @Transactional
    public void eliminar(Long id, UsuarioToken usuario) {
        exigirSupervisor(usuario);
        Asignacion asignacion = asignacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación no encontrada"));
        asignacionRepository.delete(asignacion);
    }

    @Transactional(readOnly = true)
    public List<AsignacionDTO> empresasDeTecnico(Long idTecnico, UsuarioToken usuario) {
        if (!ROL_TECNICO.equals(usuario.rol()) || !usuario.idUsuario().equals(idTecnico)) {
            throw new AccesoDenegadoException("Solo puedes consultar tu propia información");
        }
        return asignacionRepository.findByIdTecnicoOrderByNombreClienteAsc(idTecnico).stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public List<AsignacionDTO> tecnicosDeCliente(Long idCliente, UsuarioToken usuario) {
        if (!"CLIENTE".equals(usuario.rol()) || !usuario.idUsuario().equals(idCliente)) {
            throw new AccesoDenegadoException("Solo puedes consultar tu propia información");
        }
        return asignacionRepository.findByIdClienteOrderByNombreTecnicoAsc(idCliente).stream().map(this::convertir).toList();
    }

    private void exigirSupervisor(UsuarioToken usuario) {
        if (!ROL_SUPERVISOR.equals(usuario.rol())) {
            throw new AccesoDenegadoException("Esta acción es solo para el supervisor");
        }
    }

    private AsignacionDTO convertir(Asignacion a) {
        AsignacionDTO dto = new AsignacionDTO();
        dto.setIdAsignacion(a.getId());
        dto.setIdCliente(a.getIdCliente());
        dto.setNombreCliente(a.getNombreCliente());
        dto.setIdTecnico(a.getIdTecnico());
        dto.setNombreTecnico(a.getNombreTecnico());
        dto.setFechaAsignacion(a.getFechaAsignacion().format(FORMATO));
        return dto;
    }
}
