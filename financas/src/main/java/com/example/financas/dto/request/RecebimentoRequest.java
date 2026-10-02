package com.example.financas.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecebimentoRequest(
    @NotNull(message = "O valor é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        BigDecimal valor,
    @NotNull(message = "A data do recebimento é obrigatória") LocalDate data,
    @NotNull(message = "A conta é obrigatória") Long contaId) {}
