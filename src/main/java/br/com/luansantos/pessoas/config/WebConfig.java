package br.com.luansantos.pessoas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Deixa a pagina de explicacao acessivel em /explicacao, sem o .html no fim.
 *
 * E so um apelido: o arquivo continua sendo o estatico explicacao.html. Serve
 * para o endereco que vai no e-mail e na apresentacao ficar limpo.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/explicacao").setViewName("forward:/explicacao.html");
    }
}
