package com.example.financas.mapper;

import com.example.financas.dto.response.CategoriaResponse;
import com.example.financas.entity.Categoria;

/** Conversão entre a entidade {@code Categoria} e seu DTO de resposta. */
public final class CategoriaMapper {

  private CategoriaMapper() {
    // classe utilitária: não deve ser instanciada
  }

  public static CategoriaResponse toResponse(Categoria categoria) {
    return new CategoriaResponse(
        categoria.getIdCategoria(),
        categoria.getNome(),
        categoria.getTipo(),
        categoria.getUsuario().getIdUsuario());
  }
}
