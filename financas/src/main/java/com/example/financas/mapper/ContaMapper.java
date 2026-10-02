package com.example.financas.mapper;

import com.example.financas.dto.response.ContaResponse;
import com.example.financas.entity.Conta;

import java.math.BigDecimal;

/**
 * Conversão entre a entidade {@code Conta} e seu DTO de resposta.
 *
 * <p>O saldo atual é recebido por parâmetro porque <b>não é um campo da
 * entidade</b> — ele é calculado a partir das movimentações
 * ({@code saldoInicial + receitas − despesas}). O mapper não deve ir ao banco
 * buscar esse valor: quem faz isso é o service. O mapper apenas monta a resposta.
 */
public final class ContaMapper {

  private ContaMapper() {
    // classe utilitária: não deve ser instanciada
  }

  /**
   * @param conta entidade já carregada
   * @param saldoAtual saldo calculado pelo service; quando {@code null}, assume o
   *     saldo inicial (conta ainda sem nenhuma movimentação)
   */
  public static ContaResponse toResponse(Conta conta, BigDecimal saldoAtual) {
    BigDecimal saldo = saldoAtual != null ? saldoAtual : conta.getSaldoInicial();
    return new ContaResponse(
        conta.getIdConta(),
        conta.getNome(),
        conta.getTipo(),
        conta.getSaldoInicial(),
        saldo,
        conta.getUsuario().getIdUsuario());
  }
}
