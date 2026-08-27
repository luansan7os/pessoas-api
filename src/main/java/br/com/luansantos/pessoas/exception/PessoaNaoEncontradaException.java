package br.com.luansantos.pessoas.exception;

public class PessoaNaoEncontradaException extends RuntimeException {

    public PessoaNaoEncontradaException(String documento) {
        super("Nenhuma pessoa registrada com o documento " + documento);
    }
}
