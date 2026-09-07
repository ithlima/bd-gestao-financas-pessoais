package com.example.financas.repository;

import com.example.financas.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    List<Pagamento> findByMovimentacaoIdMovimentacao(Long movimentacaoId);

    List<Pagamento> findByContaIdConta(Long contaId);
}