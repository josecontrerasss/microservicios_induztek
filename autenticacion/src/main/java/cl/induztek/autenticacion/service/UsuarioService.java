package cl.induztek.autenticacion.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import cl.induztek.autenticacion.exception.ConflictoException;
import cl.induztek.autenticacion.exception.CredencialesInvalidasException;
import cl.induztek.autenticacion.exception.RecursoNoEncontradoException;
import cl.induztek.autenticacion.exception.SolicitudInvalidaException;
import cl.induztek.autenticacion.model.AuthRespuestaDTO;
import cl.induztek.autenticacion.model.CambiarClaveDTO;
import cl.induztek.autenticacion.model.LoginDTO;
import cl.induztek.autenticacion.model.Usuario;
import cl.induztek.autenticacion.model.UsuarioActualizarDTO;
import cl.induztek.autenticacion.model.UsuarioMostrarDTO;
import cl.induztek.autenticacion.model.UsuarioRegistroDTO;
import cl.induztek.autenticacion.repository.UsuarioRepository;
import cl.induztek.autenticacion.security.JwtService;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UsuarioMostrarDTO registrarCliente(UsuarioRegistroDTO dto) {
        if (!RutValidador.esValido(dto.getRut())) {
            throw new SolicitudInvalidaException("RUT inválido");
        }
        String rut = RutValidador.formatear(dto.getRut());
        String correo = normalizarCorreo(dto.getCorreo());
        if (repository.existsByRut(rut) || repository.existsByCorreo(correo)) {
            throw new ConflictoException("El RUT o el correo ya están registrados");
        }

        Usuario usuario = new Usuario();
        usuario.setRut(rut);
        usuario.setNombre(dto.getNombre().trim());
        usuario.setCorreo(correo);
        usuario.setTelefono(textoOpcional(dto.getTelefono()));
        usuario.setContrasena(passwordEncoder.encode(dto.getPassword()));
        usuario.setRol(Usuario.ROL_CLIENTE);
        usuario.setActivo(true);
        usuario.setGiro(textoOpcional(dto.getGiro()));
        usuario.setContactoNombre(textoOpcional(dto.getContactoNombre()));
        usuario.setContactoCargo(textoOpcional(dto.getContactoCargo()));
        return convertirADTO(repository.save(usuario));
    }

    public AuthRespuestaDTO iniciarSesion(LoginDTO dto) {
        Usuario usuario = repository.findByCorreo(normalizarCorreo(dto.getCorreo()))
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos"));
        if (!Boolean.TRUE.equals(usuario.getActivo())
                || !passwordEncoder.matches(dto.getPassword(), usuario.getContrasena())) {
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos");
        }
        String token = jwtService.generarToken(usuario.getId(), usuario.getCorreo(), usuario.getRol());
        return new AuthRespuestaDTO(token, usuario.getRol(), convertirADTO(usuario));
    }

    public List<UsuarioMostrarDTO> listarPorRol(String rol) {
        return repository.findByRolAndActivoTrueOrderByNombreAsc(rol).stream().map(this::convertirADTO).toList();
    }

    public UsuarioMostrarDTO obtenerUsuario(Long id) {
        return convertirADTO(buscarPorId(id));
    }

    public UsuarioMostrarDTO actualizarPerfil(Long id, UsuarioActualizarDTO dto) {
        Usuario usuario = buscarPorId(id);
        String correo = normalizarCorreo(dto.getCorreo());
        if (repository.existsByCorreoAndIdNot(correo, id)) {
            throw new ConflictoException("Ese correo ya está en uso");
        }
        usuario.setNombre(dto.getNombre().trim());
        usuario.setCorreo(correo);
        usuario.setTelefono(textoOpcional(dto.getTelefono()));
        usuario.setGiro(textoOpcional(dto.getGiro()));
        usuario.setContactoNombre(textoOpcional(dto.getContactoNombre()));
        usuario.setContactoCargo(textoOpcional(dto.getContactoCargo()));
        return convertirADTO(repository.save(usuario));
    }

    public void cambiarClave(Long id, CambiarClaveDTO dto) {
        Usuario usuario = buscarPorId(id);
        if (!passwordEncoder.matches(dto.getClaveActual(), usuario.getContrasena())) {
            throw new SolicitudInvalidaException("La contraseña actual es incorrecta");
        }
        if (dto.getClaveActual().equals(dto.getClaveNueva())) {
            throw new SolicitudInvalidaException("La nueva contraseña debe ser distinta a la actual");
        }
        usuario.setContrasena(passwordEncoder.encode(dto.getClaveNueva()));
        repository.save(usuario);
    }

    private Usuario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase();
    }

    private String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private UsuarioMostrarDTO convertirADTO(Usuario u) {
        UsuarioMostrarDTO dto = new UsuarioMostrarDTO();
        dto.setIdUsuario(u.getId());
        dto.setRut(u.getRut());
        dto.setNombre(u.getNombre());
        dto.setCorreo(u.getCorreo());
        dto.setTelefono(u.getTelefono());
        dto.setRol(u.getRol());
        dto.setIdRol(switch (u.getRol()) {
            case Usuario.ROL_TECNICO -> 1;
            case Usuario.ROL_SUPERVISOR -> 3;
            default -> 4;
        });
        dto.setActivo(u.getActivo());
        dto.setGiro(u.getGiro());
        dto.setContactoNombre(u.getContactoNombre());
        dto.setContactoCargo(u.getContactoCargo());
        return dto;
    }
}
