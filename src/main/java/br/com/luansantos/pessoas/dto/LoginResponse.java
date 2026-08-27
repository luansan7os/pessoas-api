package br.com.luansantos.pessoas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Token de acesso emitido apos autenticacao")
public record LoginResponse(
        String token,
        String tipo,
        long expiraEmSegundos,
        String usuario,
        List<String> perfis
) {
}
