package com.example.financas.dto.response;

import com.example.financas.entity.TipoMovimentacao;

/** Representação pública de uma categoria. */
public record CategoriaResponse(Long idCategoria, String nome, TipoMovimentacao tipo,
    Long usuarioId) {}
