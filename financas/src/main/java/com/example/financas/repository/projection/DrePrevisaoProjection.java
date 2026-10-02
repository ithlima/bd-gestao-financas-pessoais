package com.example.financas.repository.projection;

import java.math.BigDecimal;

/**
 * Linha agregada da <b>DRE prevista</b>: soma dos valores previstos dos títulos,
 * agrupada por categoria e tipo.
 *
 * <p>É uma <i>projeção</i> — uma interface que o Spring Data preenche
 * diretamente a partir do resultado do {@code SELECT}. Isso evita trazer todas
 * as entidades para a memória só para somar valores.
 *
 * <p>{@code tipo} vem como {@code String} porque a consulta é SQL nativo: nesse
 * caminho o Spring Data entrega o valor cru da coluna ({@code ENUM} do MariaDB)
 * e não faz a conversão automática para o enum Java. A conversão é feita de
 * forma explícita pelo service, usando {@code TipoMovimentacao.valueOf(...)}.
 */
public interface DrePrevisaoProjection {

  /** Categoria que forma a linha da DRE. */
  Long getCategoriaId();

  String getCategoriaNome();

  String getTipo();

  /** Soma de {@code titulo.valor_previsto} do grupo. */
  BigDecimal getTotalPrevisto();
}
