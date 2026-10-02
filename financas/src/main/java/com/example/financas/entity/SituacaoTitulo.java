package com.example.financas.entity;

/**
 * Situação ARMAZENADA de um título.
 *
 * <p>Repare que <b>VENCIDO não está aqui</b>. Isso é intencional.
 *
 * <p>A situação guardada no banco representa uma <i>decisão</i> ou uma
 * <i>consequência de um pagamento</i>:
 *
 * <ul>
 *   <li>{@link #PENDENTE} — o título existe e ainda não foi quitado;</li>
 *   <li>{@link #PAGO} — o título foi <b>quitado</b> (soma das movimentações
 *       igual ao valor previsto);</li>
 *   <li>{@link #CANCELADO} — o compromisso deixou de existir.</li>
 * </ul>
 *
 * <p>Já "vencido" é apenas uma <b>consequência do tempo</b>: um título pendente
 * cujo vencimento já passou. Se gravássemos VENCIDO no banco, precisaríamos de
 * uma rotina agendada varrendo a tabela toda madrugada. Se ela falhasse, a
 * situação ficaria mentindo. Por isso ele é <b>derivado em tempo de leitura</b>,
 * pelo método {@link Titulo#getSituacaoEfetiva()}.
 */
public enum SituacaoTitulo {
  PENDENTE,
  PAGO,
  CANCELADO
}
