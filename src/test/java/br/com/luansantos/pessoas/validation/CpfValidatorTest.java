package br.com.luansantos.pessoas.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Validacao de CPF")
class CpfValidatorTest {

    private final CpfValidator validator = new CpfValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "529.982.247-25",
            "52998224725",
            "111.444.777-35",
            "12345678909",
            "390.533.447-05",
            "168.995.350-09"
    })
    @DisplayName("aceita CPF valido, com ou sem mascara")
    void aceitaCpfValido(String cpf) {
        assertThat(validator.isValid(cpf, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "529.982.247-26",   // digito verificador errado
            "111.111.111-11",   // todos os digitos iguais
            "000.000.000-00",
            "123456789",        // curto demais
            "123456789012",     // longo demais
            "abc.def.ghi-jk",   // nao numerico
            "529982247-25",     // mascara quebrada
            "   "
    })
    @DisplayName("recusa CPF invalido")
    void recusaCpfInvalido(String cpf) {
        assertThat(validator.isValid(cpf, null)).isFalse();
    }

    @Test
    @DisplayName("recusa nulo")
    void recusaNulo() {
        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    @DisplayName("normalizacao remove a pontuacao")
    void normalizaDocumento() {
        assertThat(CpfValidator.normalizar("529.982.247-25")).isEqualTo("52998224725");
        assertThat(CpfValidator.normalizar("52998224725")).isEqualTo("52998224725");
    }
}
