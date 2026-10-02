package com.example.financas.repository.projection;

import java.math.BigDecimal;

/**
 * Linha agregada da <b>DRE realizada</b>: soma dos valores efetivamente
 * movimentados, agrupada por categoria e tipo.
 *
 * <p>A fonte é <b>exclusivamente</b> a tabela {@code movimentacao}. Um título
 * pendente, ainda que vencido, jamais aparece aqui.
 *
 * <p>{@code tipo} vem como {@code String} porque a consulta é SQL nativo — ver
 * {@link DrePrevisaoProjection}.
 */
public interface DreRealizadoProjection {

  Long getCategoriaId();

  String getCategoriaNome();

  String getTipo();

  /** Soma de {@code movimentacao.valor} do grupo. */
  BigDecimal getTotalRealizado();
}
