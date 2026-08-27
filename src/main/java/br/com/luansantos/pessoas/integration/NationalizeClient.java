package br.com.luansantos.pessoas.integration;

import br.com.luansantos.pessoas.exception.ServicoExternoIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Cliente da API publica de previsao de nacionalidade.
 *
 * Isolado numa classe propria para que o resto do sistema nao saiba que existe
 * um HTTP externo no caminho -- e para que o teste possa trocar por um mock.
 */
@Component
public class NationalizeClient {

    private static final Logger log = LoggerFactory.getLogger(NationalizeClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public NationalizeClient(RestClient nationalizeRestClient,
                             @Value("${app.nationalize.base-url}") String baseUrl) {
        this.restClient = nationalizeRestClient;
        this.baseUrl = baseUrl;
    }

    public NationalizeResponse preverPor(String nome) {
        String uri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("name", nome)
                .build()
                .toUriString();
        try {
            NationalizeResponse resposta = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(NationalizeResponse.class);

            return resposta == null ? new NationalizeResponse(nome, 0, List.of()) : resposta;

        } catch (ResourceAccessException e) {
            log.warn("Timeout ou falha de rede ao consultar a nationalize.io para o nome '{}'", nome);
            throw new ServicoExternoIndisponivelException(
                    "O servico de previsao de nacionalidade nao respondeu a tempo", e);

        } catch (RestClientException e) {
            log.warn("Resposta invalida da nationalize.io para o nome '{}': {}", nome, e.getMessage());
            throw new ServicoExternoIndisponivelException(
                    "O servico de previsao de nacionalidade retornou um erro", e);
        }
    }
}
