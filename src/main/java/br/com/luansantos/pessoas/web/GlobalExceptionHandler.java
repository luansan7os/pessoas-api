package br.com.luansantos.pessoas.web;

import br.com.luansantos.pessoas.exception.DocumentoDuplicadoException;
import br.com.luansantos.pessoas.exception.PessoaNaoEncontradaException;
import br.com.luansantos.pessoas.exception.ServicoExternoIndisponivelException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Um unico lugar decide o formato de erro da API inteira.
 *
 * O corpo segue o RFC 7807 (ProblemDetail, nativo do Spring 6) com dois campos
 * extras: 'timestamp' e, quando ha erro de validacao, 'erros' campo a campo --
 * porque "400 Bad Request" sem dizer qual campo quebrou nao ajuda ninguem.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Erros de validacao do corpo da requisicao (@Valid no @RequestBody). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getObjectName(), erro.getDefaultMessage()));

        ProblemDetail problema = criar(HttpStatus.BAD_REQUEST,
                "Dados invalidos",
                "Um ou mais campos nao passaram na validacao. Veja o campo 'erros'.");
        problema.setProperty("erros", erros);

        return ResponseEntity.badRequest().body(problema);
    }

    /** JSON malformado ou tipo incompativel antes mesmo da validacao. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return ResponseEntity.badRequest().body(criar(HttpStatus.BAD_REQUEST,
                "Corpo da requisicao invalido",
                "Nao consegui ler o JSON enviado. Confira a sintaxe e os tipos dos campos."));
    }

    /**
     * Validacao em variavel de path e parametro de query (@Validated no controller).
     * E o que faz /list/123 responder 400 em vez de 404.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> tratarViolacaoDeConstraint(ConstraintViolationException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (ConstraintViolation<?> violacao : ex.getConstraintViolations()) {
            String caminho = violacao.getPropertyPath().toString();
            String campo = caminho.contains(".") ? caminho.substring(caminho.lastIndexOf('.') + 1) : caminho;
            erros.putIfAbsent(campo, violacao.getMessage());
        }

        ProblemDetail problema = criar(HttpStatus.BAD_REQUEST,
                "Parametro invalido",
                "O parametro enviado na URL nao passou na validacao. Veja o campo 'erros'.");
        problema.setProperty("erros", erros);

        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(PessoaNaoEncontradaException.class)
    public ResponseEntity<ProblemDetail> tratarPessoaNaoEncontrada(PessoaNaoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(criar(HttpStatus.NOT_FOUND, "Pessoa nao encontrada", ex.getMessage()));
    }

    @ExceptionHandler(DocumentoDuplicadoException.class)
    public ResponseEntity<ProblemDetail> tratarDocumentoDuplicado(DocumentoDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(criar(HttpStatus.CONFLICT, "Documento ja registrado", ex.getMessage()));
    }

    /** Falha do fornecedor externo nao pode virar 500 nosso. */
    @ExceptionHandler(ServicoExternoIndisponivelException.class)
    public ResponseEntity<ProblemDetail> tratarServicoExterno(ServicoExternoIndisponivelException ex) {
        ProblemDetail problema = criar(HttpStatus.BAD_GATEWAY,
                "Servico externo indisponivel", ex.getMessage());
        problema.setProperty("fornecedor", "api.nationalize.io");

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problema);
    }

    /** Rede de seguranca: qualquer coisa nao prevista vira 500 padronizado e logado. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> tratarInesperado(Exception ex) {
        log.error("Erro nao tratado", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(criar(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Erro interno",
                        "Algo quebrou do nosso lado. O erro foi registrado no log da aplicacao."));
    }

    private ProblemDetail criar(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setProperty("timestamp", Instant.now());
        return problema;
    }
}
