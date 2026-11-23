package cl.induztek.instalaciones.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.instalaciones.exception.AccesoDenegadoException;
import cl.induztek.instalaciones.exception.ConflictoException;
import cl.induztek.instalaciones.exception.SolicitudInvalidaException;
import cl.induztek.instalaciones.model.EquipoCrearDTO;
import cl.induztek.instalaciones.model.InstalacionCrearDTO;
import cl.induztek.instalaciones.repository.AsignacionRepository;
import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.model.Equipo;
import cl.induztek.instalaciones.model.EquipoDTO;
import cl.induztek.instalaciones.model.Instalacion;
import cl.induztek.instalaciones.model.InstalacionDTO;
import cl.induztek.instalaciones.model.TipoEquipo;
import cl.induztek.instalaciones.model.Visita;
import cl.induztek.instalaciones.model.VisitaDTO;
import cl.induztek.instalaciones.repository.EquipoRepository;
import cl.induztek.instalaciones.repository.InstalacionRepository;
import cl.induztek.instalaciones.repository.TipoEquipoRepository;
import cl.induztek.instalaciones.repository.VisitaRepository;
import cl.induztek.instalaciones.security.UsuarioToken;

@Service
@Transactional(readOnly = true)
public class InstalacionService {
    private static final String ROL_TECNICO = "TECNICO";
    private static final String ROL_CLIENTE = "CLIENTE";

    private final InstalacionRepository instalacionRepository;
    private final EquipoRepository equipoRepository;
    private final VisitaRepository visitaRepository;
    private final TipoEquipoRepository tipoEquipoRepository;
    private final AsignacionRepository asignacionRepository;
    private static final DateTimeFormatter FORMATO_FECHA_VISITA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FORMATO_ACTIVIDAD = DateTimeFormatter.ofPattern("d MMM. yyyy", Locale.forLanguageTag("es-CL"));

    public InstalacionService(InstalacionRepository instalacionRepository, EquipoRepository equipoRepository,
            VisitaRepository visitaRepository, TipoEquipoRepository tipoEquipoRepository,
            AsignacionRepository asignacionRepository) {
        this.instalacionRepository = instalacionRepository;
        this.equipoRepository = equipoRepository;
        this.visitaRepository = visitaRepository;
        this.tipoEquipoRepository = tipoEquipoRepository;
        this.asignacionRepository = asignacionRepository;
    }

    public List<InstalacionDTO> instalacionesDeCliente(Long idCliente, UsuarioToken usuario) {
        exigirMismoUsuario(idCliente, ROL_CLIENTE, usuario);
        return instalacionRepository.findByIdClienteOrderByNombreAsc(idCliente).stream().map(this::convertir).toList();
    }

    public List<InstalacionDTO> instalacionesDeTecnico(Long idTecnico, UsuarioToken usuario) {
        exigirMismoUsuario(idTecnico, ROL_TECNICO, usuario);
        return instalacionRepository.findAsignadasATecnico(idTecnico).stream().map(this::convertir).toList();
    }

    public List<VisitaDTO> visitasDeCliente(Long idCliente, UsuarioToken usuario) {
        exigirMismoUsuario(idCliente, ROL_CLIENTE, usuario);
        List<Long> ids = instalacionRepository.findByIdClienteOrderByNombreAsc(idCliente).stream().map(Instalacion::getId).toList();
        return visitaRepository.findByIdInstalacionInOrderByIdDesc(ids).stream().map(this::convertir).toList();
    }

    public List<VisitaDTO> visitasDeTecnico(Long idTecnico, UsuarioToken usuario) {
        exigirMismoUsuario(idTecnico, ROL_TECNICO, usuario);
        return visitaRepository.findByIdTecnicoOrderByIdDesc(idTecnico).stream().map(this::convertir).toList();
    }

    public List<EquipoDTO> equiposDeTecnico(Long idTecnico, UsuarioToken usuario) {
        exigirMismoUsuario(idTecnico, ROL_TECNICO, usuario);
        List<Long> ids = instalacionRepository.findAsignadasATecnico(idTecnico).stream().map(Instalacion::getId).toList();
        return convertirEquipos(equipoRepository.findByIdInstalacionInOrderByTagAsc(ids));
    }

    public InstalacionDTO obtenerInstalacion(Long id, UsuarioToken usuario) {
        return convertir(instalacionVisible(id, usuario));
    }

    public List<EquipoDTO> equiposDeInstalacion(Long idInstalacion, UsuarioToken usuario) {
        instalacionVisible(idInstalacion, usuario);
        return convertirEquipos(equipoRepository.findByIdInstalacionOrderByTagAsc(idInstalacion));
    }

    public EquipoDTO obtenerEquipo(Long id, UsuarioToken usuario) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado"));
        instalacionVisible(equipo.getIdInstalacion(), usuario);
        return convertirEquipos(List.of(equipo)).get(0);
    }

    public VisitaDTO obtenerVisita(Long id, UsuarioToken usuario) {
        Visita visita = visitaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Visita no encontrada"));
        instalacionVisible(visita.getIdInstalacion(), usuario);
        return convertir(visita);
    }

    @Transactional
    public InstalacionDTO crearInstalacion(InstalacionCrearDTO dto, UsuarioToken usuario) {
        if (!ROL_TECNICO.equals(usuario.rol())
                || !asignacionRepository.existsByIdClienteAndIdTecnico(dto.getIdCliente(), usuario.idUsuario())) {
            throw new AccesoDenegadoException("El supervisor no te ha asignado a esta empresa");
        }
        String codigo = dto.getCodigo().trim().toUpperCase();
        if (instalacionRepository.existsByCodigo(codigo)) {
            throw new ConflictoException("Ya existe una instalación con el código " + codigo);
        }
        Instalacion instalacion = new Instalacion();
        instalacion.setIdCliente(dto.getIdCliente());
        instalacion.setCodigo(codigo);
        instalacion.setNombre(dto.getNombre().trim());
        instalacion.setUbicacion(dto.getUbicacion().trim());
        instalacion.setObservacionesAcceso(dto.getObservacionesAcceso() == null ? "" : dto.getObservacionesAcceso().trim());
        instalacion.setUltimaActividad(LocalDate.now().format(FORMATO_ACTIVIDAD));
        Instalacion guardada = instalacionRepository.save(instalacion);
        asegurarVisita(guardada.getId(), usuario.idUsuario());
        return convertir(guardada);
    }

    @Transactional
    public EquipoDTO crearEquipo(Long idInstalacion, EquipoCrearDTO dto, UsuarioToken usuario) {
        if (!ROL_TECNICO.equals(usuario.rol())) {
            throw new AccesoDenegadoException("Solo un técnico puede registrar equipos");
        }
        instalacionVisible(idInstalacion, usuario);
        if (!tipoEquipoRepository.existsById(dto.getIdTipoEquipo())) {
            throw new SolicitudInvalidaException("El tipo de equipo no existe");
        }
        String tag = dto.getTag().trim();
        if (equipoRepository.existsByIdInstalacionAndTag(idInstalacion, tag)) {
            throw new ConflictoException("Ya existe un equipo con el TAG " + tag + " en esta instalación");
        }
        Equipo equipo = new Equipo();
        equipo.setIdInstalacion(idInstalacion);
        equipo.setTag(tag);
        equipo.setIdTipoEquipo(dto.getIdTipoEquipo());
        equipo.setMarca(dto.getMarca().trim());
        equipo.setModelo(dto.getModelo().trim());
        equipo.setNumeroSerie(dto.getNumeroSerie().trim());
        equipo.setAnioFabricacion(dto.getAnioFabricacion());
        equipo.setPotenciaKva(dto.getPotenciaKva());
        equipo.setTensionPrimariaKv(dto.getTensionPrimariaKv());
        equipo.setTensionSecundariaKv(dto.getTensionSecundariaKv());
        equipo.setCorrienteNominalA(dto.getCorrienteNominalA());
        equipo.setObservaciones(dto.getObservaciones() == null ? "" : dto.getObservaciones().trim());
        return convertirEquipos(List.of(equipoRepository.save(equipo))).get(0);
    }

    @Transactional
    public void asegurarVisita(Long idInstalacion, Long idTecnico) {
        if (visitaRepository.existsByIdInstalacionAndIdTecnico(idInstalacion, idTecnico)) {
            return;
        }
        Visita visita = new Visita();
        visita.setNumeroOt("OT-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        visita.setIdInstalacion(idInstalacion);
        visita.setFecha(LocalDate.now().format(FORMATO_FECHA_VISITA));
        visita.setTipoVisita("PRUEBAS_Y_MANT");
        visita.setIdTecnico(idTecnico);
        visitaRepository.save(visita);
    }

    public List<TipoEquipo> tiposEquipo() {
        return tipoEquipoRepository.findAllByOrderByNombreAsc();
    }

    private void exigirMismoUsuario(Long id, String rol, UsuarioToken usuario) {
        if (!rol.equals(usuario.rol()) || !usuario.idUsuario().equals(id)) {
            throw new AccesoDenegadoException("Solo puedes consultar tu propia información");
        }
    }

    private Instalacion instalacionVisible(Long id, UsuarioToken usuario) {
        Instalacion instalacion = instalacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Instalación no encontrada"));
        boolean esDueno = ROL_CLIENTE.equals(usuario.rol()) && instalacion.getIdCliente().equals(usuario.idUsuario());
        boolean estaAsignado = ROL_TECNICO.equals(usuario.rol())
                && visitaRepository.existsByIdInstalacionAndIdTecnico(id, usuario.idUsuario());
        if (!esDueno && !estaAsignado) {
            throw new AccesoDenegadoException("No tienes acceso a esta instalación");
        }
        return instalacion;
    }

    private List<EquipoDTO> convertirEquipos(List<Equipo> equipos) {
        Map<Long, String> tipos = tipoEquipoRepository.findAll().stream()
                .collect(Collectors.toMap(TipoEquipo::getId, TipoEquipo::getNombre));
        Function<Equipo, EquipoDTO> convertir = e -> {
            EquipoDTO dto = new EquipoDTO();
            dto.setIdEquipo(e.getId());
            dto.setIdInstalacion(e.getIdInstalacion());
            dto.setTag(e.getTag());
            dto.setIdTipoEquipo(e.getIdTipoEquipo());
            dto.setTipoEquipo(tipos.get(e.getIdTipoEquipo()));
            dto.setMarca(e.getMarca());
            dto.setModelo(e.getModelo());
            dto.setNumeroSerie(e.getNumeroSerie());
            dto.setAnioFabricacion(e.getAnioFabricacion());
            dto.setPotenciaKva(e.getPotenciaKva());
            dto.setTensionPrimariaKv(e.getTensionPrimariaKv());
            dto.setTensionSecundariaKv(e.getTensionSecundariaKv());
            dto.setCorrienteNominalA(e.getCorrienteNominalA());
            dto.setObservaciones(e.getObservaciones());
            return dto;
        };
        return equipos.stream().map(convertir).toList();
    }

    private InstalacionDTO convertir(Instalacion i) {
        InstalacionDTO dto = new InstalacionDTO();
        dto.setIdInstalacion(i.getId());
        dto.setIdUsuarioCliente(i.getIdCliente());
        dto.setCodigo(i.getCodigo());
        dto.setNombre(i.getNombre());
        dto.setUbicacion(i.getUbicacion());
        dto.setObservacionesAcceso(i.getObservacionesAcceso());
        dto.setUltimaActividad(i.getUltimaActividad());
        return dto;
    }

    private VisitaDTO convertir(Visita v) {
        VisitaDTO dto = new VisitaDTO();
        dto.setIdVisita(v.getId());
        dto.setNumeroOt(v.getNumeroOt());
        dto.setIdInstalacion(v.getIdInstalacion());
        dto.setFecha(v.getFecha());
        dto.setTipoVisita(v.getTipoVisita());
        dto.setIdTecnico(v.getIdTecnico());
        return dto;
    }
}
