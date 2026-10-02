package com.example.financas.dto.response;

import com.example.financas.entity.SituacaoTituloEfetiva;
import com.example.financas.entity.TipoMovimentacao;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DreTituloVencidoResponse(
    Long idTitulo,
    String descricao,
    BigDecimal valorPrevisto,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate dataVencimento,
    long diasDeAtraso,
    TipoMovimentacao tipo,
    String categoriaNome,
    SituacaoTituloEfetiva situacaoEfetiva) {}
