package cl.induztek.autenticacion.service;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UsuarioService service;

    @Test
    @DisplayName("Registrar un cliente con RUT válido guarda el usuario activo con rol CLIENTE")
    void registrarClienteExitoso() {
        UsuarioRegistroDTO dto = new UsuarioRegistroDTO();
        dto.setRut("12345678-5");
        dto.setNombre("  Juan Pérez  ");
        dto.setCorreo("  Juan@Induztek.CL  ");
        dto.setPassword("Clave123!");

        when(repository.existsByRut("12.345.678-5")).thenReturn(false);
        when(repository.existsByCorreo("juan@induztek.cl")).thenReturn(false);
        when(passwordEncoder.encode("Clave123!")).thenReturn("hash");
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UsuarioMostrarDTO resultado = service.registrarCliente(dto);

        assertThat(resultado.getIdUsuario()).isEqualTo(1L);
        assertThat(resultado.getRut()).isEqualTo("12.345.678-5");
        assertThat(resultado.getNombre()).isEqualTo("Juan Pérez");
        assertThat(resultado.getCorreo()).isEqualTo("juan@induztek.cl");
        assertThat(resultado.getRol()).isEqualTo(Usuario.ROL_CLIENTE);
        assertThat(resultado.getIdRol()).isEqualTo(4);
        assertThat(resultado.getActivo()).isTrue();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getContrasena()).isEqualTo("hash");
        assertThat(captor.getValue().getTelefono()).isNull();
        assertThat(captor.getValue().getGiro()).isNull();
    }

    @Test
    @DisplayName("Registrar con un RUT inválido responde solicitud inválida")
    void registrarConRutInvalido() {
        UsuarioRegistroDTO dto = new UsuarioRegistroDTO();
        dto.setRut("12345678-9");
        dto.setCorreo("juan@induztek.cl");
        dto.setPassword("Clave123!");

        assertThatThrownBy(() -> service.registrarCliente(dto))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("RUT inválido");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Registrar con un RUT ya usado responde conflicto")
    void registrarConRutDuplicado() {
        UsuarioRegistroDTO dto = new UsuarioRegistroDTO();
        dto.setRut("12345678-5");
        dto.setNombre("Juan");
        dto.setCorreo("juan@induztek.cl");
        dto.setPassword("Clave123!");

        when(repository.existsByRut("12.345.678-5")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarCliente(dto))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("El RUT o el correo ya están registrados");
    }

    @Test
    @DisplayName("Registrar con un correo ya usado responde conflicto")
    void registrarConCorreoDuplicado() {
        UsuarioRegistroDTO dto = new UsuarioRegistroDTO();
        dto.setRut("12345678-5");
        dto.setNombre("Juan");
        dto.setCorreo("juan@induztek.cl");
        dto.setPassword("Clave123!");

        when(repository.existsByRut("12.345.678-5")).thenReturn(false);
        when(repository.existsByCorreo("juan@induztek.cl")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarCliente(dto))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("El RUT o el correo ya están registrados");
    }

    @Test
    @DisplayName("Iniciar sesión con credenciales válidas devuelve un JWT con el rol del usuario")
    void iniciarSesionExitoso() {
        LoginDTO dto = new LoginDTO();
        dto.setCorreo("Tecnico@Induztek.cl");
        dto.setPassword("Clave123!");

        Usuario usuario = usuario(3L, "tecnico@induztek.cl", Usuario.ROL_TECNICO, true, "hash");
        when(repository.findByCorreo("tecnico@induztek.cl")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Clave123!", "hash")).thenReturn(true);
        when(jwtService.generarToken(3L, "tecnico@induztek.cl", Usuario.ROL_TECNICO)).thenReturn("jwt-token");

        AuthRespuestaDTO respuesta = service.iniciarSesion(dto);

        assertThat(respuesta.getToken()).isEqualTo("jwt-token");
        assertThat(respuesta.getRol()).isEqualTo("TECNICO");
        assertThat(respuesta.getUsuario().getIdUsuario()).isEqualTo(3L);
        assertThat(respuesta.getUsuario().getIdRol()).isEqualTo(1);
    }

    @Test
    @DisplayName("Iniciar sesión con un correo inexistente responde credenciales inválidas")
    void iniciarSesionCorreoInexistente() {
        LoginDTO dto = new LoginDTO();
        dto.setCorreo("no-existe@induztek.cl");
        dto.setPassword("Clave123!");

        when(repository.findByCorreo("no-existe@induztek.cl")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.iniciarSesion(dto))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos");
    }

    @Test
    @DisplayName("Iniciar sesión con contraseña incorrecta responde credenciales inválidas")
    void iniciarSesionPasswordIncorrecta() {
        LoginDTO dto = new LoginDTO();
        dto.setCorreo("tecnico@induztek.cl");
        dto.setPassword("mala");

        Usuario usuario = usuario(3L, "tecnico@induztek.cl", Usuario.ROL_TECNICO, true, "hash");
        when(repository.findByCorreo("tecnico@induztek.cl")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.iniciarSesion(dto))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos");
    }

    @Test
    @DisplayName("Un usuario inactivo no puede iniciar sesión")
    void iniciarSesionUsuarioInactivo() {
        LoginDTO dto = new LoginDTO();
        dto.setCorreo("tecnico@induztek.cl");
        dto.setPassword("Clave123!");

        Usuario usuario = usuario(3L, "tecnico@induztek.cl", Usuario.ROL_TECNICO, false, "hash");
        when(repository.findByCorreo("tecnico@induztek.cl")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.iniciarSesion(dto))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos");
        verify(jwtService, never()).generarToken(any(), any(), any());
    }

    @Test
    @DisplayName("Listar técnicos activos devuelve el rol como idRol 1")
    void listarTecnicosDevuelveIdRol1() {
        Usuario tecnico = usuario(7L, "tec@induztek.cl", Usuario.ROL_TECNICO, true, "hash");
        when(repository.findByRolAndActivoTrueOrderByNombreAsc("TECNICO"))
                .thenReturn(List.of(tecnico));

        List<UsuarioMostrarDTO> resultado = service.listarPorRol("TECNICO");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdRol()).isEqualTo(1);
        assertThat(resultado.get(0).getRol()).isEqualTo("TECNICO");
    }

    @Test
    @DisplayName("Listar supervisores devuelve el rol como idRol 3")
    void listarSupervisoresDevuelveIdRol3() {
        Usuario supervisor = usuario(8L, "sup@induztek.cl", Usuario.ROL_SUPERVISOR, true, "hash");
        when(repository.findByRolAndActivoTrueOrderByNombreAsc("SUPERVISOR"))
                .thenReturn(List.of(supervisor));

        List<UsuarioMostrarDTO> resultado = service.listarPorRol("SUPERVISOR");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdRol()).isEqualTo(3);
    }

    @Test
    @DisplayName("Listar un rol sin usuarios activos devuelve lista vacía")
    void listarRolSinUsuarios() {
        when(repository.findByRolAndActivoTrueOrderByNombreAsc("TECNICO")).thenReturn(List.of());
        assertThat(service.listarPorRol("TECNICO")).isEmpty();
    }

    @Test
    @DisplayName("Obtener un usuario existente devuelve sus datos")
    void obtenerUsuarioExistente() {
        Usuario cliente = usuario(9L, "cli@induztek.cl", Usuario.ROL_CLIENTE, true, "hash");
        when(repository.findById(9L)).thenReturn(Optional.of(cliente));

        UsuarioMostrarDTO dto = service.obtenerUsuario(9L);

        assertThat(dto.getIdUsuario()).isEqualTo(9L);
        assertThat(dto.getCorreo()).isEqualTo("cli@induztek.cl");
        assertThat(dto.getIdRol()).isEqualTo(4);
    }

    @Test
    @DisplayName("Obtener un usuario inexistente responde recurso no encontrado")
    void obtenerUsuarioInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerUsuario(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Usuario no encontrado con ID: 99");
    }

    @Test
    @DisplayName("Actualizar perfil con correo nuevo y datos válidos guarda los cambios")
    void actualizarPerfilExitoso() {
        Usuario cliente = usuario(11L, "viejo@induztek.cl", Usuario.ROL_CLIENTE, true, "hash");
        when(repository.findById(11L)).thenReturn(Optional.of(cliente));
        when(repository.existsByCorreoAndIdNot("nuevo@induztek.cl", 11L)).thenReturn(false);
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioActualizarDTO dto = new UsuarioActualizarDTO();
        dto.setNombre("  Juan Actualizado ");
        dto.setCorreo("  Nuevo@Induztek.CL ");
        dto.setTelefono("+56911112222");
        dto.setGiro("Servicios");
        dto.setContactoNombre("Pedro");
        dto.setContactoCargo("Jefe");

        UsuarioMostrarDTO resultado = service.actualizarPerfil(11L, dto);

        assertThat(resultado.getNombre()).isEqualTo("Juan Actualizado");
        assertThat(resultado.getCorreo()).isEqualTo("nuevo@induztek.cl");
        assertThat(resultado.getTelefono()).isEqualTo("+56911112222");
        assertThat(resultado.getContactoNombre()).isEqualTo("Pedro");
    }

    @Test
    @DisplayName("Actualizar perfil con correo usado por otro usuario responde conflicto")
    void actualizarPerfilCorreoEnUso() {
        Usuario cliente = usuario(11L, "viejo@induztek.cl", Usuario.ROL_CLIENTE, true, "hash");
        when(repository.findById(11L)).thenReturn(Optional.of(cliente));
        when(repository.existsByCorreoAndIdNot("nuevo@induztek.cl", 11L)).thenReturn(true);

        UsuarioActualizarDTO dto = new UsuarioActualizarDTO();
        dto.setNombre("Juan");
        dto.setCorreo("nuevo@induztek.cl");

        assertThatThrownBy(() -> service.actualizarPerfil(11L, dto))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("Ese correo ya está en uso");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar un usuario inexistente responde recurso no encontrado")
    void actualizarPerfilUsuarioNoExiste() {
        when(repository.findById(50L)).thenReturn(Optional.empty());
        UsuarioActualizarDTO dto = new UsuarioActualizarDTO();
        dto.setNombre("Juan");
        dto.setCorreo("juan@induztek.cl");

        assertThatThrownBy(() -> service.actualizarPerfil(50L, dto))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Cambiar la clave con la actual correcta y una nueva distinta guarda el nuevo hash")
    void cambiarClaveExitoso() {
        Usuario usuario = usuario(20L, "tec@induztek.cl", Usuario.ROL_TECNICO, true, "hash-viejo");
        when(repository.findById(20L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Vieja123!", "hash-viejo")).thenReturn(true);
        when(passwordEncoder.encode("Nueva456!")).thenReturn("hash-nuevo");
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        CambiarClaveDTO dto = new CambiarClaveDTO();
        dto.setClaveActual("Vieja123!");
        dto.setClaveNueva("Nueva456!");

        service.cambiarClave(20L, dto);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getContrasena()).isEqualTo("hash-nuevo");
    }

    @Test
    @DisplayName("Cambiar la clave con la actual incorrecta responde solicitud inválida")
    void cambiarClaveActualIncorrecta() {
        Usuario usuario = usuario(20L, "tec@induztek.cl", Usuario.ROL_TECNICO, true, "hash-viejo");
        when(repository.findById(20L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala", "hash-viejo")).thenReturn(false);

        CambiarClaveDTO dto = new CambiarClaveDTO();
        dto.setClaveActual("mala");
        dto.setClaveNueva("Nueva456!");

        assertThatThrownBy(() -> service.cambiarClave(20L, dto))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("La contraseña actual es incorrecta");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Cambiar la clave por la misma actual responde solicitud inválida")
    void cambiarClaveIgualALaActual() {
        Usuario usuario = usuario(20L, "tec@induztek.cl", Usuario.ROL_TECNICO, true, "hash-viejo");
        when(repository.findById(20L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Vieja123!", "hash-viejo")).thenReturn(true);

        CambiarClaveDTO dto = new CambiarClaveDTO();
        dto.setClaveActual("Vieja123!");
        dto.setClaveNueva("Vieja123!");

        assertThatThrownBy(() -> service.cambiarClave(20L, dto))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("La nueva contraseña debe ser distinta a la actual");
    }

    @Test
    @DisplayName("Cambiar la clave de un usuario inexistente responde recurso no encontrado")
    void cambiarClaveUsuarioNoExiste() {
        when(repository.findById(50L)).thenReturn(Optional.empty());
        CambiarClaveDTO dto = new CambiarClaveDTO();
        dto.setClaveActual("Vieja123!");
        dto.setClaveNueva("Nueva456!");

        assertThatThrownBy(() -> service.cambiarClave(50L, dto))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private Usuario usuario(Long id, String correo, String rol, Boolean activo, String contrasena) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setCorreo(correo);
        usuario.setNombre("Usuario de prueba");
        usuario.setRut("12.345.678-5");
        usuario.setRol(rol);
        usuario.setActivo(activo);
        usuario.setContrasena(contrasena);
        return usuario;
    }
}
