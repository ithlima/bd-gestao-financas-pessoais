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

/**
 * Gera a <b>DRE (Demonstração do Resultado do Exercício) para finanças
 * pessoais</b>.
 *
 * <p>Não é uma DRE empresarial: não há lucro bruto, margem de contribuição,
 * depreciação nem impostos sobre lucro. A estrutura é a de um orçamento pessoal,
 * em três blocos:
 *
 * <pre>
 *   RECEITAS
 *     Salários, Freelances, Rendimentos, Outras receitas ...
 *   (-) DESPESAS
 *     Moradia, Alimentação, Transporte, Educação, Saúde, Lazer, Outras despesas ...
 *   (=) RESULTADO DO PERÍODO
 * </pre>
 *
 * <p>As linhas <b>não</b> são uma lista fixa no código: vêm das categorias que o
 * usuário cadastrou. Assim o relatório serve qualquer usuário.
 *
 * <h2>A regra central</h2>
 *
 * <p>Este service lê de <b>duas fontes diferentes</b>, e nunca as mistura:
 *
 * <table border="1">
 *   <caption>Fontes de dados por bloco</caption>
 *   <tr><th>Bloco</th><th>Tabela</th><th>Data usada</th><th>Regime</th></tr>
 *   <tr><td>Previsto</td><td>{@code titulo}</td><td>{@code data_vencimento}</td>
 *       <td>competência</td></tr>
 *   <tr><td>Realizado</td><td>{@code movimentacao}</td><td>{@code data}</td>
 *       <td>caixa</td></tr>
 * </table>
 *
 * <p>Consequência prática, que é o que o sistema precisa garantir:
 *
 * <blockquote>
 * Uma conta de energia de R$ 150,00 que vence dia 10 e <b>ainda não foi paga</b>
 * aparece na DRE <b>prevista</b> (era uma obrigação do período) e <b>não</b>
 * aparece na DRE <b>realizada</b> (nenhum real saiu da conta). Só depois de
 * registrado o pagamento ela passa a compor o resultado realizado — na
 * <b>data do pagamento</b>, não na data do vencimento.
 * </blockquote>
 *
 * <p>A DRE <b>não é armazenada</b> em tabela alguma. Ela é sempre calculada a
 * partir dos fatos, o que elimina qualquer possibilidade de o relatório divergir
 * dos lançamentos que o originaram.
 */
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

  /**
   * Apura a DRE de um período.
   *
   * @param usuarioId dono dos dados
   * @param inicio primeiro dia do período de apuração
   * @param fim último dia do período de apuração
   * @param modo {@code "previsto"}, {@code "realizado"} ou {@code "comparativo"}
   */
  @Transactional(readOnly = true)
  public DreResponse gerar(Long usuarioId, LocalDate inicio, LocalDate fim, String modo) {

    if (inicio == null || fim == null) {
      throw new RegraNegocioException("As datas de início e fim do período são obrigatórias");
    }
    if (inicio.isAfter(fim)) {
      throw new RegraNegocioException("A data de início não pode ser posterior à data de fim");
    }

    // Falha cedo (404) se o usuário não existir, em vez de devolver uma DRE
    // vazia que pareceria "nenhum lançamento" quando na verdade é "usuário errado".
    usuarioService.buscarEntidade(usuarioId);

    List<DrePrevisaoProjection> previsoes = tituloService.somarPrevistoPorCategoria(usuarioId, inicio, fim);
    List<DreRealizadoProjection> realizados =
        movimentacaoRepository.somarRealizadoPorCategoria(usuarioId, inicio, fim);

    List<DreLinhaResponse> linhas = montarLinhas(previsoes, realizados);

    List<DreLinhaResponse> linhasReceitas =
        linhas.stream().filter(l -> l.tipo() == TipoMovimentacao.RECEITA).toList();
    List<DreLinhaResponse> linhasDespesas =
        linhas.stream().filter(l -> l.tipo() == TipoMovimentacao.DESPESA).toList();

    BigDecimal totalReceitasPrevistas = somar(linhasReceitas, true);
    BigDecimal totalReceitasRealizadas = somar(linhasReceitas, false);
    BigDecimal totalDespesasPrevistas = somar(linhasDespesas, true);
    BigDecimal totalDespesasRealizadas = somar(linhasDespesas, false);

    BigDecimal resultadoPrevisto = totalReceitasPrevistas.subtract(totalDespesasPrevistas);
    BigDecimal resultadoRealizado = totalReceitasRealizadas.subtract(totalDespesasRealizadas);
    BigDecimal variacao = resultadoRealizado.subtract(resultadoPrevisto);

    List<DreTituloVencidoResponse> vencidos = montarVencidos(usuarioId, LocalDate.now());

    return new DreResponse(
        usuarioId,
        modo == null ? "comparativo" : modo.toLowerCase(),
        inicio,
        fim,
        linhasReceitas,
        totalReceitasPrevistas,
        totalReceitasRealizadas,
        linhasDespesas,
        totalDespesasPrevistas,
        totalDespesasRealizadas,
        resultadoPrevisto,
        resultadoRealizado,
        variacao,
        // variação >= 0 é favorável: resultado realizado igual ou melhor que o previsto
        variacao.compareTo(BigDecimal.ZERO) >= 0,
        vencidos,
        vencidos.stream()
            .map(DreTituloVencidoResponse::valorPrevisto)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  // ------------------------------------------------------------------
  // Montagem das linhas da DRE
  // ------------------------------------------------------------------

  /**
   * Consolida previsão e realização em uma linha por categoria.
   *
   * <p>As duas consultas vêm agrupadas por categoria, então a junção é feita por
   * chave (tipo + idCategoria). Categorias que só têm previsão aparecem com
   * realizado zero (ex.: "Educação" no mês, ainda não paga); categorias que só
   * têm realização aparecem com previsto zero (ex.: um gasto imprevisto).
   */
  private List<DreLinhaResponse> montarLinhas(
      List<DrePrevisaoProjection> previsoes, List<DreRealizadoProjection> realizados) {

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
        .map(Acumulador::paraLinha)
        .sorted(
            Comparator.comparing((DreLinhaResponse l) -> l.tipo().name())
                .thenComparing(DreLinhaResponse::categoriaNome))
        .toList();
  }

  private String chave(String tipo, Long categoriaId) {
    return tipo + "#" + categoriaId;
  }

  /** Soma as linhas de um bloco, escolhendo entre previsto e realizado. */
  private BigDecimal somar(List<DreLinhaResponse> linhas, boolean previsto) {
    return linhas.stream()
        .map(l -> previsto ? l.valorPrevisto() : l.valorRealizado())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal nuloParaZero(BigDecimal valor) {
    return valor != null ? valor : BigDecimal.ZERO;
  }

  /**
   * Títulos pendentes com vencimento no passado.
   *
   * <p>O {@code situacaoEfetiva = VENCIDO} é derivado aqui, não lido do banco.
   */
  private List<DreTituloVencidoResponse> montarVencidos(Long usuarioId, LocalDate referencia) {
    List<DreTituloVencidoResponse> vencidos = new ArrayList<>();

    // Consulta dedicada: situação PENDENTE no banco + vencimento anterior à
    // referência. Um título cancelado não é atraso; um já quitado também não.
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

  /**
   * Acumulador auxiliar usado apenas durante a montagem das linhas.
   *
   * <p>Existe para não espalhar variáveis soltas pelo método de consolidação.
   */
  private static final class Acumulador {

    private final Long categoriaId;
    private final String categoriaNome;
    private final TipoMovimentacao tipo;
    private BigDecimal previsto = BigDecimal.ZERO;
    private BigDecimal realizado = BigDecimal.ZERO;

    /** A projeção entrega o tipo como String (SQL nativo). Ver as projeções. */
    private Acumulador(Long categoriaId, String categoriaNome, String tipo) {
      this.categoriaId = categoriaId;
      this.categoriaNome = categoriaNome;
      this.tipo = TipoMovimentacao.valueOf(tipo);
    }

    private DreLinhaResponse paraLinha() {
      return new DreLinhaResponse(
          categoriaId, categoriaNome, tipo, previsto, realizado, calcularVariacao(tipo, previsto, realizado));
    }

    /**
     * Variação com sinal normalizado, de modo que <b>positivo seja sempre
     * favorável</b> ao usuário:
     *
     * <ul>
     *   <li>receita: {@code realizado − previsto} (receber mais é bom);</li>
     *   <li>despesa: {@code previsto − realizado} (gastar menos é bom).</li>
     * </ul>
     */
    private static BigDecimal calcularVariacao(
        TipoMovimentacao tipo, BigDecimal previsto, BigDecimal realizado) {
      return tipo == TipoMovimentacao.RECEITA
          ? realizado.subtract(previsto).setScale(2, RoundingMode.HALF_UP)
          : previsto.subtract(realizado).setScale(2, RoundingMode.HALF_UP);
    }
  }
}
