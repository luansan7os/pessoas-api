package br.com.luansantos.pessoas.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Conversao do codigo ISO no nome da nacionalidade")
class NacionalidadeServiceTest {

    @ParameterizedTest
    @CsvSource({
            "BR, Brasil",
            "US, Estados Unidos",
            "PT, Portugal",
            "JP, Japão",
            "UG, Uganda",
            "br, Brasil"
    })
    @DisplayName("traduz o codigo ISO para o nome do pais em portugues")
    void traduzCodigoIso(String codigo, String esperado) {
        assertThat(NacionalidadeService.nomeDoPais(codigo)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("devolve o proprio codigo quando ele nao existe na base de locales")
    void devolveCodigoQuandoDesconhecido() {
        assertThat(NacionalidadeService.nomeDoPais("ZZ")).isEqualTo("ZZ");
    }

    @Test
    @DisplayName("devolve nulo para codigo ausente")
    void devolveNuloParaCodigoAusente() {
        assertThat(NacionalidadeService.nomeDoPais(null)).isNull();
        assertThat(NacionalidadeService.nomeDoPais("  ")).isNull();
    }
}
