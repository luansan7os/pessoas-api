package br.com.luansantos.pessoas.integration;

import br.com.luansantos.pessoas.exception.ServicoExternoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Testa como o cliente reage ao que a API externa devolve.
 *
 * O importante aqui nao e o caminho feliz: e o sistema saber distinguir
 * "estourei a cota" de "o fornecedor caiu" de "a rede travou".
 */
@DisplayName("Cliente da nationalize.io")
class NationalizeClientTest {

    private static final String BASE = "https://api.nationalize.io";
    private static final String URL_ESPERADA = BASE + "?name=nathaniel";

    private RestClient.Builder builder;
    private MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
        builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
    }

    private NationalizeClient clienteCom(String apiKey) {
        return new NationalizeClient(builder.build(), BASE, apiKey);
    }

    @Test
    @DisplayName("le a previsao quando a API responde bem")
    void leAPrevisao() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andRespond(withSuccess("""
                        {"count":10,"name":"nathaniel",
                         "country":[{"country_id":"US","probability":0.42}]}
                        """, MediaType.APPLICATION_JSON));

        NationalizeResponse resposta = clienteCom("").preverPor("nathaniel");

        assertThat(resposta.paises()).hasSize(1);
        assertThat(resposta.paises().getFirst().countryId()).isEqualTo("US");
        servidor.verify();
    }

    @Test
    @DisplayName("limite diario estourado (429) vira mensagem que explica o que houve")
    void limiteDiarioEstourado() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> clienteCom("").preverPor("nathaniel"))
                .isInstanceOf(ServicoExternoIndisponivelException.class)
                .hasMessageContaining("limite diario")
                .hasMessageContaining("25 consultas por dia")
                .extracting("statusFornecedor")
                .isEqualTo(429);
    }

    @Test
    @DisplayName("o status do fornecedor viaja junto na excecao")
    void guardaOStatusDoFornecedor() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> clienteCom("").preverPor("nathaniel"))
                .isInstanceOf(ServicoExternoIndisponivelException.class)
                .extracting("statusFornecedor")
                .isEqualTo(503);
    }

    @Test
    @DisplayName("falha de rede e reportada como timeout, sem status de fornecedor")
    void falhaDeRede() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andRespond(withException(new SocketTimeoutException("tempo esgotado")));

        assertThatThrownBy(() -> clienteCom("").preverPor("nathaniel"))
                .isInstanceOf(ServicoExternoIndisponivelException.class)
                .hasMessageContaining("nao respondeu a tempo")
                .extracting("statusFornecedor")
                .isNull();
    }

    /**
     * A chave desta API vai na query string, nao em cabecalho: mandada como
     * cabecalho, ela e ignorada em silencio e a requisicao conta como anonima.
     * Este teste existe para travar exatamente esse engano.
     */
    @Test
    @DisplayName("manda a chave como parametro apikey na URL, nao como cabecalho")
    void mandaChaveNaQueryString() {
        servidor.expect(requestTo(BASE + "?name=nathaniel&apikey=chave-secreta"))
                .andRespond(withSuccess("""
                        {"count":1,"name":"nathaniel","country":[]}
                        """, MediaType.APPLICATION_JSON));

        clienteCom("chave-secreta").preverPor("nathaniel");

        servidor.verify();
    }

    @Test
    @DisplayName("sem chave configurada, a URL nao carrega o parametro apikey")
    void naoMandaChaveQuandoAusente() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andRespond(withSuccess("""
                        {"count":1,"name":"nathaniel","country":[]}
                        """, MediaType.APPLICATION_JSON));

        clienteCom("").preverPor("nathaniel");

        servidor.verify();
    }

    @Test
    @DisplayName("chave recusada (401) aponta a variavel de ambiente a conferir")
    void chaveRecusada() {
        servidor.expect(requestTo(BASE + "?name=nathaniel&apikey=errada"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> clienteCom("errada").preverPor("nathaniel"))
                .isInstanceOf(ServicoExternoIndisponivelException.class)
                .hasMessageContaining("APP_NATIONALIZE_API_KEY")
                .extracting("statusFornecedor")
                .isEqualTo(401);
    }
}
