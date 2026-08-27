package br.com.luansantos.pessoas.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

    Optional<Pessoa> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

    List<Pessoa> findByNomeContainingIgnoreCaseOrSobrenomeContainingIgnoreCaseOrderByNomeAsc(
            String nome, String sobrenome);

    List<Pessoa> findAllByOrderByNomeAsc();
}
