package com.example.financas.mapper;

import com.example.financas.dto.response.CategoriaResponse;
import com.example.financas.entity.Categoria;

public final class CategoriaMapper {

  private CategoriaMapper() {

  }

  public static CategoriaResponse toResponse(Categoria categoria) {
    return new CategoriaResponse(
        categoria.getIdCategoria(),
        categoria.getNome(),
        categoria.getTipo(),
        categoria.getUsuario().getIdUsuario());
  }
}
