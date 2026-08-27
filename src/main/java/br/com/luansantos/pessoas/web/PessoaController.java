package br.com.luansantos.pessoas.web;

import br.com.luansantos.pessoas.dto.NacionalidadeResponse;
import br.com.luansantos.pessoas.dto.PessoaRequest;
import br.com.luansantos.pessoas.dto.PessoaResponse;
import br.com.luansantos.pessoas.service.NacionalidadeService;
import br.com.luansantos.pessoas.service.PessoaService;
import br.com.luansantos.pessoas.validation.Cpf;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Rotas do enunciado da prova.
 *
 * O parametro identificador escolhido e o DOCUMENTO (CPF). Motivo: e chave
 * natural e unica da pessoa e permite uma validacao de tipo de dado de verdade
 * -- formato e digito verificador -- em vez de apenas checar se um id e numero.
 *
 * Os nomes das rotas (/registrarName, /list) foram mantidos exatamente como no
 * enunciado. Em projeto proprio eu usaria /pessoas no plural, mas requisito
 * escrito nao se reescreve por gosto.
 */
@RestController
@Validated
@Tag(name = "Pessoas", description = "Cadastro, consulta, exclusao e previsao de nacionalidade")
public class PessoaController {

    private final PessoaService pessoaService;
    private final NacionalidadeService nacionalidadeService;

    public PessoaController(PessoaService pessoaService, NacionalidadeService nacionalidadeService) {
        this.pessoaService = pessoaService;
        this.nacionalidadeService = nacionalidadeService;
    }

    @PostMapping("/registrarName")
    @Operation(summary = "Registra uma pessoa",
            description = "Valida CPF (formato + digito verificador), nome e sobrenome apenas com "
                    + "letras, e-mail em formato valido e nenhum campo nulo ou em branco.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pessoa registrada"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "409", description = "Documento ja registrado", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<PessoaResponse> registrar(@Valid @RequestBody PessoaRequest request,
                                                    UriComponentsBuilder uriBuilder) {
        PessoaResponse pessoa = pessoaService.registrar(request);

        URI local = uriBuilder.path("/list/{documento}")
                .buildAndExpand(pessoa.documento())
                .toUri();

        return ResponseEntity.created(local).body(pessoa);
    }

    @GetMapping("/list")
    @Operation(summary = "Lista as pessoas registradas",
            description = "Aceita o filtro opcional 'nome', validado com no minimo 2 caracteres "
                    + "para nao varrer a base inteira com uma letra solta.")
    public List<PessoaResponse> listar(
            @Parameter(description = "Filtro parcial por nome ou sobrenome", example = "nat")
            @RequestParam(required = false)
            @Size(min = 2, max = 60, message = "o filtro nome deve ter entre 2 e 60 caracteres")
            String nome) {

        return pessoaService.listar(nome);
    }

    @GetMapping("/list/{documento}")
    @Operation(summary = "Consulta uma pessoa pelo documento",
            description = "O CPF do path e validado antes de qualquer acesso ao banco: "
                    + "documento malformado responde 400, documento valido e inexistente responde 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pessoa encontrada"),
            @ApiResponse(responseCode = "400", description = "CPF invalido", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "404", description = "Pessoa nao encontrada", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public PessoaResponse buscar(
            @Parameter(description = "CPF com ou sem mascara", example = "529.982.247-25")
            @PathVariable @Cpf String documento) {

        return pessoaService.buscarPorDocumento(documento);
    }

    @DeleteMapping("/list/{documento}")
    @Operation(summary = "Exclui uma pessoa pelo documento",
            description = "Operacao destrutiva: exige token valido E perfil ADMIN. "
                    + "O CPF do path passa pela mesma validacao de formato e digito verificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pessoa excluida"),
            @ApiResponse(responseCode = "400", description = "CPF invalido", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao para excluir", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "404", description = "Pessoa nao encontrada", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<Void> excluir(
            @Parameter(description = "CPF com ou sem mascara", example = "529.982.247-25")
            @PathVariable @Cpf String documento) {

        pessoaService.excluirPorDocumento(documento);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/findNacionalityByPerson/{documento}")
    @Operation(summary = "Preve a nacionalidade da pessoa",
            description = "Consulta a api.nationalize.io pelo nome da pessoa e converte o codigo "
                    + "ISO 3166-1 alfa-2 devolvido pela API no NOME da nacionalidade.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Previsao obtida (nacionalidade nula quando a API nao tem previsao para o nome)"),
            @ApiResponse(responseCode = "400", description = "CPF invalido", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "404", description = "Pessoa nao encontrada", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "502", description = "API externa indisponivel ou fora do ar", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public NacionalidadeResponse preverNacionalidade(
            @Parameter(description = "CPF com ou sem mascara", example = "529.982.247-25")
            @PathVariable @Cpf String documento) {

        return nacionalidadeService.preverPorDocumento(documento);
    }
}
