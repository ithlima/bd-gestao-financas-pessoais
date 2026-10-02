package com.example.financas.dto.response;

import com.example.financas.entity.TipoMovimentacao;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimentacaoResponse(
    Long idMovimentacao,
    String descricao,
    BigDecimal valor,
    TipoMovimentacao tipo,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate data,
    Long contaId,
    String contaNome,
    Long categoriaId,
    String categoriaNome,
    Long tituloId,
    Long usuarioId) {}
