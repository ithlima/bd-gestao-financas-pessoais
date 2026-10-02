package com.example.financas.service;

import com.example.financas.dto.response.PosicaoTitulosResponse;
import com.example.financas.dto.response.ResumoFinanceiroResponse;
import com.example.financas.dto.response.SituacaoEmAberto;
import com.example.financas.dto.response.TituloEmAbertoResponse;
import com.example.financas.entity.SituacaoTitulo;
import com.example.financas.entity.TipoMovimentacao;
import com.example.financas.entity.Titulo;
import com.example.financas.exception.RegraNegocioException;
import com.example.financas.repository.TituloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class ResumoFinanceiroService {

  public static final int JANELA_PADRAO_DIAS = 30;

  private final TituloRepository tituloRepository;
  private final MovimentacaoService movimentacaoService;
  private final UsuarioService usuarioService;

  public ResumoFinanceiroService(
      TituloRepository tituloRepository,
      MovimentacaoService movimentacaoService,
      UsuarioService usuarioService) {
    this.tituloRepository = tituloRepository;
    this.movimentacaoService = movimentacaoService;
    this.usuarioService = usuarioService;
  }

  @Transactional(readOnly = true)
  public PosicaoTitulosResponse contasAPagar(
      Long usuarioId, LocalDate dataReferencia, Integer janelaDias) {
    return montarPosicao(usuarioId, TipoMovimentacao.DESPESA, dataReferencia, janelaDias);
  }

  @Transactional(readOnly = true)
  public PosicaoTitulosResponse contasAReceber(
      Long usuarioId, LocalDate dataReferencia, Integer janelaDias) {
    return montarPosicao(usuarioId, TipoMovimentacao.RECEITA, dataReferencia, janelaDias);
  }

  @Transactional(readOnly = true)
  public ResumoFinanceiroResponse resumo(
      Long usuarioId, LocalDate dataReferencia, Integer janelaDias) {

    LocalDate referencia = dataReferencia != null ? dataReferencia : LocalDate.now();

    usuarioService.buscarEntidade(usuarioId);

    PosicaoTitulosResponse aPagar = contasAPagar(usuarioId, referencia, janelaDias);
    PosicaoTitulosResponse aReceber = contasAReceber(usuarioId, referencia, janelaDias);

    BigDecimal saldoPrevisto =
        aReceber.totalEmAberto().subtract(aPagar.totalEmAberto());

    return new ResumoFinanceiroResponse(
        usuarioId,
        referencia,
        aPagar,
        aReceber,
        saldoPrevisto,
        saldoPrevisto.compareTo(BigDecimal.ZERO) >= 0);
  }

  private PosicaoTitulosResponse montarPosicao(
      Long usuarioId, TipoMovimentacao tipo, LocalDate dataReferencia, Integer janelaDias) {

    usuarioService.buscarEntidade(usuarioId);

    LocalDate referencia = dataReferencia != null ? dataReferencia : LocalDate.now();
    int janela = validarJanela(janelaDias);
    LocalDate limiteDaJanela = referencia.plusDays(janela);

    List<Titulo> pendentes =
        tituloRepository
            .findByUsuarioIdUsuarioAndSituacao(usuarioId, SituacaoTitulo.PENDENTE)
            .stream()
            .filter(t -> t.getTipo() == tipo)
            .toList();

    List<TituloEmAbertoResponse> itens =
        pendentes.stream()
            .map(titulo -> montarItem(titulo, referencia))

            .sorted(Comparator.comparing(TituloEmAbertoResponse::dataVencimento))
            .toList();

    List<TituloEmAbertoResponse> vencidos =
        itens.stream().filter(TituloEmAbertoResponse::estaVencido).toList();

    List<TituloEmAbertoResponse> aVencer =
        itens.stream()
            .filter(i -> !i.estaVencido())
            .filter(i -> i.dataVencimento().isBefore(limiteDaJanela.plusDays(1)))
            .toList();

    return new PosicaoTitulosResponse(
        usuarioId,
        tipo,
        referencia,
        itens,
        somarEmAberto(itens),
        somarEmAberto(vencidos),
        somarEmAberto(aVencer),
        itens.size(),
        vencidos.size(),
        (int) itens.stream().filter(TituloEmAbertoResponse::isParcial).count(),
        aVencer.size(),
        vencidos,
        aVencer,
        janela);
  }

  private TituloEmAbertoResponse montarItem(Titulo titulo, LocalDate referencia) {

    BigDecimal jaRealizado =
        movimentacaoService.totalRealizadoDoTitulo(titulo.getIdTitulo());

    BigDecimal emAberto = titulo.getValorPrevisto().subtract(jaRealizado);
    if (emAberto.compareTo(BigDecimal.ZERO) < 0) {

      emAberto = BigDecimal.ZERO;
    }

    long dias = ChronoUnit.DAYS.between(titulo.getDataVencimento(), referencia);
    boolean vencido = titulo.getDataVencimento().isBefore(referencia);
    boolean parcial = jaRealizado.compareTo(BigDecimal.ZERO) > 0;

    return new TituloEmAbertoResponse(
        titulo.getIdTitulo(),
        titulo.getDescricao(),
        titulo.getTipo(),
        titulo.getValorPrevisto(),
        jaRealizado,
        emAberto,
        titulo.getDataVencimento(),
        dias,
        classificar(vencido, parcial),
        titulo.getCategoria().getNome(),
        titulo.getObservacao());
  }

  private SituacaoEmAberto classificar(boolean vencido, boolean parcial) {
    if (parcial) {
      return vencido ? SituacaoEmAberto.PARCIAL_VENCIDO : SituacaoEmAberto.PARCIAL;
    }
    return vencido ? SituacaoEmAberto.VENCIDO : SituacaoEmAberto.A_VENCER;
  }

  private BigDecimal somarEmAberto(List<TituloEmAbertoResponse> itens) {
    return itens.stream()
        .map(TituloEmAbertoResponse::valorEmAberto)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private int validarJanela(Integer janelaDias) {
    if (janelaDias == null) {
      return JANELA_PADRAO_DIAS;
    }
    if (janelaDias < 0) {
      throw new RegraNegocioException("A janela de dias não pode ser negativa");
    }
    return janelaDias;
  }
}
