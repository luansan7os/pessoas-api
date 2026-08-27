package br.com.luansantos.pessoas.exception;

public class DocumentoDuplicadoException extends RuntimeException {

    public DocumentoDuplicadoException(String documento) {
        super("Ja existe uma pessoa registrada com o documento " + documento);
    }
}
