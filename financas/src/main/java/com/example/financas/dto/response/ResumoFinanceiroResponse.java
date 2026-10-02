package com.example.financas.dto.response;

import com.example.financas.entity.TipoMovimentacao;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Posição financeira consolidada do usuário: <b>o que ele deve</b> e <b>o que
 * tem a receber</b>, lado a lado.
 *
 * <p>É a resposta que responde à pergunta mais prática do dia a dia — <i>"como
 * está a minha situação?"</i> — combinando os dois lados em uma única chamada.
 *
 * <p>O campo mais útil aqui é {@code saldoPrevisto}: o que ainda vai entrar menos
 * o que ainda vai sair. Ele <b>não</b> considera o dinheiro que já está na conta;
 * para isso existe o saldo das contas
 * ({@code GET /api/v1/contas?usuarioId=}).
 *
 * <p><b>Por que não misturar os dois.</b> Somar saldo de conta com contas em
 * aberto produziria um número enganoso: o saldo bancário é passado (fatos
 * consumados) e as contas em aberto são futuro (previsões). Mantê-los separados
 * deixa claro o que já aconteceu e o que ainda está por acontecer.
 */
public record ResumoFinanceiroResponse(
    Long usuarioId,
    LocalDate dataReferencia,

    /** Contas a pagar: obrigações com valor ainda em aberto. */
    PosicaoTitulosResponse contasAPagar,

    /** Contas a receber: direitos com valor ainda em aberto. */
    PosicaoTitulosResponse contasAReceber,

    /** {@code totalAReceber − totalAPagar}. Negativo significa que falta dinheiro. */
    BigDecimal saldoPrevisto,

    /** {@code true} quando o que há a receber cobre o que há a pagar. */
    boolean saldoPrevistoPositivo) {

  /** Atalho de leitura: total que ainda deve sair. */
  public BigDecimal getTotalAPagar() {
    return contasAPagar.totalEmAberto();
  }

  /** Atalho de leitura: total que ainda deve entrar. */
  public BigDecimal getTotalAReceber() {
    return contasAReceber.totalEmAberto();
  }
}
