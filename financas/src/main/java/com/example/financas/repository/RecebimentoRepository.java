package com.example.financas.repository;

import com.example.financas.entity.Recebimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecebimentoRepository extends JpaRepository<Recebimento, Long> {

    List<Recebimento> findByMovimentacaoIdMovimentacao(Long movimentacaoId);

    List<Recebimento> findByContaIdConta(Long contaId);
}