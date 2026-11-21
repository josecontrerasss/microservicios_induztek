package cl.induztek.autenticacion.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private final SecretKey llave;
    private final Duration validez;

    public JwtService(@Value("${jwt.secreto}") String secreto, @Value("${jwt.horas-validez}") long horas) {
        this.llave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.validez = Duration.ofHours(horas);
    }

    public String generarToken(Long idUsuario, String correo, String rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(idUsuario))
                .claim("correo", correo)
                .claim("rol", rol)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(validez)))
                .signWith(llave)
                .compact();
    }

    public UsuarioToken validarToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(llave).build().parseSignedClaims(token).getPayload();
            return new UsuarioToken(Long.valueOf(claims.getSubject()), claims.get("rol", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
