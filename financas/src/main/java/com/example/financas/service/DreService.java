package com.example.financas.service;

import com.example.financas.dto.response.DreLinhaResponse;
import com.example.financas.dto.response.DreResponse;
import com.example.financas.dto.response.DreTituloVencidoResponse;
import com.example.financas.entity.TipoMovimentacao;
import com.example.financas.entity.Titulo;
import com.example.financas.exception.RegraNegocioException;
import com.example.financas.repository.MovimentacaoRepository;
import com.example.financas.repository.projection.DrePrevisaoProjection;
import com.example.financas.repository.projection.DreRealizadoProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DreService {

  private final TituloService tituloService;
  private final MovimentacaoRepository movimentacaoRepository;
  private final UsuarioService usuarioService;

  public DreService(
      TituloService tituloService,
      MovimentacaoRepository movimentacaoRepository,
      UsuarioService usuarioService) {
    this.tituloService = tituloService;
    this.movimentacaoRepository = movimentacaoRepository;
    this.usuarioService = usuarioService;
  }

  @Transactional(readOnly = true)
  public DreResponse gerar(Long usuarioId, LocalDate inicio, LocalDate fim, String modo) {

    if (inicio == null || fim == null) {
      throw new RegraNegocioException("As datas de início e fim do período são obrigatórias");
    }
    if (inicio.isAfter(fim)) {
      throw new RegraNegocioException("A data de início não pode ser posterior à data de fim");
    }

    String modoNormalizado = normalizarModo(modo);
    boolean mostrarPrevisto = !modoNormalizado.equals("realizado");
    boolean mostrarRealizado = !modoNormalizado.equals("previsto");

    usuarioService.buscarEntidade(usuarioId);

    List<DrePrevisaoProjection> previsoes = tituloService.somarPrevistoPorCategoria(usuarioId, inicio, fim);
    List<DreRealizadoProjection> realizados =
        movimentacaoRepository.somarRealizadoPorCategoria(usuarioId, inicio, fim);

    List<DreLinhaResponse> todasAsLinhas = montarLinhas(previsoes, realizados, true, true);

    List<DreLinhaResponse> receitas =
        todasAsLinhas.stream().filter(l -> l.tipo() == TipoMovimentacao.RECEITA).toList();
    List<DreLinhaResponse> despesas =
        todasAsLinhas.stream().filter(l -> l.tipo() == TipoMovimentacao.DESPESA).toList();

    BigDecimal totalReceitasPrevistas = somar(receitas, true);
    BigDecimal totalReceitasRealizadas = somar(receitas, false);
    BigDecimal totalDespesasPrevistas = somar(despesas, true);
    BigDecimal totalDespesasRealizadas = somar(despesas, false);

    BigDecimal resultadoPrevisto = totalReceitasPrevistas.subtract(totalDespesasPrevistas);
    BigDecimal resultadoRealizado = totalReceitasRealizadas.subtract(totalDespesasRealizadas);
    BigDecimal variacao = resultadoRealizado.subtract(resultadoPrevisto);

    List<DreLinhaResponse> linhasVisiveis =
        montarLinhas(previsoes, realizados, mostrarPrevisto, mostrarRealizado);

    List<DreLinhaResponse> receitasVisiveis =
        linhasVisiveis.stream().filter(l -> l.tipo() == TipoMovimentacao.RECEITA).toList();
    List<DreLinhaResponse> despesasVisiveis =
        linhasVisiveis.stream().filter(l -> l.tipo() == TipoMovimentacao.DESPESA).toList();

    List<DreTituloVencidoResponse> vencidos = montarVencidos(usuarioId, LocalDate.now());
    return new DreResponse(
        usuarioId,
        modoNormalizado,
        inicio,
        fim,

        receitasVisiveis,
        mostrarPrevisto ? totalReceitasPrevistas : null,
        mostrarRealizado ? totalReceitasRealizadas : null,
        despesasVisiveis,
        mostrarPrevisto ? totalDespesasPrevistas : null,
        mostrarRealizado ? totalDespesasRealizadas : null,
        mostrarPrevisto ? resultadoPrevisto : null,
        mostrarRealizado ? resultadoRealizado : null,
        mostrarPrevisto && mostrarRealizado ? variacao : null,

        mostrarPrevisto && mostrarRealizado && variacao.compareTo(BigDecimal.ZERO) >= 0,
        vencidos,
        vencidos.stream()
            .map(DreTituloVencidoResponse::valorPrevisto)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  private List<DreLinhaResponse> montarLinhas(
      List<DrePrevisaoProjection> previsoes,
      List<DreRealizadoProjection> realizados,
      boolean mostrarPrevisto,
      boolean mostrarRealizado) {

    Map<String, Acumulador> porCategoria = new LinkedHashMap<>();

    for (DrePrevisaoProjection p : previsoes) {
      Acumulador acumulador =
          porCategoria.computeIfAbsent(
              chave(p.getTipo(), p.getCategoriaId()),
              k -> new Acumulador(p.getCategoriaId(), p.getCategoriaNome(), p.getTipo()));
      acumulador.previsto = acumulador.previsto.add(nuloParaZero(p.getTotalPrevisto()));
    }

    for (DreRealizadoProjection r : realizados) {
      Acumulador acumulador =
          porCategoria.computeIfAbsent(
              chave(r.getTipo(), r.getCategoriaId()),
              k -> new Acumulador(r.getCategoriaId(), r.getCategoriaNome(), r.getTipo()));
      acumulador.realizado = acumulador.realizado.add(nuloParaZero(r.getTotalRealizado()));
    }

    return porCategoria.values().stream()
        .map(a -> a.paraLinha(mostrarPrevisto, mostrarRealizado))
        .sorted(
            Comparator.comparing((DreLinhaResponse l) -> l.tipo().name())
                .thenComparing(DreLinhaResponse::categoriaNome))
        .toList();
  }

  private String normalizarModo(String modo) {
    if (modo == null || modo.isBlank()) {
      return "comparativo";
    }
    String normalizado = modo.trim().toLowerCase(java.util.Locale.ROOT);
    if (!List.of("previsto", "realizado", "comparativo").contains(normalizado)) {
      throw new RegraNegocioException(
          "Modo de apuração inválido: '" + modo + "'. Use 'previsto', 'realizado' ou 'comparativo'");
    }
    return normalizado;
  }

  private String chave(String tipo, Long categoriaId) {
    return tipo + "#" + categoriaId;
  }

  private BigDecimal somar(List<DreLinhaResponse> linhas, boolean previsto) {
    return linhas.stream()
        .map(l -> previsto ? l.valorPrevisto() : l.valorRealizado())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal nuloParaZero(BigDecimal valor) {
    return valor != null ? valor : BigDecimal.ZERO;
  }

  private List<DreTituloVencidoResponse> montarVencidos(Long usuarioId, LocalDate referencia) {
    List<DreTituloVencidoResponse> vencidos = new ArrayList<>();

    for (Titulo titulo :
        tituloService.listarEntidadesVencidas(usuarioId, referencia)) {

      vencidos.add(
          new DreTituloVencidoResponse(
              titulo.getIdTitulo(),
              titulo.getDescricao(),
              titulo.getValorPrevisto(),
              titulo.getDataVencimento(),
              ChronoUnit.DAYS.between(titulo.getDataVencimento(), referencia),
              titulo.getTipo(),
              titulo.getCategoria().getNome(),
              titulo.getSituacaoEfetiva(referencia)));
    }

    vencidos.sort(Comparator.comparing(DreTituloVencidoResponse::dataVencimento));
    return vencidos;
  }

  private static final class Acumulador {

    private final Long categoriaId;
    private final String categoriaNome;
    private final TipoMovimentacao tipo;
    private BigDecimal previsto = BigDecimal.ZERO;
    private BigDecimal realizado = BigDecimal.ZERO;

    private Acumulador(Long categoriaId, String categoriaNome, String tipo) {
      this.categoriaId = categoriaId;
      this.categoriaNome = categoriaNome;
      this.tipo = TipoMovimentacao.valueOf(tipo);
    }

    private DreLinhaResponse paraLinha(boolean mostrarPrevisto, boolean mostrarRealizado) {

      return new DreLinhaResponse(
          categoriaId,
          categoriaNome,
          tipo,
          mostrarPrevisto ? previsto : null,
          mostrarRealizado ? realizado : null,
          mostrarPrevisto && mostrarRealizado
              ? calcularVariacao(tipo, previsto, realizado)
              : null);
    }

    private static BigDecimal calcularVariacao(
        TipoMovimentacao tipo, BigDecimal previsto, BigDecimal realizado) {
      return tipo == TipoMovimentacao.RECEITA
          ? realizado.subtract(previsto).setScale(2, RoundingMode.HALF_UP)
          : previsto.subtract(realizado).setScale(2, RoundingMode.HALF_UP);
    }
  }
}
