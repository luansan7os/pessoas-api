package br.com.luansantos.pessoas.integration;

import br.com.luansantos.pessoas.exception.ServicoExternoIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Locale;

/**
 * Cliente da API publica de previsao de nacionalidade.
 *
 * Isolado numa classe propria para que o resto do sistema nao saiba que existe
 * um HTTP externo no caminho -- e para que o teste possa trocar por um mock.
 *
 * Dois cuidados com o fato de a API ser publica e gratuita:
 *
 * 1. Ela limita 25 chamadas por dia POR IP (cabecalho x-rate-limit-limit).
 *    Em hospedagem compartilhada o IP de saida e o mesmo para varios clientes,
 *    entao a cota pode ja chegar estourada. Por isso o resultado fica em cache:
 *    consultar dez vezes a mesma pessoa gasta uma chamada, nao dez.
 *
 * 2. Se houver chave de API configurada, ela vai no cabecalho e o limite sobe.
 *    Sem chave, funciona igual -- so com a cota menor.
 */
@Component
public class NationalizeClient {

    private static final Logger log = LoggerFactory.getLogger(NationalizeClient.class);

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;

    public NationalizeClient(RestClient nationalizeRestClient,
                             @Value("${app.nationalize.base-url}") String baseUrl,
                             @Value("${app.nationalize.api-key:}") String apiKey) {
        this.restClient = nationalizeRestClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /**
     * O cache guarda por nome normalizado. So resultado bom entra: excecao nao
     * e cacheada, entao uma falha momentanea nao fica grudada na resposta.
     */
    @Cacheable(cacheNames = "previsaoDeNacionalidade", key = "#nome.toLowerCase()")
    public NationalizeResponse preverPor(String nome) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("name", nome);

        // A chave vai como parametro de URL, e nao como cabecalho: esta API so
        // reconhece 'apikey' na query string -- cabecalho ela ignora em silencio,
        // respondendo como se a requisicao fosse anonima.
        if (temChave()) {
            uriBuilder.queryParam("apikey", apiKey);
        }

        String uri = uriBuilder.build().toUriString();

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

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            log.warn("A nationalize.io respondeu {} para o nome '{}'", status, nome);
            throw new ServicoExternoIndisponivelException(mensagemPara(status), status, e);

        } catch (RestClientException e) {
            log.warn("Resposta ilegivel da nationalize.io para o nome '{}': {}", nome, e.getMessage());
            throw new ServicoExternoIndisponivelException(
                    "O servico de previsao de nacionalidade devolveu uma resposta invalida", e);
        }
    }

    private boolean temChave() {
        return apiKey != null && !apiKey.isBlank();
    }

    private String mensagemPara(int status) {
        if (status == 429) {
            return temChave()
                    ? "A cota da chave de API de nacionalidade foi esgotada."
                    : "O limite diario da API publica de nacionalidade foi atingido "
                            + "(25 consultas por dia, contadas por IP). Ela volta a responder "
                            + "quando a cota reiniciar, ou imediatamente com uma chave de API configurada.";
        }
        if (status == 401 || status == 403) {
            return "A API de previsao de nacionalidade recusou a chave configurada. "
                    + "Confira o valor de APP_NATIONALIZE_API_KEY.";
        }
        return String.format(Locale.ROOT,
                "O servico de previsao de nacionalidade retornou erro %d", status);
    }
}
