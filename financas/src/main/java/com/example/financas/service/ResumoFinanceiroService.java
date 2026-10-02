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

/**
 * Calcula a posição de <b>contas a pagar</b> e <b>contas a receber</b> do usuário.
 *
 * <h2>A decisão de projeto mais importante desta classe</h2>
 *
 * <p>Esta funcionalidade <b>não introduz nenhuma tabela, entidade nem campo
 * novos</b>. Tudo o que ela precisa já existe desde a evolução para o modelo de
 * DRE + Títulos:
 *
 * <ul>
 *   <li><b>contas a pagar</b> = títulos com {@code tipo = DESPESA};</li>
 *   <li><b>contas a receber</b> = títulos com {@code tipo = RECEITA};</li>
 *   <li><b>está em aberto</b> = {@code situacao = PENDENTE} (não foi quitado nem
 *       cancelado);</li>
 *   <li><b>quanto falta</b> = {@code valorPrevisto − Σ movimentações};</li>
 *   <li><b>está vencido</b> = {@code dataVencimento < dataReferencia}.</li>
 * </ul>
 *
 * <p>Ou seja: "contas a pagar" e "contas a receber" <b>não são conceitos novos no
 * domínio</b> — são a mesma entidade {@code Titulo} vista de dois ângulos
 * diferentes. Criar tabelas separadas para elas seria duplicar a mesma
 * informação em três lugares (título de despesa, título de receita, conta a
 * pagar) e criar a possibilidade de os três divergirem.
 *
 * <blockquote>
 * A alternativa seria modelar {@code ContaAPagar} e {@code ContaAReceber} como
 * entidades próprias. Isso levaria a três tabelas com os mesmos campos
 * (descrição, valor, vencimento, situação), exatamente o mesmo problema que a
 * evolução anterior corrigiu ao absorver {@code recebimento} e {@code pagamento}
 * em {@code movimentacao}.
 * </blockquote>
 *
 * <p>A única coisa que esta classe acrescenta é <b>visão</b>: agrupar, somar,
 * classificar por urgência e ordenar. Nada é gravado.
 *
 * <h2>A regra do valor em aberto</h2>
 *
 * <p>Os totais somam {@code valorEmAberto}, nunca {@code valorPrevisto}. Um
 * aluguel de R$ 1.200,00 com R$ 600,00 já pagos entra como <b>R$ 600,00</b> nas
 * contas a pagar. Somar o valor previsto inflaria a dívida e faria o usuário
 * achar que deve mais do que realmente deve.
 */
@Service
public class ResumoFinanceiroService {

  /** Janela padrão de "próximos dias" quando o cliente não informa outra. */
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

  // ------------------------------------------------------------------
  // Contas a pagar
  // ------------------------------------------------------------------

  /**
   * Posição de <b>contas a pagar</b>: o que ainda falta pagar.
   *
   * @param usuarioId dono dos títulos
   * @param dataReferencia data usada para decidir o que está vencido; quando
   *     {@code null}, usa hoje
   * @param janelaDias quantos dias à frente considerar em "próximos dias"
   */
  @Transactional(readOnly = true)
  public PosicaoTitulosResponse contasAPagar(
      Long usuarioId, LocalDate dataReferencia, Integer janelaDias) {
    return montarPosicao(usuarioId, TipoMovimentacao.DESPESA, dataReferencia, janelaDias);
  }

  // ------------------------------------------------------------------
  // Contas a receber
  // ------------------------------------------------------------------

  /**
   * Posição de <b>contas a receber</b>: o que ainda falta receber.
   *
   * <p>É a operação espelho de {@link #contasAPagar}. A única diferença é o tipo
   * filtrado — o que, de novo, mostra que os dois conceitos são a mesma coisa
   * olhada de dois lados.
   */
  @Transactional(readOnly = true)
  public PosicaoTitulosResponse contasAReceber(
      Long usuarioId, LocalDate dataReferencia, Integer janelaDias) {
    return montarPosicao(usuarioId, TipoMovimentacao.RECEITA, dataReferencia, janelaDias);
  }

  // ------------------------------------------------------------------
  // Resumo consolidado
  // ------------------------------------------------------------------

  /**
   * Situação consolidada: o que deve sair e o que deve entrar, lado a lado.
   *
   * <p>{@code saldoPrevisto} é a diferença entre os dois. Ele é <b>separado</b> do
   * saldo das contas de propósito: o saldo bancário é passado (fatos consumados) e
   * as contas em aberto são futuro (previsões). Somá-los produziria um número
   * enganoso.
   */
  @Transactional(readOnly = true)
  public ResumoFinanceiroResponse resumo(
      Long usuarioId, LocalDate dataReferencia, Integer janelaDias) {

    LocalDate referencia = dataReferencia != null ? dataReferencia : LocalDate.now();

    // Falha cedo (404) se o usuário não existir, em vez de devolver zeros que
    // pareceriam "nada em aberto" quando na verdade é "usuário errado".
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

  // ------------------------------------------------------------------
  // Cálculo
  // ------------------------------------------------------------------

  /**
   * Monta a posição de um tipo de título.
   *
   * <p>Carrega os títulos pendentes de uma vez e calcula tudo em memória. A
   * alternativa — uma consulta de soma por título — seria N+1 consultas para a
   * mesma informação.
   */
  private PosicaoTitulosResponse montarPosicao(
      Long usuarioId, TipoMovimentacao tipo, LocalDate dataReferencia, Integer janelaDias) {

    usuarioService.buscarEntidade(usuarioId);

    LocalDate referencia = dataReferencia != null ? dataReferencia : LocalDate.now();
    int janela = validarJanela(janelaDias);
    LocalDate limiteDaJanela = referencia.plusDays(janela);

    // Só títulos PENDENTES. Quitados não são mais conta a pagar/receber; cancelados
    // nunca foram obrigação de verdade.
    List<Titulo> pendentes =
        tituloRepository
            .findByUsuarioIdUsuarioAndSituacao(usuarioId, SituacaoTitulo.PENDENTE)
            .stream()
            .filter(t -> t.getTipo() == tipo)
            .toList();

    List<TituloEmAbertoResponse> itens =
        pendentes.stream()
            .map(titulo -> montarItem(titulo, referencia))
            // Ordena por urgência: o que vence primeiro aparece primeiro.
            // Como vencidos já passaram, eles naturalmente ficam no topo.
            .sorted(Comparator.comparing(TituloEmAbertoResponse::dataVencimento))
            .toList();

    List<TituloEmAbertoResponse> vencidos =
        itens.stream().filter(TituloEmAbertoResponse::estaVencido).toList();

    // A janela é INCLUSIVA nas duas pontas: um título que vence exatamente no
    // último dia da janela entra nela. "Vence em 30 dias" deve caber em uma
    // janela de 30 dias.
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

  /** Converte o título no item de posição, calculando o que falta e a urgência. */
  private TituloEmAbertoResponse montarItem(Titulo titulo, LocalDate referencia) {

    BigDecimal jaRealizado =
        movimentacaoService.totalRealizadoDoTitulo(titulo.getIdTitulo());

    // O valor que interessa a contas a pagar/receber é o que FALTA, não o previsto.
    BigDecimal emAberto = titulo.getValorPrevisto().subtract(jaRealizado);
    if (emAberto.compareTo(BigDecimal.ZERO) < 0) {
      // Não deveria acontecer (a quitação impede pagar além do previsto), mas
      // proteger aqui evita que um dado inconsistente vire um total negativo.
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

  /** Soma o valor em aberto de uma lista. Vazio resulta em zero. */
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
