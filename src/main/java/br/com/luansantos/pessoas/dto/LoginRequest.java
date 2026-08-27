package br.com.luansantos.pessoas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de acesso")
public record LoginRequest(

        @Schema(description = "Usuario de acesso. Os perfis disponiveis estao no README.",
                example = "admin")
        @NotBlank(message = "usuario e obrigatorio")
        String usuario,

        // Sem 'example' de proposito: senha nao entra em documentacao publica.
        // Ela acompanha a entrega, no README do projeto.
        @Schema(description = "Senha do usuario. Informada junto com a entrega, no README.")
        @NotBlank(message = "senha e obrigatoria")
        String senha
) {
}
