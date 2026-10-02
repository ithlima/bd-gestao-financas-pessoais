package com.example.financas.mapper;

import com.example.financas.dto.response.TituloResponse;
import com.example.financas.entity.Titulo;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class TituloMapper {

  private TituloMapper() {

  }

  public static TituloResponse toResponse(
      Titulo titulo,
      BigDecimal valorRealizado,
      int quantidadeMovimentacoes,
      LocalDate referencia) {

    BigDecimal realizado = valorRealizado != null ? valorRealizado : BigDecimal.ZERO;
    BigDecimal emAberto = titulo.getValorPrevisto().subtract(realizado);

    return new TituloResponse(
        titulo.getIdTitulo(),
        titulo.getDescricao(),
        titulo.getValorPrevisto(),
        realizado,
        emAberto,
        titulo.getDataVencimento(),
        titulo.getDataPagamento(),
        titulo.getTipo(),
        titulo.getSituacao(),
        titulo.getSituacaoEfetiva(referencia),
        titulo.isAbertoParaQuitacao(),
        quantidadeMovimentacoes,
        titulo.getObservacao(),
        titulo.getCategoria().getIdCategoria(),
        titulo.getCategoria().getNome(),
        titulo.getUsuario().getIdUsuario());
  }

  public static TituloResponse toResponse(
      Titulo titulo, BigDecimal valorRealizado, int quantidadeMovimentacoes) {
    return toResponse(titulo, valorRealizado, quantidadeMovimentacoes, LocalDate.now());
  }
}
