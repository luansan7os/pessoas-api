package br.com.luansantos.pessoas.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache da previsao de nacionalidade.
 *
 * A API publica libera 25 consultas por dia por IP. Sem cache, abrir a tela e
 * clicar em "prever" nas quatro pessoas cinco vezes ja consome a cota do dia.
 * Com cache, cada nome custa uma chamada -- e nomes se repetem muito.
 *
 * E um mapa em memoria de proposito: cabe no escopo desta prova, nao exige
 * servico externo e some junto com a aplicacao. Num sistema com mais de uma
 * instancia isto viraria Redis, trocando esta classe e nada mais.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PREVISAO_DE_NACIONALIDADE = "previsaoDeNacionalidade";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(PREVISAO_DE_NACIONALIDADE);
    }
}
