package br.com.luansantos.pessoas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Emissao e leitura do token JWT.
 *
 * HS256 com segredo em configuracao. Em producao o segredo vem de variavel de
 * ambiente (APP_JWT_SECRET) -- o valor do application.yml existe so para o
 * projeto subir com um comando na avaliacao.
 */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long validadeSegundos;
    private final String emissor;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.validade-segundos}") long validadeSegundos,
                      @Value("${app.jwt.emissor}") String emissor) {
        this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validadeSegundos = validadeSegundos;
        this.emissor = emissor;
    }

    public String gerar(String usuario, List<String> perfis) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .issuer(emissor)
                .subject(usuario)
                .claim("perfis", perfis)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(validadeSegundos)))
                .signWith(chave)
                .compact();
    }

    /**
     * Devolve as claims do token, ou null se ele for invalido, expirado ou
     * assinado com outra chave. Quem chama decide o que fazer com o null.
     */
    public Claims lerClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer(emissor)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public long getValidadeSegundos() {
        return validadeSegundos;
    }
}
