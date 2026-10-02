package com.example.financas.dto.response;

import com.example.financas.entity.TipoMovimentacao;

public record CategoriaResponse(Long idCategoria, String nome, TipoMovimentacao tipo,
    Long usuarioId) {}
