package br.com.luansantos.pessoas.dto;

import br.com.luansantos.pessoas.validation.Cpf;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST /registrarName.
 *
 * Concentra as validacoes de tipo de dado do cadastro:
 * documento com digito verificador, nome e sobrenome apenas com letras,
 * e-mail em formato valido e nenhum campo nulo ou em branco.
 */
@Schema(description = "Dados para registrar uma pessoa")
public record PessoaRequest(

        @Schema(example = "529.982.247-25", description = "CPF com ou sem mascara")
        @NotBlank(message = "documento e obrigatorio")
        @Cpf
        String documento,

        @Schema(example = "Nathaniel")
        @NotBlank(message = "nome e obrigatorio")
        @Size(min = 2, max = 60, message = "nome deve ter entre 2 e 60 caracteres")
        @Pattern(
                regexp = "^\\p{L}[\\p{L}'\\-\\s]*$",
                message = "nome deve conter apenas letras, espaco, hifen ou apostrofo")
        String nome,

        @Schema(example = "Santos")
        @NotBlank(message = "sobrenome e obrigatorio")
        @Size(min = 2, max = 60, message = "sobrenome deve ter entre 2 e 60 caracteres")
        @Pattern(
                regexp = "^\\p{L}[\\p{L}'\\-\\s]*$",
                message = "sobrenome deve conter apenas letras, espaco, hifen ou apostrofo")
        String sobrenome,

        @Schema(example = "nathaniel@exemplo.com.br")
        @NotBlank(message = "email e obrigatorio")
        @Size(max = 120, message = "email deve ter no maximo 120 caracteres")
        @Email(message = "email em formato invalido")
        String email
) {
}
