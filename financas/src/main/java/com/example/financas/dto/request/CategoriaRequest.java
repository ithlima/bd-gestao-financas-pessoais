package com.example.financas.dto.request;

import com.example.financas.entity.TipoMovimentacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
    @NotBlank(message = "O nome da categoria é obrigatório")
        @Size(max = 100, message = "O nome da categoria deve ter no máximo 100 caracteres")
        String nome,
    @NotNull(message = "O tipo da categoria é obrigatório") TipoMovimentacao tipo,
    @NotNull(message = "O usuário é obrigatório") Long usuarioId) {}
