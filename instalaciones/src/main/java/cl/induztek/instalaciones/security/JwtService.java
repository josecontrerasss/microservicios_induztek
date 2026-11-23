package cl.induztek.instalaciones.security;

import java.nio.charset.StandardCharsets;

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

    public JwtService(@Value("${jwt.secreto}") String secreto) {
        this.llave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
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
