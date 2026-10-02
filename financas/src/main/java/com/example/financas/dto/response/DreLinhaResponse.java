package com.example.financas.dto.response;

import com.example.financas.entity.TipoMovimentacao;

import java.math.BigDecimal;

public record DreLinhaResponse(
    Long categoriaId,
    String categoriaNome,
    TipoMovimentacao tipo,
    BigDecimal valorPrevisto,
    BigDecimal valorRealizado,
    BigDecimal variacao) {}
