package com.example.financas.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferenciaResponse(
    Long idMovimentacaoOrigem,
    Long idMovimentacaoDestino,
    Long contaOrigemId,
    Long contaDestinoId,
    BigDecimal valor,
    LocalDate data,
    String descricao) {}