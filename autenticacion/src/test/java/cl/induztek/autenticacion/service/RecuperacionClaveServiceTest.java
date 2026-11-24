package cl.induztek.autenticacion.service;

import cl.induztek.autenticacion.exception.SolicitudInvalidaException;
import cl.induztek.autenticacion.model.CodigoRecuperacion;
import cl.induztek.autenticacion.model.RecuperarClaveDTO;
import cl.induztek.autenticacion.model.RestablecerClaveDTO;
import cl.induztek.autenticacion.model.Usuario;
import cl.induztek.autenticacion.repository.CodigoRecuperacionRepository;
import cl.induztek.autenticacion.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecuperacionClaveServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CodigoRecuperacionRepository codigoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    private RecuperacionClaveService service;

    @BeforeEach
    void setUp() {
        service = new RecuperacionClaveService(usuarioRepository, codigoRepository, passwordEncoder,
                mailSender, 15, "no-responder@induztek.cl");
    }

    @Test
    @DisplayName("Solicitar código para un cliente activo guarda el código y envía el correo")
    void solicitarCodigoClienteActivo() {
        Usuario usuario = usuario(1L, "cliente@induztek.cl", "Juan", true);
        when(usuarioRepository.findByCorreo("cliente@induztek.cl")).thenReturn(Optional.of(usuario));

        service.solicitarCodigo(dtoRecuperar("  Cliente@Induztek.CL  "));

        ArgumentCaptor<CodigoRecuperacion> codigoCaptor = ArgumentCaptor.forClass(CodigoRecuperacion.class);
        verify(codigoRepository).save(codigoCaptor.capture());
        CodigoRecuperacion guardado = codigoCaptor.getValue();
        assertThat(guardado.getIdUsuario()).isEqualTo(1L);
        assertThat(guardado.getCodigo()).matches("\\d{6}");
        assertThat(guardado.getUsado()).isFalse();
        assertThat(guardado.getExpiraEn()).isAfter(LocalDateTime.now());

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        SimpleMailMessage mail = mailCaptor.getValue();
        assertThat(mail.getFrom()).isEqualTo("no-responder@induztek.cl");
        assertThat(mail.getTo()).containsExactly("cliente@induztek.cl");
        assertThat(mail.getSubject()).contains("Induztek");
        assertThat(mail.getText()).contains(guardado.getCodigo());
        assertThat(mail.getText()).contains("Juan");
    }

    @Test
    @DisplayName("Un cliente inactivo no recibe código de recuperación")
    void solicitarCodigoClienteInactivo() {
        Usuario usuario = usuario(1L, "cliente@induztek.cl", "Juan", false);
        when(usuarioRepository.findByCorreo("cliente@induztek.cl")).thenReturn(Optional.of(usuario));

        service.solicitarCodigo(dtoRecuperar("cliente@induztek.cl"));

        verify(codigoRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Un correo no registrado no genera código ni envía correo")
    void solicitarCodigoCorreoNoRegistrado() {
        when(usuarioRepository.findByCorreo("nadie@induztek.cl")).thenReturn(Optional.empty());

        service.solicitarCodigo(dtoRecuperar("nadie@induztek.cl"));

        verify(codigoRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Si el remitente está en blanco se usa no-responder@induztek.cl por defecto")
    void remitenteEnBlancoUsaDefault() {
        RecuperacionClaveService conDefault = new RecuperacionClaveService(usuarioRepository, codigoRepository,
                passwordEncoder, mailSender, 15, "   ");
        Usuario usuario = usuario(1L, "cliente@induztek.cl", "Juan", true);
        when(usuarioRepository.findByCorreo("cliente@induztek.cl")).thenReturn(Optional.of(usuario));

        conDefault.solicitarCodigo(dtoRecuperar("cliente@induztek.cl"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isEqualTo("no-responder@induztek.cl");
    }

    @Test
    @DisplayName("Restablecer la contraseña con un código válido y vigente guarda el nuevo hash")
    void restablecerClaveExitoso() {
        Usuario usuario = usuario(1L, "cliente@induztek.cl", "Juan", true);
        CodigoRecuperacion codigo = codigo(1L, "123456", LocalDateTime.now().plusMinutes(10), false);

        when(usuarioRepository.findByCorreo("cliente@induztek.cl")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByIdUsuarioAndUsadoFalseOrderByIdDesc(1L)).thenReturn(Optional.of(codigo));
        when(passwordEncoder.encode("Nueva123!")).thenReturn("hash-nuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(codigoRepository.save(any(CodigoRecuperacion.class))).thenReturn(codigo);

        service.restablecerClave(dtoRestablecer("cliente@induztek.cl", "123456", "Nueva123!"));

        assertThat(codigo.getUsado()).isTrue();
        assertThat(usuario.getContrasena()).isEqualTo("hash-nuevo");
        verify(codigoRepository).save(codigo);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Restablecer con un correo no registrado responde código inválido o expirado")
    void restablecerCorreoNoRegistrado() {
        when(usuarioRepository.findByCorreo("nadie@induztek.cl")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.restablecerClave(
                dtoRestablecer("nadie@induztek.cl", "123456", "Nueva123!")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("Código inválido o expirado");
    }

    @Test
    @DisplayName("Restablecer con un código distinto al guardado responde código inválido")
    void restablecerCodigoIncorrecto() {
        Usuario usuario = usuario(1L, "cliente@induztek.cl", "Juan", true);
        CodigoRecuperacion codigo = codigo(1L, "123456", LocalDateTime.now().plusMinutes(10), false);

        when(usuarioRepository.findByCorreo("cliente@induztek.cl")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByIdUsuarioAndUsadoFalseOrderByIdDesc(1L)).thenReturn(Optional.of(codigo));

        assertThatThrownBy(() -> service.restablecerClave(
                dtoRestablecer("cliente@induztek.cl", "999999", "Nueva123!")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("Código inválido o expirado");
        verify(codigoRepository, never()).save(codigo);
    }

    @Test
    @DisplayName("Restablecer con un código expirado responde código inválido o expirado")
    void restablecerCodigoExpirado() {
        Usuario usuario = usuario(1L, "cliente@induztek.cl", "Juan", true);
        CodigoRecuperacion codigo = codigo(1L, "123456", LocalDateTime.now().minusMinutes(1), false);

        when(usuarioRepository.findByCorreo("cliente@induztek.cl")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByIdUsuarioAndUsadoFalseOrderByIdDesc(1L)).thenReturn(Optional.of(codigo));

        assertThatThrownBy(() -> service.restablecerClave(
                dtoRestablecer("cliente@induztek.cl", "123456", "Nueva123!")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("Código inválido o expirado");
        verify(codigoRepository, never()).save(codigo);
    }

    @Test
    @DisplayName("Restablecer sin códigos vigentes para el usuario responde código inválido o expirado")
    void restablecerSinCodigoVigente() {
        Usuario usuario = usuario(1L, "cliente@induztek.cl", "Juan", true);
        when(usuarioRepository.findByCorreo("cliente@induztek.cl")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByIdUsuarioAndUsadoFalseOrderByIdDesc(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.restablecerClave(
                dtoRestablecer("cliente@induztek.cl", "123456", "Nueva123!")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("Código inválido o expirado");
    }

    private RecuperarClaveDTO dtoRecuperar(String correo) {
        RecuperarClaveDTO dto = new RecuperarClaveDTO();
        dto.setCorreo(correo);
        return dto;
    }

    private RestablecerClaveDTO dtoRestablecer(String correo, String codigo, String clave) {
        RestablecerClaveDTO dto = new RestablecerClaveDTO();
        dto.setCorreo(correo);
        dto.setCodigo(codigo);
        dto.setClaveNueva(clave);
        return dto;
    }

    private Usuario usuario(Long id, String correo, String nombre, Boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setCorreo(correo);
        usuario.setNombre(nombre);
        usuario.setActivo(activo);
        usuario.setContrasena("hash-viejo");
        return usuario;
    }

    private CodigoRecuperacion codigo(Long idUsuario, String codigo, LocalDateTime expira, Boolean usado) {
        CodigoRecuperacion c = new CodigoRecuperacion();
        c.setId(1L);
        c.setIdUsuario(idUsuario);
        c.setCodigo(codigo);
        c.setExpiraEn(expira);
        c.setUsado(usado);
        return c;
    }
}
