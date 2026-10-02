package com.example.financas.mapper;

import com.example.financas.dto.response.TituloResponse;
import com.example.financas.entity.Titulo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Conversão entre a entidade {@code Titulo} e seu DTO de resposta.
 *
 * <p>Aqui ficam concentrados os cálculos derivados que dão sentido ao título:
 * {@code valorRealizado}, {@code valorEmAberto} e {@code situacaoEfetiva}. O
 * mapper não acessa o banco: o valor já realizado é somado pelo service (uma
 * consulta {@code SUM}) e repassado como parâmetro.
 */
public final class TituloMapper {

  private TituloMapper() {
    // classe utilitária: não deve ser instanciada
  }

  /**
   * @param titulo entidade já carregada
   * @param valorRealizado soma das movimentações vinculadas, calculada pelo service
   * @param quantidadeMovimentacoes quantas movimentações já quitaram parte do título
   * @param referencia data usada para decidir se o título está vencido; permite
   *     testar a regra sem depender do relógio da máquina
   */
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

  /** Atalho que usa a data de hoje como referência. */
  public static TituloResponse toResponse(
      Titulo titulo, BigDecimal valorRealizado, int quantidadeMovimentacoes) {
    return toResponse(titulo, valorRealizado, quantidadeMovimentacoes, LocalDate.now());
  }
}
