package com.example.financas.dto.request;

import com.example.financas.entity.TipoMovimentacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TituloRequest(
    @NotBlank(message = "A descrição do título é obrigatória")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String descricao,
    @NotNull(message = "O valor previsto é obrigatório")
        @Positive(message = "O valor previsto deve ser maior que zero")
        BigDecimal valorPrevisto,
    @NotNull(message = "A data de vencimento é obrigatória") LocalDate dataVencimento,
    @NotNull(message = "O tipo do título é obrigatório") TipoMovimentacao tipo,
    @Size(max = 255, message = "A observação deve ter no máximo 255 caracteres")
        String observacao,
    @NotNull(message = "A categoria é obrigatória") Long categoriaId,
    @NotNull(message = "O usuário é obrigatório") Long usuarioId) {}
