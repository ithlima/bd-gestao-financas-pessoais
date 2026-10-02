package com.example.financas.mapper;

import com.example.financas.dto.response.MovimentacaoResponse;
import com.example.financas.entity.Movimentacao;

public final class MovimentacaoMapper {

  private MovimentacaoMapper() {

  }

  public static MovimentacaoResponse toResponse(Movimentacao movimentacao) {
    return new MovimentacaoResponse(
        movimentacao.getIdMovimentacao(),
        movimentacao.getDescricao(),
        movimentacao.getValor(),
        movimentacao.getTipo(),
        movimentacao.getData(),
        movimentacao.getConta().getIdConta(),
        movimentacao.getConta().getNome(),
        movimentacao.getCategoria().getIdCategoria(),
        movimentacao.getCategoria().getNome(),

        movimentacao.getTitulo() != null ? movimentacao.getTitulo().getIdTitulo() : null,
        movimentacao.getUsuario().getIdUsuario());
  }
}
