package com.example.financas.dto.response;

import com.example.financas.entity.TipoMovimentacao;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Um título que <b>ainda tem valor em aberto</b> — ou seja, uma conta a pagar ou
 * uma conta a receber.
 *
 * <p><b>Por que este DTO existe separado de {@code TituloResponse}.</b> O
 * {@code TituloResponse} descreve o título inteiro (valor previsto, histórico de
 * quitação, situação armazenada e derivada). Este DTO responde a uma pergunta
 * diferente e mais estreita: <i>"o que ainda está em aberto, quanto falta e
 * quando vence?"</i>. Os campos que não interessam a essa pergunta não aparecem —
 * e o mais importante deles é {@code valorEmAberto}, que é o número que importa
 * em contas a pagar e a receber, não o valor originalmente previsto.
 *
 * <p>Para o total do resumo, o que é somado é {@code valorEmAberto}. Um aluguel
 * de R$ 1.200,00 com R$ 600,00 já pagos contribui com <b>R$ 600,00</b> para as
 * contas a pagar, e não com R$ 1.200,00. Somar o valor previsto inflaria a
 * dívida.
 */
public record TituloEmAbertoResponse(
    Long idTitulo,
    String descricao,
    TipoMovimentacao tipo,
    BigDecimal valorPrevisto,
    BigDecimal valorRealizado,
    BigDecimal valorEmAberto,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate dataVencimento,
    /** Dias até o vencimento; negativo quando já venceu. */
    long diasParaVencimento,
    SituacaoEmAberto situacao,
    String categoriaNome,
    String observacao) {

  /**
   * O título já passou do vencimento.
   *
   * <p>É derivado de {@link #situacao} em vez de ser mais um campo: manter as duas
   * informações separadas permitiria que discordassem uma da outra.
   */
  public boolean estaVencido() {
    return situacao == SituacaoEmAberto.VENCIDO || situacao == SituacaoEmAberto.PARCIAL_VENCIDO;
  }

  /** Já houve algum pagamento, mas o título ainda não foi quitado. */
  public boolean isParcial() {
    return situacao == SituacaoEmAberto.PARCIAL || situacao == SituacaoEmAberto.PARCIAL_VENCIDO;
  }
}
