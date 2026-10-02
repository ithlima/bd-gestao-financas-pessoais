package com.example.financas.mapper;

import com.example.financas.dto.response.MovimentacaoResponse;
import com.example.financas.entity.Movimentacao;

/** Conversão entre a entidade {@code Movimentacao} e seu DTO de resposta. */
public final class MovimentacaoMapper {

  private MovimentacaoMapper() {
    // classe utilitária: não deve ser instanciada
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
        // nulo quando o lançamento é avulso, sem título de origem
        movimentacao.getTitulo() != null ? movimentacao.getTitulo().getIdTitulo() : null,
        movimentacao.getUsuario().getIdUsuario());
  }
}
