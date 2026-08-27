package br.com.luansantos.pessoas.config;

import br.com.luansantos.pessoas.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.net.URI;

/**
 * Autenticacao por JWT em toda a API, com um nivel a mais no ponto critico.
 *
 * Decisao de projeto (a prova permitia proteger apenas uma das APIs):
 *  - todas as rotas de negocio exigem token valido;
 *  - DELETE /list/{documento} exige, alem do token, o perfil ADMIN. E a unica
 *    operacao destrutiva e irreversivel do sistema, entao autenticar nao basta:
 *    precisa autorizar.
 *
 * Ficam abertas apenas: o login, a interface web, o Swagger e o console do H2.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index.html", "/app.js", "/styles.css", "/favicon.ico",
                                "/explicacao", "/explicacao.html", "/img/**",
                                "/auth/login",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/h2-console/**")
                        .permitAll()
                        // Os dois enderecos levam ao mesmo metodo: a exigencia de ADMIN
                        // precisa cobrir os dois, senao a rota REST vira porta dos fundos.
                        .requestMatchers(HttpMethod.DELETE, "/list/**", "/pessoas/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> escrever(res,
                                HttpStatus.UNAUTHORIZED,
                                "Nao autenticado",
                                "Envie um token valido no cabecalho Authorization: Bearer <token>. "
                                        + "Obtenha o token em POST /auth/login.",
                                req.getRequestURI()))
                        .accessDeniedHandler((req, res, e) -> escrever(res,
                                HttpStatus.FORBIDDEN,
                                "Acesso negado",
                                "Seu token e valido, mas o perfil nao permite esta operacao. "
                                        + "Excluir pessoa exige o perfil ADMIN.",
                                req.getRequestURI())))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Usuarios em memoria: o foco da prova e o mecanismo de autenticacao, nao a
     * gestao de usuarios. As senhas vem de configuracao e sao gravadas com BCrypt.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder,
                                                 @Value("${app.usuarios.admin.usuario}") String admin,
                                                 @Value("${app.usuarios.admin.senha}") String senhaAdmin,
                                                 @Value("${app.usuarios.padrao.usuario}") String padrao,
                                                 @Value("${app.usuarios.padrao.senha}") String senhaPadrao) {
        return new InMemoryUserDetailsManager(
                User.withUsername(admin)
                        .password(encoder.encode(senhaAdmin))
                        .roles("USER", "ADMIN")
                        .build(),
                User.withUsername(padrao)
                        .password(encoder.encode(senhaPadrao))
                        .roles("USER")
                        .build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    private void escrever(HttpServletResponse response, HttpStatus status,
                          String titulo, String detalhe, String caminho) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setInstance(URI.create(caminho));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), problema);
    }
}
