package br.com.luansantos.pessoas.service;

import br.com.luansantos.pessoas.domain.Pessoa;
import br.com.luansantos.pessoas.dto.NacionalidadeResponse;
import br.com.luansantos.pessoas.dto.PessoaResponse;
import br.com.luansantos.pessoas.integration.NationalizeClient;
import br.com.luansantos.pessoas.integration.NationalizeResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Traduz a previsao da nationalize.io para o que o requisito pede.
 *
 * A API externa devolve o pais em codigo ISO 3166-1 alfa-2 ("US"). O requisito
 * pede o NOME da possivel nacionalidade, entao a conversao ISO -> nome acontece
 * aqui, usando a base de locales da propria JDK (nao precisa de dependencia
 * nem de tabela mantida a mao).
 */
@Service
public class NacionalidadeService {

    private static final Locale IDIOMA_SAIDA = Locale.of("pt", "BR");

    private final PessoaService pessoaService;
    private final NationalizeClient client;

    public NacionalidadeService(PessoaService pessoaService, NationalizeClient client) {
        this.pessoaService = pessoaService;
        this.client = client;
    }

    public NacionalidadeResponse preverPorDocumento(String documento) {
        Pessoa pessoa = pessoaService.buscarEntidade(documento);
        NationalizeResponse previsao = client.preverPor(pessoa.getNome());

        List<NationalizeResponse.Pais> paises = previsao.paises().stream()
                .filter(pais -> pais.countryId() != null)
                .sorted(Comparator.comparing(
                        NationalizeResponse.Pais::probability,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        String documentoFormatado = PessoaResponse.de(pessoa).documento();

        if (paises.isEmpty()) {
            // Nome sem previsao na base da API. Nao e erro: e ausencia de dado.
            return new NacionalidadeResponse(
                    documentoFormatado,
                    pessoa.getNomeCompleto(),
                    pessoa.getNome(),
                    null,
                    null,
                    null,
                    List.of());
        }

        NationalizeResponse.Pais principal = paises.getFirst();

        List<NacionalidadeResponse.NacionalidadePrevista> alternativas = paises.stream()
                .skip(1)
                .map(pais -> new NacionalidadeResponse.NacionalidadePrevista(
                        nomeDoPais(pais.countryId()),
                        pais.countryId(),
                        pais.probability()))
                .toList();

        return new NacionalidadeResponse(
                documentoFormatado,
                pessoa.getNomeCompleto(),
                pessoa.getNome(),
                nomeDoPais(principal.countryId()),
                principal.countryId(),
                principal.probability(),
                alternativas);
    }

    /** Codigos ISO 3166-1 alfa-2 que a JDK reconhece como pais de verdade. */
    private static final Set<String> PAISES_ISO = Set.of(Locale.getISOCountries());

    /**
     * "US" -> "Estados Unidos".
     *
     * Se o codigo nao for um pais ISO conhecido, devolve o proprio codigo em vez
     * de inventar um nome. A checagem contra a lista ISO e necessaria porque a
     * JDK traduz codigos reservados como "ZZ" para "Regiao desconhecida" -- o que
     * seria devolver um nome de pais que nao existe.
     */
    static String nomeDoPais(String codigoIso) {
        if (codigoIso == null || codigoIso.isBlank()) {
            return null;
        }
        String codigo = codigoIso.trim().toUpperCase(Locale.ROOT);

        if (!PAISES_ISO.contains(codigo)) {
            return codigo;
        }

        String nome = Locale.of("", codigo).getDisplayCountry(IDIOMA_SAIDA);

        if (nome == null || nome.isBlank() || nome.equals(codigo)) {
            String nomeEmIngles = Locale.of("", codigo).getDisplayCountry(Locale.ENGLISH);
            return (nomeEmIngles == null || nomeEmIngles.isBlank()) ? codigo : nomeEmIngles;
        }
        return nome;
    }
}
