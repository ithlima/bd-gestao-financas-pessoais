package com.example.financas.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferenciaRequest(
    @NotNull Long contaOrigemId,
    @NotNull Long contaDestinoId,
    @NotNull @Positive BigDecimal valor,
    @NotNull LocalDate data,
    String descricao) {}