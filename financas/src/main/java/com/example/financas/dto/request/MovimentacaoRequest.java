package com.example.financas.dto.request;

import com.example.financas.entity.TipoMovimentacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimentacaoRequest(
    @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String descricao,
    @NotNull(message = "O valor é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        BigDecimal valor,
    @NotNull(message = "O tipo é obrigatório") TipoMovimentacao tipo,
    @NotNull(message = "A data é obrigatória") LocalDate data,
    @NotNull(message = "A categoria é obrigatória") Long categoriaId,
    @NotNull(message = "A conta é obrigatória") Long contaId,
    @NotNull(message = "O usuário é obrigatório") Long usuarioId) {}
