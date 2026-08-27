package br.com.luansantos.pessoas.web;

import br.com.luansantos.pessoas.dto.LoginRequest;
import br.com.luansantos.pessoas.dto.LoginResponse;
import br.com.luansantos.pessoas.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticacao", description = "Emissao do token de acesso")
@SecurityRequirements
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e devolve o token JWT",
            description = "Dois perfis: ADMIN (le, grava e exclui) e USER (le e grava, mas nao "
                    + "exclui). As credenciais acompanham a entrega, no README do projeto.")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication autenticacao = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.usuario(), request.senha()));

            List<String> perfis = autenticacao.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            String token = jwtService.gerar(autenticacao.getName(), perfis);

            return new LoginResponse(
                    token,
                    "Bearer",
                    jwtService.getValidadeSegundos(),
                    autenticacao.getName(),
                    perfis);

        } catch (BadCredentialsException e) {
            // Mensagem generica de proposito: nao revela se o usuario existe.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario ou senha invalidos");
        }
    }
}
