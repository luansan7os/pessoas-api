package br.com.luansantos.pessoas.service;

import br.com.luansantos.pessoas.domain.Pessoa;
import br.com.luansantos.pessoas.domain.PessoaRepository;
import br.com.luansantos.pessoas.dto.PessoaRequest;
import br.com.luansantos.pessoas.dto.PessoaResponse;
import br.com.luansantos.pessoas.exception.DocumentoDuplicadoException;
import br.com.luansantos.pessoas.exception.PessoaNaoEncontradaException;
import br.com.luansantos.pessoas.validation.CpfValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de cadastro de pessoa.
 *
 * Todo documento entra e sai daqui normalizado (so digitos), para que
 * "529.982.247-25" e "52998224725" sejam sempre a mesma pessoa.
 */
@Service
public class PessoaService {

    private final PessoaRepository repository;

    public PessoaService(PessoaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PessoaResponse registrar(PessoaRequest request) {
        String documento = CpfValidator.normalizar(request.documento());

        if (repository.existsByDocumento(documento)) {
            throw new DocumentoDuplicadoException(request.documento());
        }

        Pessoa pessoa = new Pessoa(
                documento,
                request.nome().trim(),
                request.sobrenome().trim(),
                request.email().trim().toLowerCase());

        return PessoaResponse.de(repository.save(pessoa));
    }

    @Transactional(readOnly = true)
    public List<PessoaResponse> listar(String filtroNome) {
        List<Pessoa> pessoas = (filtroNome == null || filtroNome.isBlank())
                ? repository.findAllByOrderByNomeAsc()
                : repository.findByNomeContainingIgnoreCaseOrSobrenomeContainingIgnoreCaseOrderByNomeAsc(
                        filtroNome.trim(), filtroNome.trim());

        return pessoas.stream().map(PessoaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public PessoaResponse buscarPorDocumento(String documento) {
        return PessoaResponse.de(buscarEntidade(documento));
    }

    @Transactional
    public void excluirPorDocumento(String documento) {
        Pessoa pessoa = buscarEntidade(documento);
        repository.delete(pessoa);
    }

    @Transactional(readOnly = true)
    public Pessoa buscarEntidade(String documento) {
        String normalizado = CpfValidator.normalizar(documento);
        return repository.findByDocumento(normalizado)
                .orElseThrow(() -> new PessoaNaoEncontradaException(documento));
    }
}
