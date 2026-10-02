package com.example.financas.service;

import com.example.financas.entity.Conta;
import com.example.financas.entity.TipoMovimentacao;
import com.example.financas.entity.Titulo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados de que se precisa para registrar um evento financeiro <b>originado de um
 * título</b>.
 *
 * <p>É um objeto de comando interno do domínio, não um DTO de API. Existe por um
 * motivo específico: quando a movimentação nasce da quitação de um título, o
 * {@code tipo}, a {@code categoria} e a {@code descricao} <b>não vêm do
 * cliente</b> — são herdados do título (RN16). Se o service aceitasse o
 * {@code MovimentacaoRequest} completo, haveria campos que o cliente poderia
 * mandar e que teriam de ser ignorados, ou pior, que poderiam divergir do título.
 *
 * <p>Aqui só existem os dados que o cliente realmente decide no momento do
 * pagamento: <b>quanto</b>, <b>quando</b> e <b>de qual conta</b>.
 */
public record NovaMovimentacao(Titulo titulo, BigDecimal valor, LocalDate data, Conta conta) {

  /** O tipo (e portanto a direção do dinheiro) é sempre o do título. */
  public TipoMovimentacao tipo() {
    return titulo.getTipo();
  }
}
