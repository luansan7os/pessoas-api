package br.com.luansantos.pessoas.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Implementacao da checagem de CPF.
 *
 * A validacao vai alem do formato: confere os dois digitos verificadores pelo
 * modulo 11. Um "111.111.111-11" tem o formato certo e mesmo assim e recusado.
 */
public class CpfValidator implements ConstraintValidator<Cpf, String> {

    /**
     * Ou 11 digitos limpos, ou a mascara completa. Formato pela metade
     * ("529982247-25") e recusado: entrada ambigua nao entra no sistema.
     */
    private static final Pattern FORMATO =
            Pattern.compile("\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        if (!FORMATO.matcher(valor).matches()) {
            return false;
        }
        return isCpfValido(normalizar(valor));
    }

    /** Remove tudo que nao for digito. Ponto unico de normalizacao do documento. */
    public static String normalizar(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }

    public static boolean isCpfValido(String digitos) {
        if (digitos == null || digitos.length() != 11) {
            return false;
        }
        if (digitos.chars().distinct().count() == 1) {
            // 00000000000, 11111111111 ... passam no modulo 11 mas nao existem
            return false;
        }
        int primeiroDigito = calcularDigito(digitos, 9);
        int segundoDigito = calcularDigito(digitos, 10);
        return primeiroDigito == Character.getNumericValue(digitos.charAt(9))
                && segundoDigito == Character.getNumericValue(digitos.charAt(10));
    }

    private static int calcularDigito(String digitos, int posicao) {
        int soma = 0;
        int peso = posicao + 1;
        for (int i = 0; i < posicao; i++) {
            soma += Character.getNumericValue(digitos.charAt(i)) * peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
