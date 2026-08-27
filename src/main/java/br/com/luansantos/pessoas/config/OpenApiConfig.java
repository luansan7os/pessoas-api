package br.com.luansantos.pessoas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Pessoas e Previsao de Nacionalidade")
                        .version("1.0.0")
                        .description("""
                                Cadastro de pessoas com consulta da nacionalidade provavel.

                                Como testar por aqui:
                                1. Chame POST /auth/login com as credenciais que acompanham a entrega
                                2. Copie o campo token da resposta
                                3. Clique em Authorize (cadeado) e cole o token
                                4. As demais rotas passam a responder

                                As credenciais estao no README do projeto, nao na interface.
                                """)
                        .contact(new Contact().name("Luan Santos")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA))
                .components(new Components().addSecuritySchemes(ESQUEMA,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtido em POST /auth/login")));
    }
}
