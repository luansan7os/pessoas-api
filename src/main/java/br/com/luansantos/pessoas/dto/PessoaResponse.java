package br.com.luansantos.pessoas.dto;

import br.com.luansantos.pessoas.domain.Pessoa;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Representacao de saida da pessoa.
 *
 * A entidade JPA nunca sai do service: quem trafega na API e este record.
 * Assim, mudar o banco nao quebra o contrato publico.
 */
@Schema(description = "Pessoa registrada no sistema")
public record PessoaResponse(
        Long id,
        String documento,
        String nome,
        String sobrenome,
        String nomeCompleto,
        String email,
        Instant criadoEm
) {

    public static PessoaResponse de(Pessoa pessoa) {
        return new PessoaResponse(
                pessoa.getId(),
                formatarDocumento(pessoa.getDocumento()),
                pessoa.getNome(),
                pessoa.getSobrenome(),
                pessoa.getNomeCompleto(),
                pessoa.getEmail(),
                pessoa.getCriadoEm());
    }

    /** Guardamos so digitos; devolvemos com mascara para leitura humana. */
    private static String formatarDocumento(String digitos) {
        if (digitos == null || digitos.length() != 11) {
            return digitos;
        }
        return digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "."
                + digitos.substring(6, 9) + "-" + digitos.substring(9);
    }
}
