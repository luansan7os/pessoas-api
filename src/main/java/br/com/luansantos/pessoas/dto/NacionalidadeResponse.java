package br.com.luansantos.pessoas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resposta do GET /findNacionalityByPerson/{documento}.
 *
 * A nationalize.io devolve codigo ISO ("BR"). O requisito pede o NOME da
 * nacionalidade, entao devolvemos os dois: o nome traduzido e o codigo de
 * origem, para nao esconder o dado bruto de quem consome.
 */
@Schema(description = "Previsao de nacionalidade de uma pessoa")
public record NacionalidadeResponse(

        @Schema(example = "529.982.247-25")
        String documento,

        @Schema(example = "Nathaniel Santos")
        String nomeCompleto,

        @Schema(example = "Nathaniel", description = "Nome enviado para a API de previsao")
        String nomeConsultado,

        @Schema(example = "Estados Unidos", description = "Nome da nacionalidade mais provavel")
        String nacionalidade,

        @Schema(example = "US", description = "Codigo ISO 3166-1 alfa-2 devolvido pela API")
        String codigoIso,

        @Schema(example = "0.42", description = "Probabilidade de 0 a 1")
        Double probabilidade,

        @Schema(description = "Demais nacionalidades previstas, da mais para a menos provavel")
        List<NacionalidadePrevista> alternativas
) {

    @Schema(description = "Nacionalidade alternativa prevista")
    public record NacionalidadePrevista(
            String nacionalidade,
            String codigoIso,
            Double probabilidade
    ) {
    }
}
