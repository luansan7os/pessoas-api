package br.com.luansantos.pessoas.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Espelho do JSON da https://api.nationalize.io/?name=X
 *
 * Exemplo:
 * {"count":5096,"name":"nathaniel","country":[{"country_id":"UG","probability":0.06}]}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NationalizeResponse(
        String name,
        Integer count,
        List<Pais> country
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pais(
            @JsonProperty("country_id") String countryId,
            Double probability
    ) {
    }

    public List<Pais> paises() {
        return country == null ? List.of() : country;
    }
}
