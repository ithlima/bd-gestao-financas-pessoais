package com.example.financas.dto.response;

import com.example.financas.entity.TipoMovimentacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumoFinanceiroResponse(
    Long usuarioId,
    LocalDate dataReferencia,

    PosicaoTitulosResponse contasAPagar,

    PosicaoTitulosResponse contasAReceber,

    BigDecimal saldoPrevisto,

    boolean saldoPrevistoPositivo) {

  public BigDecimal getTotalAPagar() {
    return contasAPagar.totalEmAberto();
  }

  public BigDecimal getTotalAReceber() {
    return contasAReceber.totalEmAberto();
  }
}
