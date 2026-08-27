package br.com.luansantos.pessoas.web;

import br.com.luansantos.pessoas.integration.NationalizeResponse;
import br.com.luansantos.pessoas.exception.ServicoExternoIndisponivelException;
import br.com.luansantos.pessoas.integration.NationalizeClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de ponta a ponta das rotas da prova: autenticacao, validacao, contrato
 * de erro e conversao do codigo ISO.
 *
 * A API externa e substituida por um dublê -- teste que depende de internet nao
 * e teste, e loteria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("API de pessoas")
class PessoaApiIntegrationTest {

    /** CPF que a carga inicial (data.sql) ja registrou. */
    private static final String CPF_SEMEADO = "529.982.247-25";

    /** CPF valido e propositalmente ausente da carga inicial. */
    private static final String CPF_LIVRE = "168.995.350-09";

    /** Formato certo, digito verificador errado. */
    private static final String CPF_INVALIDO = "111.444.777-99";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NationalizeClient nationalizeClient;

    /* ---------------- autenticacao ---------------- */

    @Test
    @DisplayName("sem token, qualquer rota de negocio responde 401")
    void semTokenResponde401() throws Exception {
        mockMvc.perform(get("/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Nao autenticado"));
    }

    @Test
    @DisplayName("credencial errada responde 401 sem revelar se o usuario existe")
    void credencialErradaResponde401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("usuario", "admin", "senha", "errada"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login valido devolve token e perfis")
    void loginValidoDevolveToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("usuario", "admin", "senha", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.perfis").isArray());
    }

    /* ---------------- cadastro ---------------- */

    @Test
    @DisplayName("registra pessoa valida e devolve 201 com Location")
    void registraPessoaValida() throws Exception {
        mockMvc.perform(post("/registrarName")
                        .header("Authorization", "Bearer " + tokenDe("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "documento", CPF_LIVRE,
                                "nome", "Akira",
                                "sobrenome", "Yamamoto",
                                "email", "akira@exemplo.com.br"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.documento").value(CPF_LIVRE))
                .andExpect(jsonPath("$.nomeCompleto").value("Akira Yamamoto"));
    }

    @Test
    @DisplayName("CPF com digito verificador errado responde 400 detalhando o campo")
    void cpfInvalidoResponde400() throws Exception {
        mockMvc.perform(post("/registrarName")
                        .header("Authorization", "Bearer " + tokenDe("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "documento", CPF_INVALIDO,
                                "nome", "Akira",
                                "sobrenome", "Yamamoto",
                                "email", "akira@exemplo.com.br"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados invalidos"))
                .andExpect(jsonPath("$.erros.documento").isNotEmpty());
    }

    @Test
    @DisplayName("e-mail fora de formato e nome com numero respondem 400")
    void emailENomeInvalidosRespondem400() throws Exception {
        mockMvc.perform(post("/registrarName")
                        .header("Authorization", "Bearer " + tokenDe("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "documento", CPF_LIVRE,
                                "nome", "Akira 2",
                                "sobrenome", "Yamamoto",
                                "email", "isso-nao-e-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").isNotEmpty())
                .andExpect(jsonPath("$.erros.nome").isNotEmpty());
    }

    @Test
    @DisplayName("documento repetido responde 409")
    void documentoRepetidoResponde409() throws Exception {
        mockMvc.perform(post("/registrarName")
                        .header("Authorization", "Bearer " + tokenDe("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "documento", CPF_SEMEADO,
                                "nome", "Outra",
                                "sobrenome", "Pessoa",
                                "email", "outra@exemplo.com.br"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Documento ja registrado"));
    }

    /* ---------------- consulta ---------------- */

    @Test
    @DisplayName("lista as pessoas registradas")
    void listaPessoas() throws Exception {
        mockMvc.perform(get("/list")
                        .header("Authorization", "Bearer " + tokenDe("user", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].documento").isNotEmpty());
    }

    @Test
    @DisplayName("filtro de nome com uma letra so responde 400")
    void filtroCurtoResponde400() throws Exception {
        mockMvc.perform(get("/list").param("nome", "a")
                        .header("Authorization", "Bearer " + tokenDe("user", "user123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parametro invalido"));
    }

    @Test
    @DisplayName("busca pelo documento devolve a pessoa")
    void buscaPorDocumento() throws Exception {
        mockMvc.perform(get("/list/{documento}", CPF_SEMEADO)
                        .header("Authorization", "Bearer " + tokenDe("user", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nathaniel"));
    }

    @Test
    @DisplayName("documento malformado no path responde 400, nao 404")
    void documentoMalformadoResponde400() throws Exception {
        mockMvc.perform(get("/list/{documento}", "123")
                        .header("Authorization", "Bearer " + tokenDe("user", "user123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.documento").isNotEmpty());
    }

    @Test
    @DisplayName("documento valido e nao registrado responde 404")
    void documentoNaoRegistradoResponde404() throws Exception {
        mockMvc.perform(get("/list/{documento}", CPF_LIVRE)
                        .header("Authorization", "Bearer " + tokenDe("user", "user123")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Pessoa nao encontrada"));
    }

    /* ---------------- exclusao ---------------- */

    @Test
    @DisplayName("perfil USER nao pode excluir: 403")
    void usuarioComumNaoExclui() throws Exception {
        mockMvc.perform(delete("/list/{documento}", CPF_SEMEADO)
                        .header("Authorization", "Bearer " + tokenDe("user", "user123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));
    }

    @Test
    @DisplayName("perfil ADMIN exclui e a pessoa some da consulta")
    void adminExclui() throws Exception {
        String token = tokenDe("admin", "admin123");

        mockMvc.perform(delete("/list/{documento}", CPF_SEMEADO)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/list/{documento}", CPF_SEMEADO)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    /* ---------------- nacionalidade ---------------- */

    @Test
    @DisplayName("converte o codigo ISO da API externa no nome da nacionalidade")
    void converteIsoParaNomeDoPais() throws Exception {
        given(nationalizeClient.preverPor(anyString())).willReturn(
                new NationalizeResponse("Nathaniel", 1200, List.of(
                        new NationalizeResponse.Pais("US", 0.42),
                        new NationalizeResponse.Pais("BR", 0.18))));

        mockMvc.perform(get("/findNacionalityByPerson/{documento}", CPF_SEMEADO)
                        .header("Authorization", "Bearer " + tokenDe("user", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nacionalidade").value("Estados Unidos"))
                .andExpect(jsonPath("$.codigoIso").value("US"))
                .andExpect(jsonPath("$.probabilidade").value(0.42))
                .andExpect(jsonPath("$.alternativas[0].nacionalidade").value("Brasil"));
    }

    @Test
    @DisplayName("nome sem previsao na API externa responde 200 com nacionalidade nula")
    void nomeSemPrevisaoResponde200() throws Exception {
        given(nationalizeClient.preverPor(anyString()))
                .willReturn(new NationalizeResponse("Nathaniel", 0, List.of()));

        mockMvc.perform(get("/findNacionalityByPerson/{documento}", CPF_SEMEADO)
                        .header("Authorization", "Bearer " + tokenDe("user", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nacionalidade").doesNotExist())
                .andExpect(jsonPath("$.alternativas").isEmpty());
    }

    @Test
    @DisplayName("API externa fora do ar responde 502, nao 500")
    void apiExternaForaDoArResponde502() throws Exception {
        given(nationalizeClient.preverPor(anyString()))
                .willThrow(new ServicoExternoIndisponivelException("timeout", new RuntimeException()));

        mockMvc.perform(get("/findNacionalityByPerson/{documento}", CPF_SEMEADO)
                        .header("Authorization", "Bearer " + tokenDe("user", "user123")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.fornecedor").value("api.nationalize.io"));
    }

    /* ---------------- rotas REST equivalentes ---------------- */

    @Test
    @DisplayName("GET /pessoas lista igual a GET /list")
    void rotaRestLista() throws Exception {
        mockMvc.perform(get("/pessoas")
                        .header("Authorization", "Bearer " + tokenDe("user", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].documento").isNotEmpty());
    }

    @Test
    @DisplayName("GET /pessoas/{documento} consulta igual a GET /list/{documento}")
    void rotaRestBusca() throws Exception {
        mockMvc.perform(get("/pessoas/{documento}", CPF_SEMEADO)
                        .header("Authorization", "Bearer " + tokenDe("user", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nathaniel"));
    }

    @Test
    @DisplayName("POST /pessoas registra igual a POST /registrarName")
    void rotaRestRegistra() throws Exception {
        mockMvc.perform(post("/pessoas")
                        .header("Authorization", "Bearer " + tokenDe("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "documento", CPF_LIVRE,
                                "nome", "Akira",
                                "sobrenome", "Yamamoto",
                                "email", "akira@exemplo.com.br"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documento").value(CPF_LIVRE));
    }

    @Test
    @DisplayName("GET /pessoas/{documento}/nacionalidade preve igual a rota do enunciado")
    void rotaRestNacionalidade() throws Exception {
        given(nationalizeClient.preverPor(anyString())).willReturn(
                new NationalizeResponse("Nathaniel", 1200, List.of(
                        new NationalizeResponse.Pais("US", 0.42))));

        mockMvc.perform(get("/pessoas/{documento}/nacionalidade", CPF_SEMEADO)
                        .header("Authorization", "Bearer " + tokenDe("user", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nacionalidade").value("Estados Unidos"));
    }

    @Test
    @DisplayName("a rota REST de exclusao tambem exige ADMIN: perfil USER leva 403")
    void rotaRestExclusaoTambemExigeAdmin() throws Exception {
        mockMvc.perform(delete("/pessoas/{documento}", CPF_SEMEADO)
                        .header("Authorization", "Bearer " + tokenDe("user", "user123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));
    }

    @Test
    @DisplayName("DELETE /pessoas/{documento} exclui quando o perfil e ADMIN")
    void rotaRestExclusaoComAdmin() throws Exception {
        String token = tokenDe("admin", "admin123");

        mockMvc.perform(delete("/pessoas/{documento}", CPF_SEMEADO)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/pessoas/{documento}", CPF_SEMEADO)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("as rotas REST tambem exigem token")
    void rotaRestExigeToken() throws Exception {
        mockMvc.perform(get("/pessoas"))
                .andExpect(status().isUnauthorized());
    }

    /* ---------------- apoio ---------------- */

    private String tokenDe(String usuario, String senha) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("usuario", usuario, "senha", senha))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode no = objectMapper.readTree(corpo);
        return no.get("token").asText();
    }

    private String json(Object valor) throws Exception {
        return objectMapper.writeValueAsString(valor);
    }
}
