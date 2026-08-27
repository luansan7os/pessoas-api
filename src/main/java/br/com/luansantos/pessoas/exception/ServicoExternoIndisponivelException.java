package br.com.luansantos.pessoas.exception;

/**
 * A API de previsao de nacionalidade e de terceiro. Se ela cair, sai um 502
 * explicito no lugar de um 500 generico -- quem consome precisa distinguir
 * "erro meu" de "erro do fornecedor".
 *
 * Carrega o status que o fornecedor devolveu, quando houve um. Sem isso, todo
 * problema externo vira a mesma mensagem opaca e ninguem descobre se foi limite
 * de uso, indisponibilidade ou timeout sem ir ler log de servidor.
 */
public class ServicoExternoIndisponivelException extends RuntimeException {

    /** Limite diario de uso da API publica estourado (HTTP 429). */
    private static final int LIMITE_EXCEDIDO = 429;

    private final Integer statusFornecedor;

    public ServicoExternoIndisponivelException(String mensagem, Throwable causa) {
        this(mensagem, null, causa);
    }

    public ServicoExternoIndisponivelException(String mensagem, Integer statusFornecedor, Throwable causa) {
        super(mensagem, causa);
        this.statusFornecedor = statusFornecedor;
    }

    public Integer getStatusFornecedor() {
        return statusFornecedor;
    }

    public boolean isLimiteDeUsoExcedido() {
        return statusFornecedor != null && statusFornecedor == LIMITE_EXCEDIDO;
    }
}
