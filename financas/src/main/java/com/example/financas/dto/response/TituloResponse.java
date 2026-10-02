package com.example.financas.dto.response;

import com.example.financas.entity.SituacaoTitulo;
import com.example.financas.entity.SituacaoTituloEfetiva;
import com.example.financas.entity.TipoMovimentacao;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TituloResponse(
    Long idTitulo,
    String descricao,
    BigDecimal valorPrevisto,
    BigDecimal valorRealizado,
    BigDecimal valorEmAberto,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate dataVencimento,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate dataPagamento,
    TipoMovimentacao tipo,
    SituacaoTitulo situacao,
    SituacaoTituloEfetiva situacaoEfetiva,
    boolean podeRegistrarPagamento,
    int quantidadeMovimentacoes,
    String observacao,
    Long categoriaId,
    String categoriaNome,
    Long usuarioId) {}
