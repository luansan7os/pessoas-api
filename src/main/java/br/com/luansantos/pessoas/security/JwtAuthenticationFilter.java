package br.com.luansantos.pessoas.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Le o cabecalho Authorization: Bearer <token> e popula o contexto de
 * seguranca. Token ausente ou invalido nao lanca erro aqui -- deixa passar sem
 * autenticacao e o Spring Security decide se a rota exigia ou nao.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecalho != null && cabecalho.startsWith(PREFIXO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            Claims claims = jwtService.lerClaims(cabecalho.substring(PREFIXO.length()).trim());

            if (claims != null) {
                @SuppressWarnings("unchecked")
                List<String> perfis = claims.get("perfis", List.class);
                List<SimpleGrantedAuthority> autoridades = (perfis == null ? List.<String>of() : perfis)
                        .stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                var autenticacao = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, autoridades);
                autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(autenticacao);
            }
        }

        filterChain.doFilter(request, response);
    }
}
