package br.com.luansantos.pessoas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Cliente HTTP da API externa, com timeout curto e explicito.
 *
 * Sem timeout, uma lentidao da nationalize.io prende as threads da nossa API.
 * Cinco segundos e o teto: passou disso, respondemos 502 e seguimos de pe.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient nationalizeRestClient(
            RestClient.Builder builder,
            @Value("${app.nationalize.timeout-conexao-ms}") long timeoutConexao,
            @Value("${app.nationalize.timeout-leitura-ms}") long timeoutLeitura) {

        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(timeoutConexao))
                .withReadTimeout(Duration.ofMillis(timeoutLeitura));

        return builder
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }
}
