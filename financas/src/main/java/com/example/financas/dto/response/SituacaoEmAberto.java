package com.example.financas.dto.response;

/**
 * Situação de um título <b>do ponto de vista de contas a pagar / a receber</b>.
 *
 * <p>É uma leitura mais específica que {@code SituacaoTituloEfetiva}: em vez de
 * dizer apenas que o título está pendente, separa o que ainda está no prazo do
 * que já venceu, e distingue a quitação parcial.
 *
 * <p>Repare que {@code PAGO} e {@code CANCELADO} não aparecem: um título quitado
 * não é mais uma conta a pagar (nada está em aberto), e um cancelado nunca foi
 * obrigação de verdade. Os dois são <b>filtrados</b>, não rotulados.
 */
public enum SituacaoEmAberto {

  /** Pendente, ainda dentro do prazo de vencimento. */
  A_VENCER,

  /**
   * Pendente e já com vencimento ultrapassado.
   *
   * <p>Como todo estado "vencido" neste sistema, é <b>derivado</b> da comparação
   * entre a data de vencimento e a data de referência — nunca lido de uma coluna.
   */
  VENCIDO,

  /**
   * Parcialmente quitado e ainda dentro do prazo.
   *
   * <p>Título que já recebeu algum pagamento, mas não o suficiente para quitar.
   * Aparece com {@code valorEmAberto} menor que o valor previsto.
   */
  PARCIAL,

  /** Parcialmente quitado e com o vencimento ultrapassado. */
  PARCIAL_VENCIDO
}
