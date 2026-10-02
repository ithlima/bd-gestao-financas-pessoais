package com.example.financas.dto.request;

import com.example.financas.entity.TipoMovimentacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada para cadastrar ou atualizar uma categoria.
 *
 * <p>O {@code tipo} é obrigatório porque é ele que posiciona a categoria na DRE:
 * categorias de RECEITA viram linhas do bloco de receitas; de DESPESA, do bloco
 * de despesas. O service valida (RN03/RN15) que o tipo da categoria seja igual
 * ao tipo do título ou da movimentação que a utiliza.
 */
public record CategoriaRequest(
    @NotBlank(message = "O nome da categoria é obrigatório")
        @Size(max = 100, message = "O nome da categoria deve ter no máximo 100 caracteres")
        String nome,
    @NotNull(message = "O tipo da categoria é obrigatório") TipoMovimentacao tipo,
    @NotNull(message = "O usuário é obrigatório") Long usuarioId) {}
