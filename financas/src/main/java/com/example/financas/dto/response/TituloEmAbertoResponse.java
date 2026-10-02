package com.example.financas.dto.response;

import com.example.financas.entity.TipoMovimentacao;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TituloEmAbertoResponse(
    Long idTitulo,
    String descricao,
    TipoMovimentacao tipo,
    BigDecimal valorPrevisto,
    BigDecimal valorRealizado,
    BigDecimal valorEmAberto,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate dataVencimento,

    long diasParaVencimento,
    SituacaoEmAberto situacao,
    String categoriaNome,
    String observacao) {

  public boolean estaVencido() {
    return situacao == SituacaoEmAberto.VENCIDO || situacao == SituacaoEmAberto.PARCIAL_VENCIDO;
  }

  public boolean isParcial() {
    return situacao == SituacaoEmAberto.PARCIAL || situacao == SituacaoEmAberto.PARCIAL_VENCIDO;
  }
}
