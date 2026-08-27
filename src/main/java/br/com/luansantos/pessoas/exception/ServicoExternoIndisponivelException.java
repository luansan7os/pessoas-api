package br.com.luansantos.pessoas.exception;

/**
 * A API de previsao de nacionalidade e de terceiro. Se ela cair, sai um 502
 * explicito no lugar de um 500 generico -- quem consome precisa distinguir
 * "erro meu" de "erro do fornecedor".
 */
public class ServicoExternoIndisponivelException extends RuntimeException {

    public ServicoExternoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
