package com.example.financas.dto.response;

import com.example.financas.entity.TipoMovimentacao;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representação pública de uma <b>movimentação</b> — um evento financeiro
 * realizado.
 *
 * <p>{@code tituloId} é nulo quando o lançamento é avulso (sem previsão de
 * origem). Os nomes de conta e categoria vêm "achatados" no JSON para que o
 * cliente não precise fazer chamadas adicionais só para exibir uma lista.
 */
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
