package cl.induztek.protocolos.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.protocolos.client.InstalacionesCliente;
import cl.induztek.protocolos.exception.AccesoDenegadoException;
import cl.induztek.protocolos.exception.ConflictoException;
import cl.induztek.protocolos.exception.RecursoNoEncontradoException;
import cl.induztek.protocolos.exception.SolicitudInvalidaException;
import cl.induztek.protocolos.model.EquipoRemotoDTO;
import cl.induztek.protocolos.model.InstalacionRemotaDTO;
import cl.induztek.protocolos.model.Medicion;
import cl.induztek.protocolos.model.MedicionDTO;
import cl.induztek.protocolos.model.Protocolo;
import cl.induztek.protocolos.model.ProtocoloConMedicionesDTO;
import cl.induztek.protocolos.model.ProtocoloDTO;
import cl.induztek.protocolos.model.SyncRespuestaDTO;
import cl.induztek.protocolos.model.VisitaRemotaDTO;
import cl.induztek.protocolos.repository.ProtocoloRepository;
import cl.induztek.protocolos.security.UsuarioToken;
import jakarta.validation.Validator;

@Service
public class ProtocoloService {
    private static final String ROL_TECNICO = "TECNICO";
    private static final String ROL_CLIENTE = "CLIENTE";
    private static final String ROL_SUPERVISOR = "SUPERVISOR";
    private static final String ESTADO_REGISTRADO = "REGISTRADO_TERRENO";

    private final ProtocoloRepository repository;
    private final InstalacionesCliente instalaciones;
    private final Validator validator;

    public ProtocoloService(ProtocoloRepository repository, InstalacionesCliente instalaciones, Validator validator) {
        this.repository = repository;
        this.instalaciones = instalaciones;
        this.validator = validator;
    }

    @Transactional
    public ProtocoloConMedicionesDTO crear(ProtocoloConMedicionesDTO dto, UsuarioToken usuario, String autorizacion) {
        exigirRol(ROL_TECNICO, usuario);
        String numero = dto.getProtocolo().getNumero().trim();
        if (repository.findByNumero(numero).isPresent()) {
            throw new ConflictoException("Ya existe un protocolo con el número " + numero);
        }
        return convertirConMediciones(guardar(dto, usuario, autorizacion));
    }

    @Transactional
    public SyncRespuestaDTO sincronizar(List<ProtocoloConMedicionesDTO> lista, UsuarioToken usuario, String autorizacion) {
        exigirRol(ROL_TECNICO, usuario);
        List<String> aceptados = new ArrayList<>();
        List<String> rechazados = new ArrayList<>();
        for (ProtocoloConMedicionesDTO item : lista) {
            if (item == null || item.getProtocolo() == null || !validator.validate(item).isEmpty()) {
                rechazados.add(etiqueta(item) + ": datos incompletos o inválidos");
                continue;
            }
            Optional<Protocolo> existente = repository.findByNumero(item.getProtocolo().getNumero().trim());
            if (existente.isPresent()) {
                if (existente.get().getIdTecnico().equals(usuario.idUsuario())) {
                    aceptados.add(existente.get().getNumero());
                } else {
                    rechazados.add(etiqueta(item) + ": el número ya pertenece a otro técnico");
                }
                continue;
            }
            try {
                aceptados.add(guardar(item, usuario, autorizacion).getNumero());
            } catch (SolicitudInvalidaException | AccesoDenegadoException e) {
                rechazados.add(etiqueta(item) + ": " + e.getMessage());
            }
        }
        return new SyncRespuestaDTO(aceptados.size(), aceptados, rechazados);
    }

    @Transactional(readOnly = true)
    public ProtocoloConMedicionesDTO obtener(Long id, UsuarioToken usuario) {
        Protocolo protocolo = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Protocolo no encontrado"));
        boolean esCliente = ROL_CLIENTE.equals(usuario.rol()) && protocolo.getIdCliente().equals(usuario.idUsuario());
        boolean esTecnico = ROL_TECNICO.equals(usuario.rol()) && protocolo.getIdTecnico().equals(usuario.idUsuario());
        if (!esCliente && !esTecnico) {
            throw new AccesoDenegadoException("No tienes acceso a este protocolo");
        }
        return convertirConMediciones(protocolo);
    }

    @Transactional(readOnly = true)
    public List<ProtocoloDTO> protocolosDeCliente(Long idCliente, UsuarioToken usuario) {
        exigirMismoUsuario(idCliente, ROL_CLIENTE, usuario);
        return repository.findByIdClienteOrderByIdDesc(idCliente).stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public List<ProtocoloDTO> protocolosDeTecnico(Long idTecnico, UsuarioToken usuario) {
        exigirMismoUsuario(idTecnico, ROL_TECNICO, usuario);
        return repository.findByIdTecnicoOrderByIdDesc(idTecnico).stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public List<ProtocoloConMedicionesDTO> pendientesDeRevision(UsuarioToken usuario) {
        exigirRol(ROL_SUPERVISOR, usuario);
        return repository.findByEstadoOrderByIdDesc(ESTADO_REGISTRADO).stream().map(this::convertirConMediciones).toList();
    }

    @Transactional
    public ProtocoloDTO cambiarEstado(Long id, boolean aprobar, UsuarioToken usuario) {
        exigirRol(ROL_SUPERVISOR, usuario);
        Protocolo protocolo = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Protocolo no encontrado"));
        if (!ESTADO_REGISTRADO.equals(protocolo.getEstado())) {
            throw new ConflictoException("El protocolo ya fue revisado");
        }
        protocolo.setEstado(aprobar ? "FORMALIZADO" : "ANULADO");
        return convertir(repository.save(protocolo));
    }

    private Protocolo guardar(ProtocoloConMedicionesDTO dto, UsuarioToken usuario, String autorizacion) {
        ProtocoloDTO datos = dto.getProtocolo();
        VisitaRemotaDTO visita = instalaciones.obtenerVisita(datos.getIdVisita(), autorizacion);
        if (!usuario.idUsuario().equals(visita.getIdTecnico())) {
            throw new AccesoDenegadoException("La visita no está asignada a este técnico");
        }
        EquipoRemotoDTO equipo = instalaciones.obtenerEquipo(datos.getIdEquipo(), autorizacion);
        if (!visita.getIdInstalacion().equals(equipo.getIdInstalacion())) {
            throw new SolicitudInvalidaException("El equipo no pertenece a la instalación de la visita");
        }
        InstalacionRemotaDTO instalacion = instalaciones.obtenerInstalacion(visita.getIdInstalacion(), autorizacion);

        Protocolo protocolo = new Protocolo();
        protocolo.setNumero(datos.getNumero().trim());
        protocolo.setIdVisita(datos.getIdVisita());
        protocolo.setIdEquipo(datos.getIdEquipo());
        protocolo.setIdInstalacion(visita.getIdInstalacion());
        protocolo.setIdCliente(instalacion.getIdUsuarioCliente());
        protocolo.setIdTecnico(usuario.idUsuario());
        protocolo.setFechaHoraPrueba(datos.getFechaHoraPrueba());
        protocolo.setTemperaturaC(datos.getTemperaturaC());
        protocolo.setHumedadPct(datos.getHumedadPct());
        protocolo.setDescripcionMantenimiento(datos.getDescripcionMantenimiento());
        protocolo.setObservaciones(datos.getObservaciones());
        protocolo.setEstado(ESTADO_REGISTRADO);
        protocolo.setResultado(datos.getResultado());
        protocolo.setFotoPath(datos.getFotoPath());
        if (dto.getMediciones() != null) {
            for (MedicionDTO m : dto.getMediciones()) {
                Medicion medicion = new Medicion();
                medicion.setParametroCodigo(m.getParametroCodigo());
                medicion.setParametroNombre(m.getParametroNombre());
                medicion.setValor(m.getValor());
                medicion.setUnidad(m.getUnidad());
                medicion.setEstadoAlerta(textoODefecto(m.getEstadoAlerta(), "OK"));
                protocolo.agregarMedicion(medicion);
            }
        }
        return repository.save(protocolo);
    }

    private void exigirRol(String rol, UsuarioToken usuario) {
        if (!rol.equals(usuario.rol())) {
            throw new AccesoDenegadoException("Esta acción es solo para el rol " + rol);
        }
    }

    private void exigirMismoUsuario(Long id, String rol, UsuarioToken usuario) {
        if (!rol.equals(usuario.rol()) || !usuario.idUsuario().equals(id)) {
            throw new AccesoDenegadoException("Solo puedes consultar tu propia información");
        }
    }

    private String etiqueta(ProtocoloConMedicionesDTO item) {
        if (item == null || item.getProtocolo() == null || item.getProtocolo().getNumero() == null
                || item.getProtocolo().getNumero().isBlank()) {
            return "Protocolo sin número";
        }
        return item.getProtocolo().getNumero();
    }

    private String textoODefecto(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor.trim();
    }

    private ProtocoloConMedicionesDTO convertirConMediciones(Protocolo p) {
        ProtocoloConMedicionesDTO dto = new ProtocoloConMedicionesDTO();
        dto.setProtocolo(convertir(p));
        dto.setMediciones(p.getMediciones().stream().map(m -> {
            MedicionDTO medicion = new MedicionDTO();
            medicion.setIdMedicion(m.getId());
            medicion.setIdProtocolo(p.getId());
            medicion.setParametroCodigo(m.getParametroCodigo());
            medicion.setParametroNombre(m.getParametroNombre());
            medicion.setValor(m.getValor());
            medicion.setUnidad(m.getUnidad());
            medicion.setEstadoAlerta(m.getEstadoAlerta());
            return medicion;
        }).toList());
        return dto;
    }

    private ProtocoloDTO convertir(Protocolo p) {
        ProtocoloDTO dto = new ProtocoloDTO();
        dto.setIdProtocolo(p.getId());
        dto.setNumero(p.getNumero());
        dto.setIdVisita(p.getIdVisita());
        dto.setIdEquipo(p.getIdEquipo());
        dto.setIdUsuarioRegistra(p.getIdTecnico());
        dto.setIdInstalacion(p.getIdInstalacion());
        dto.setIdCliente(p.getIdCliente());
        dto.setFechaHoraPrueba(p.getFechaHoraPrueba());
        dto.setTemperaturaC(p.getTemperaturaC());
        dto.setHumedadPct(p.getHumedadPct());
        dto.setDescripcionMantenimiento(p.getDescripcionMantenimiento());
        dto.setObservaciones(p.getObservaciones());
        dto.setEstado(p.getEstado());
        dto.setResultado(p.getResultado());
        dto.setSincronizado(true);
        dto.setFotoPath(p.getFotoPath());
        return dto;
    }
}
