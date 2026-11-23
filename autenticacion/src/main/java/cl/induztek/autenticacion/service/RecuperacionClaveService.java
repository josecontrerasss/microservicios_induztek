package cl.induztek.autenticacion.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.autenticacion.exception.SolicitudInvalidaException;
import cl.induztek.autenticacion.model.CodigoRecuperacion;
import cl.induztek.autenticacion.model.RecuperarClaveDTO;
import cl.induztek.autenticacion.model.RestablecerClaveDTO;
import cl.induztek.autenticacion.model.Usuario;
import cl.induztek.autenticacion.repository.CodigoRecuperacionRepository;
import cl.induztek.autenticacion.repository.UsuarioRepository;

@Service
public class RecuperacionClaveService {
    private final UsuarioRepository usuarioRepository;
    private final CodigoRecuperacionRepository codigoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecureRandom aleatorio = new SecureRandom();
    private final long minutosValidez;
    private final String remitente;

    public RecuperacionClaveService(UsuarioRepository usuarioRepository, CodigoRecuperacionRepository codigoRepository,
            PasswordEncoder passwordEncoder, JavaMailSender mailSender,
            @Value("${recuperacion.minutos-validez}") long minutosValidez,
            @Value("${recuperacion.remitente}") String remitente) {
        this.usuarioRepository = usuarioRepository;
        this.codigoRepository = codigoRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.minutosValidez = minutosValidez;
        this.remitente = remitente.isBlank() ? "no-responder@induztek.cl" : remitente;
    }

    @Transactional
    public void solicitarCodigo(RecuperarClaveDTO dto) {
        usuarioRepository.findByCorreo(dto.getCorreo().trim().toLowerCase())
                .filter(usuario -> Boolean.TRUE.equals(usuario.getActivo()))
                .ifPresent(this::enviarCodigo);
    }

    @Transactional
    public void restablecerClave(RestablecerClaveDTO dto) {
        Usuario usuario = usuarioRepository.findByCorreo(dto.getCorreo().trim().toLowerCase())
                .orElseThrow(() -> new SolicitudInvalidaException("Código inválido o expirado"));
        CodigoRecuperacion codigo = codigoRepository.findFirstByIdUsuarioAndUsadoFalseOrderByIdDesc(usuario.getId())
                .filter(c -> c.getCodigo().equals(dto.getCodigo()))
                .filter(c -> c.getExpiraEn().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new SolicitudInvalidaException("Código inválido o expirado"));
        codigo.setUsado(true);
        codigoRepository.save(codigo);
        usuario.setContrasena(passwordEncoder.encode(dto.getClaveNueva()));
        usuarioRepository.save(usuario);
    }

    private void enviarCodigo(Usuario usuario) {
        CodigoRecuperacion codigo = new CodigoRecuperacion();
        codigo.setIdUsuario(usuario.getId());
        codigo.setCodigo(String.format("%06d", aleatorio.nextInt(1_000_000)));
        codigo.setExpiraEn(LocalDateTime.now().plusMinutes(minutosValidez));
        codigo.setUsado(false);
        codigoRepository.save(codigo);

        SimpleMailMessage correo = new SimpleMailMessage();
        correo.setFrom(remitente);
        correo.setTo(usuario.getCorreo());
        correo.setSubject("Induztek - Código para restablecer tu contraseña");
        correo.setText("Hola " + usuario.getNombre() + ",\n\n"
                + "Tu código para restablecer la contraseña es: " + codigo.getCodigo() + "\n\n"
                + "Vence en " + minutosValidez + " minutos. Si no lo pediste, ignora este correo.");
        mailSender.send(correo);
    }
}
