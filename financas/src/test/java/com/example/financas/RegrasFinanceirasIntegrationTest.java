package com.example.financas;

import com.example.financas.dto.request.TituloRequest;
import com.example.financas.dto.response.DreResponse;
import com.example.financas.dto.response.PosicaoTitulosResponse;
import com.example.financas.dto.response.ResumoFinanceiroResponse;
import com.example.financas.dto.response.SituacaoEmAberto;
import com.example.financas.dto.response.TituloResponse;
import com.example.financas.entity.SituacaoTitulo;
import com.example.financas.entity.SituacaoTituloEfetiva;
import com.example.financas.entity.TipoMovimentacao;
import com.example.financas.exception.RegraNegocioException;
import com.example.financas.service.DreService;
import com.example.financas.service.ResumoFinanceiroService;
import com.example.financas.service.TituloService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de integração das regras de negócio centrais do sistema.
 *
 * <p>Estes testes verificam exatamente a promessa conceitual da evolução:
 *
 * <pre>
 *   TÍTULO (previsão)  ->  MOVIMENTAÇÃO (realização)  ->  DRE
 * </pre>
 *
 * <p>O teste mais importante da classe é
 * {@link #tituloPendenteNaoEntraNaDreRealizada()}: é ele que prova que uma conta
 * ainda não paga <b>não</b> é contada como despesa efetivamente realizada.
 *
 * <p>Os dados iniciais vêm de {@code testdata/10-dados-base.sql}, executado
 * antes de cada teste (ver {@code @Sql} abaixo) em um banco separado
 * ({@code financas_test}), criado e destruído automaticamente.
 */
@SpringBootTest
@Transactional
@Sql(scripts = "/testdata/10-dados-base.sql")
class RegrasFinanceirasIntegrationTest {

  private static final Long ANA = 1L;
  private static final Long BRUNO = 2L;
  private static final Long CONTA_DA_ANA = 1L;
  private static final Long CONTA_DO_BRUNO = 2L;
  private static final Long CATEGORIA_MORADIA = 1L;
  private static final Long CATEGORIA_SALARIOS = 2L;
  private static final Long CATEGORIA_DO_BRUNO = 4L;

  private static final LocalDate INICIO = LocalDate.of(2026, 3, 1);
  private static final LocalDate FIM = LocalDate.of(2026, 3, 31);

  @Autowired private TituloService tituloService;
  @Autowired private DreService dreService;
  @Autowired private ResumoFinanceiroService resumoFinanceiroService;

  // ==================================================================
  // Apoio
  // ==================================================================

  /**
   * Compara valores monetários por <b>valor</b>, ignorando a escala.
   *
   * <p>Por que não usar {@code assertEquals} direto em {@code BigDecimal}:
   * {@code BigDecimal.equals} considera a escala, então
   * {@code new BigDecimal("0")} e {@code new BigDecimal("0.00")} são
   * <i>diferentes</i> para o JUnit, ainda que sejam o mesmo valor monetário. A
   * comparação correta entre BigDecimals é {@code compareTo}.
   */
  private static void verificarValor(String mensagem, String esperado, BigDecimal real) {
    assertEquals(0, new BigDecimal(esperado).compareTo(real), mensagem);
  }

  private Long criarTituloDespesa(String descricao, String valor, LocalDate vencimento) {
    return tituloService
        .criar(
            new TituloRequest(
                descricao,
                new BigDecimal(valor),
                vencimento,
                TipoMovimentacao.DESPESA,
                null,
                CATEGORIA_MORADIA,
                ANA))
        .idTitulo();
  }

  private Long criarTituloReceita(String descricao, String valor, LocalDate vencimento) {
    return tituloService
        .criar(
            new TituloRequest(
                descricao,
                new BigDecimal(valor),
                vencimento,
                TipoMovimentacao.RECEITA,
                null,
                CATEGORIA_SALARIOS,
                ANA))
        .idTitulo();
  }

  // ==================================================================
  // A REGRA CENTRAL: previsão não é realização
  // ==================================================================

  @Test
  @DisplayName("Cadastrar um título não cria despesa: a DRE realizada continua zerada")
  void tituloPendenteNaoEntraNaDreRealizada() {
    criarTituloDespesa("Conta de energia", "150.00", LocalDate.of(2026, 3, 10));

    DreResponse dre = dreService.gerar(ANA, INICIO, FIM, "comparativo");

    // O compromisso aparece como PREVISÃO...
    verificarValor(
        "O título pendente deve compor a DRE prevista", "150.00", dre.totalDespesasPrevistas());

    // ...mas nada foi realizado, então a DRE realizada não pode ser afetada.
    verificarValor(
        "Um título pendente NÃO pode ser contado como despesa realizada",
        "0.00",
        dre.totalDespesasRealizadas());
    verificarValor(
        "Receitas realizadas devem permanecer zeradas", "0.00", dre.totalReceitasRealizadas());
  }

  @Test
  @DisplayName("Pagar o título move o valor da DRE prevista para a DRE realizada")
  void pagamentoMoveOValorParaADreRealizada() {
    Long idTitulo = criarTituloDespesa("Conta de energia", "150.00", LocalDate.of(2026, 3, 10));

    // O pagamento acontece em 08/03, dentro do período.
    TituloResponse tituloPago =
        tituloService.pagar(
            idTitulo, new BigDecimal("150.00"), LocalDate.of(2026, 3, 8), CONTA_DA_ANA);

    assertEquals(SituacaoTitulo.PAGO, tituloPago.situacao());
    verificarValor("Valor realizado após quitação", "150.00", tituloPago.valorRealizado());
    verificarValor("Nada deve sobrar em aberto", "0.00", tituloPago.valorEmAberto());
    assertNotNull(tituloPago.dataPagamento(), "Título quitado precisa ter data de pagamento");

    DreResponse dre = dreService.gerar(ANA, INICIO, FIM, "comparativo");

    verificarValor("Previsão de despesas", "150.00", dre.totalDespesasPrevistas());
    verificarValor("Realização de despesas", "150.00", dre.totalDespesasRealizadas());
  }

  @Test
  @DisplayName("O valor realizado é reconhecido pela DATA DO PAGAMENTO, não pelo vencimento")
  void realizadoUsaDataDoPagamento() {
    Long idTitulo = criarTituloDespesa("Conta de energia", "150.00", LocalDate.of(2026, 3, 10));

    // Vence em março, mas é pago em abril.
    tituloService.pagar(idTitulo, new BigDecimal("150.00"), LocalDate.of(2026, 4, 5), CONTA_DA_ANA);

    DreResponse marco = dreService.gerar(ANA, INICIO, FIM, "comparativo");
    DreResponse abril =
        dreService.gerar(ANA, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), "comparativo");

    // Março tem a previsão (vencimento), mas nenhuma realização (o dinheiro saiu em abril).
    verificarValor("Março: previsão pelo vencimento", "150.00", marco.totalDespesasPrevistas());
    verificarValor("Março: nenhuma realização", "0.00", marco.totalDespesasRealizadas());

    // Abril tem a realização, mas não tem a previsão.
    verificarValor("Abril: sem previsão", "0.00", abril.totalDespesasPrevistas());
    verificarValor("Abril: realização pela data do pagamento", "150.00",
        abril.totalDespesasRealizadas());
  }

  // ==================================================================
  // Pagamento parcial
  // ==================================================================

  @Test
  @DisplayName("Pagamento parcial mantém o título PENDENTE e expõe o valor em aberto")
  void pagamentoParcialMantemPendente() {
    Long idTitulo = criarTituloDespesa("Aluguel", "1200.00", LocalDate.of(2026, 3, 5));

    TituloResponse aposPrimeiraParcela =
        tituloService.pagar(
            idTitulo, new BigDecimal("600.00"), LocalDate.of(2026, 3, 5), CONTA_DA_ANA);

    // "PAGO" significa QUITADO. Meio pago é PENDENTE com valor em aberto.
    assertEquals(SituacaoTitulo.PENDENTE, aposPrimeiraParcela.situacao());
    verificarValor("Realizado após 1ª parcela", "600.00", aposPrimeiraParcela.valorRealizado());
    verificarValor("Em aberto após 1ª parcela", "600.00", aposPrimeiraParcela.valorEmAberto());
    assertNull(aposPrimeiraParcela.dataPagamento(), "Título parcial não tem data de quitação");
    assertEquals(1, aposPrimeiraParcela.quantidadeMovimentacoes());

    // A DRE realizada reflete os 600 já pagos — proporcionalmente, sem esconder nada.
    DreResponse aposPrimeira = dreService.gerar(ANA, INICIO, FIM, "comparativo");
    verificarValor("Previsão total do aluguel", "1200.00", aposPrimeira.totalDespesasPrevistas());
    verificarValor("Realizado proporcional", "600.00", aposPrimeira.totalDespesasRealizadas());
  }

  @Test
  @DisplayName("A segunda parcela quita o título e soma as movimentações")
  void segundaParcelaQuitaOTitulo() {
    Long idTitulo = criarTituloDespesa("Aluguel", "1200.00", LocalDate.of(2026, 3, 5));

    tituloService.pagar(idTitulo, new BigDecimal("600.00"), LocalDate.of(2026, 3, 5), CONTA_DA_ANA);
    TituloResponse quitado =
        tituloService.pagar(
            idTitulo, new BigDecimal("600.00"), LocalDate.of(2026, 3, 20), CONTA_DA_ANA);

    assertEquals(SituacaoTitulo.PAGO, quitado.situacao());
    verificarValor("Soma das duas parcelas", "1200.00", quitado.valorRealizado());
    verificarValor("Em aberto após quitar", "0.00", quitado.valorEmAberto());
    assertEquals(2, quitado.quantidadeMovimentacoes());
    // A data de quitação é a do ÚLTIMO pagamento.
    assertEquals(LocalDate.of(2026, 3, 20), quitado.dataPagamento());

    DreResponse dre = dreService.gerar(ANA, INICIO, FIM, "comparativo");
    verificarValor("Realizado total", "1200.00", dre.totalDespesasRealizadas());
  }

  // ==================================================================
  // Regras de integridade
  // ==================================================================

  @Test
  @DisplayName("Não é possível pagar mais do que o valor em aberto do título")
  void naoPermitePagarAcimaDoValorEmAberto() {
    Long idTitulo = criarTituloDespesa("Conta de energia", "150.00", LocalDate.of(2026, 3, 10));

    RegraNegocioException erro =
        assertThrows(
            RegraNegocioException.class,
            () ->
                tituloService.pagar(
                    idTitulo, new BigDecimal("200.00"), LocalDate.of(2026, 3, 8), CONTA_DA_ANA));

    assertTrue(erro.getMessage().contains("maior que o valor em aberto"), erro.getMessage());

    // O título permanece intacto: nada foi gravado.
    assertEquals(SituacaoTitulo.PENDENTE, tituloService.buscarPorId(idTitulo).situacao());
  }

  @Test
  @DisplayName("Um título quitado não aceita novo pagamento")
  void naoPermitePagarTituloJaQuitado() {
    Long idTitulo = criarTituloDespesa("Conta de energia", "150.00", LocalDate.of(2026, 3, 10));

    tituloService.pagar(idTitulo, new BigDecimal("150.00"), LocalDate.of(2026, 3, 8), CONTA_DA_ANA);

    RegraNegocioException erro =
        assertThrows(
            RegraNegocioException.class,
            () ->
                tituloService.pagar(
                    idTitulo, new BigDecimal("10.00"), LocalDate.of(2026, 3, 9), CONTA_DA_ANA));

    assertTrue(erro.getMessage().contains("já está quitado"), erro.getMessage());
  }

  @Test
  @DisplayName("Um título cancelado não aceita pagamento e sai da previsão")
  void naoPermitePagarTituloCancelado() {
    Long idTitulo = criarTituloDespesa("Conta de energia", "150.00", LocalDate.of(2026, 3, 10));

    tituloService.cancelar(idTitulo);

    RegraNegocioException erro =
        assertThrows(
            RegraNegocioException.class,
            () ->
                tituloService.pagar(
                    idTitulo, new BigDecimal("150.00"), LocalDate.of(2026, 3, 8), CONTA_DA_ANA));

    assertTrue(erro.getMessage().contains("cancelado"), erro.getMessage());

    // Título cancelado desaparece da previsão: o compromisso deixou de existir.
    DreResponse dre = dreService.gerar(ANA, INICIO, FIM, "comparativo");
    verificarValor("Cancelado não conta como previsão", "0.00", dre.totalDespesasPrevistas());
  }

  @Test
  @DisplayName("Não é possível usar a conta de outro usuário para pagar um título")
  void naoPermiteUsarContaDeOutroUsuario() {
    Long idTitulo = criarTituloDespesa("Conta de energia", "150.00", LocalDate.of(2026, 3, 10));

    RegraNegocioException erro =
        assertThrows(
            RegraNegocioException.class,
            () ->
                tituloService.pagar(
                    idTitulo,
                    new BigDecimal("150.00"),
                    LocalDate.of(2026, 3, 8),
                    CONTA_DO_BRUNO));

    assertTrue(erro.getMessage().contains("outro usuário"), erro.getMessage());
  }

  @Test
  @DisplayName("Não é possível classificar uma receita em categoria de despesa")
  void naoPermiteCategoriaDeTipoIncompativel() {
    RegraNegocioException erro =
        assertThrows(
            RegraNegocioException.class,
            () ->
                tituloService.criar(
                    new TituloRequest(
                        "Freelance",
                        new BigDecimal("800.00"),
                        LocalDate.of(2026, 3, 25),
                        TipoMovimentacao.RECEITA,
                        null,
                        // Categoria "Moradia" é de DESPESA.
                        CATEGORIA_MORADIA,
                        ANA)));

    assertTrue(erro.getMessage().contains("não pode ser usada"), erro.getMessage());
  }

  @Test
  @DisplayName("Não é possível usar a categoria de outro usuário")
  void naoPermiteCategoriaDeOutroUsuario() {
    RegraNegocioException erro =
        assertThrows(
            RegraNegocioException.class,
            () ->
                tituloService.criar(
                    new TituloRequest(
                        "Conta",
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 3, 10),
                        TipoMovimentacao.DESPESA,
                        null,
                        CATEGORIA_DO_BRUNO,
                        ANA)));

    assertTrue(erro.getMessage().contains("outro usuário"), erro.getMessage());
  }

  // ==================================================================
  // Situação derivada (VENCIDO)
  // ==================================================================

  @Test
  @DisplayName("VENCIDO é derivado: o banco guarda PENDENTE")
  void vencidoEhDerivadoENaoArmazenado() {
    Long idTitulo = criarTituloDespesa("Conta de energia", "150.00", LocalDate.of(2026, 3, 10));

    var titulo = tituloService.buscarEntidade(idTitulo);

    // Armazenado: sempre PENDENTE.
    assertEquals(SituacaoTitulo.PENDENTE, titulo.getSituacao());

    // Derivado: depende da data de referência.
    assertEquals(
        SituacaoTituloEfetiva.PENDENTE,
        titulo.getSituacaoEfetiva(LocalDate.of(2026, 3, 1)),
        "Antes do vencimento, é PENDENTE");
    assertEquals(
        SituacaoTituloEfetiva.VENCIDO,
        titulo.getSituacaoEfetiva(LocalDate.of(2026, 3, 11)),
        "Depois do vencimento, é VENCIDO");
  }

  @Test
  @DisplayName("A DRE sinaliza os títulos vencidos sem somá-los ao resultado")
  void dreSinalizaTitulosVencidos() {
    tituloService.criar(
        new TituloRequest(
            "Conta atrasada",
            new BigDecimal("400.00"),
            LocalDate.now().minusDays(5),
            TipoMovimentacao.DESPESA,
            null,
            CATEGORIA_MORADIA,
            ANA));

    LocalDate hoje = LocalDate.now();
    DreResponse dre = dreService.gerar(ANA, hoje.minusDays(30), hoje, "comparativo");

    assertEquals(1, dre.titulosVencidos().size(), "Deve haver 1 título vencido");
    verificarValor("Total vencido", "400.00", dre.totalVencido());
    assertEquals(5L, dre.titulosVencidos().get(0).diasDeAtraso(), "5 dias de atraso");
    // O vencido NÃO altera o resultado realizado.
    verificarValor("Vencido não é realizado", "0.00", dre.totalDespesasRealizadas());
  }

  // ==================================================================
  // DRE por categoria (as "linhas" do relatório)
  // ==================================================================

  @Test
  @DisplayName("A DRE agrupa por categoria, formando as linhas do relatório")
  void dreAgrupaPorCategoria() {
    Long salario = criarTituloReceita("Salário", "5000.00", LocalDate.of(2026, 3, 5));
    Long moradia = criarTituloDespesa("Aluguel", "1500.00", LocalDate.of(2026, 3, 10));

    // Recebe o salário integralmente e paga metade do aluguel.
    tituloService.receber(
        salario, new BigDecimal("5000.00"), LocalDate.of(2026, 3, 5), CONTA_DA_ANA);
    tituloService.pagar(moradia, new BigDecimal("750.00"), LocalDate.of(2026, 3, 10), CONTA_DA_ANA);

    DreResponse dre = dreService.gerar(ANA, INICIO, FIM, "comparativo");

    // Bloco de receitas
    assertEquals(1, dre.linhasReceitas().size());
    assertEquals("Salarios", dre.linhasReceitas().get(0).categoriaNome());
    verificarValor("Previsão de receita", "5000.00", dre.linhasReceitas().get(0).valorPrevisto());
    verificarValor("Realização de receita", "5000.00", dre.linhasReceitas().get(0).valorRealizado());

    // Bloco de despesas
    assertEquals(1, dre.linhasDespesas().size());
    assertEquals("Moradia", dre.linhasDespesas().get(0).categoriaNome());
    verificarValor("Previsão de despesa", "1500.00", dre.linhasDespesas().get(0).valorPrevisto());
    verificarValor("Realização de despesa", "750.00", dre.linhasDespesas().get(0).valorRealizado());

    // Resultado do período = receitas - despesas
    verificarValor("Resultado previsto", "3500.00", dre.resultadoPrevisto());
    verificarValor("Resultado realizado", "4250.00", dre.resultadoRealizado());

    // Recebeu tudo o que previu e gastou menos do que previu -> favorável.
    assertTrue(dre.variacaoFavoravel(), "Gastar menos que o previsto é favorável");
    assertTrue(dre.variacaoResultado().compareTo(BigDecimal.ZERO) > 0);
  }

  @Test
  @DisplayName("A variação é normalizada: gastar a mais é negativo, receber a mais é positivo")
  void variacaoTemSinalNormalizado() {
    Long salario = criarTituloReceita("Salário", "5000.00", LocalDate.of(2026, 3, 5));
    Long lazer = criarTituloDespesa("Cinema", "100.00", LocalDate.of(2026, 3, 15));

    // Previsão de receita maior que a realização: há um bônus previsto que não entrou.
    criarTituloReceita("Bônus previsto", "1000.00", LocalDate.of(2026, 3, 6));

    tituloService.receber(
        salario, new BigDecimal("5000.00"), LocalDate.of(2026, 3, 5), CONTA_DA_ANA);
    tituloService.pagar(lazer, new BigDecimal("100.00"), LocalDate.of(2026, 3, 15), CONTA_DA_ANA);

    DreResponse dre = dreService.gerar(ANA, INICIO, FIM, "comparativo");

    var linhaReceita =
        dre.linhasReceitas().stream()
            .filter(l -> l.categoriaNome().equals("Salarios"))
            .findFirst()
            .orElseThrow();
    var linhaDespesa =
        dre.linhasDespesas().stream()
            .filter(l -> l.categoriaNome().equals("Moradia"))
            .findFirst()
            .orElseThrow();

    // Receita: previsto 6000 (5000 + 1000), realizado 5000 -> variação -1000.
    verificarValor("Previsão de receita agrupada", "6000.00", linhaReceita.valorPrevisto());
    verificarValor("Realização de receita", "5000.00", linhaReceita.valorRealizado());
    verificarValor("Receber menos que o previsto é desfavorável", "-1000.00",
        linhaReceita.variacao());

    // Despesa: previsto 100, realizado 100 -> sem variação.
    verificarValor("Previsão de despesa", "100.00", linhaDespesa.valorPrevisto());
    verificarValor("Realização de despesa", "100.00", linhaDespesa.valorRealizado());
    verificarValor("Sem variação quando previsto = realizado", "0.00", linhaDespesa.variacao());
  }

  @Test
  @DisplayName("Gastar menos que o previsto produz variação positiva (favorável)")
  void gastarMenosEhVariacaoPositiva() {
    Long moradia = criarTituloDespesa("Aluguel", "1500.00", LocalDate.of(2026, 3, 10));
    tituloService.pagar(moradia, new BigDecimal("1200.00"), LocalDate.of(2026, 3, 10), CONTA_DA_ANA);

    DreResponse dre = dreService.gerar(ANA, INICIO, FIM, "comparativo");

    var linha =
        dre.linhasDespesas().stream()
            .filter(l -> l.categoriaNome().equals("Moradia"))
            .findFirst()
            .orElseThrow();

    verificarValor("Gastou 300 a menos que o previsto", "300.00", linha.variacao());
  }

  // ==================================================================
  // Contas a pagar e a receber
  // ==================================================================

  @Test
  @DisplayName("Contas a pagar somam o valor EM ABERTO, não o valor previsto")
  void contasAPagarSomamOValorEmAberto() {
    LocalDate referencia = LocalDate.of(2026, 3, 15);

    // Aluguel de 1.200 com metade paga: só 600 continuam em aberto.
    Long aluguel = criarTituloDespesa("Aluguel", "1200.00", LocalDate.of(2026, 3, 20));
    tituloService.pagar(aluguel, new BigDecimal("600.00"), LocalDate.of(2026, 3, 10), CONTA_DA_ANA);

    // Conta de luz ainda intocada: 150 em aberto.
    criarTituloDespesa("Conta de energia", "150.00", LocalDate.of(2026, 3, 25));

    PosicaoTitulosResponse posicao =
        resumoFinanceiroService.contasAPagar(ANA, referencia, null);

    assertEquals(2, posicao.quantidadeTitulos());
    // 600 (o que falta do aluguel) + 150 (luz) = 750. Se somasse o valor previsto,
    // daria 1.350 — e o usuário acharia que deve mais do que realmente deve.
    verificarValor("Soma apenas o que falta pagar", "750.00", posicao.totalEmAberto());
    assertTrue(
        posicao.totalEmAberto().compareTo(new BigDecimal("1350.00")) < 0,
        "O total não pode ser a soma dos valores previstos");

    // O aluguel é parcial e está no prazo.
    var itemAluguel =
        posicao.titulos().stream()
            .filter(t -> t.descricao().equals("Aluguel"))
            .findFirst()
            .orElseThrow();
    assertEquals(SituacaoEmAberto.PARCIAL, itemAluguel.situacao());
    verificarValor("Valor já pago do aluguel", "600.00", itemAluguel.valorRealizado());
    verificarValor("Falta pagar do aluguel", "600.00", itemAluguel.valorEmAberto());
    assertEquals(1, posicao.quantidadeParciais());
  }

  @Test
  @DisplayName("Título quitado e título cancelado não são contas a pagar")
  void quitadoECanceladoNaoSaoContasAPagar() {
    LocalDate referencia = LocalDate.of(2026, 3, 15);

    // Quitado: nada mais é devido.
    Long pago = criarTituloDespesa("Conta quitada", "150.00", LocalDate.of(2026, 3, 10));
    tituloService.pagar(pago, new BigDecimal("150.00"), LocalDate.of(2026, 3, 8), CONTA_DA_ANA);

    // Cancelado: nunca foi obrigação de verdade.
    Long cancelado = criarTituloDespesa("Conta cancelada", "200.00", LocalDate.of(2026, 3, 12));
    tituloService.cancelar(cancelado);

    // O único que realmente está em aberto.
    criarTituloDespesa("Conta em aberto", "100.00", LocalDate.of(2026, 3, 20));

    PosicaoTitulosResponse posicao =
        resumoFinanceiroService.contasAPagar(ANA, referencia, null);

    assertEquals(1, posicao.quantidadeTitulos(), "Só o título em aberto deve aparecer");
    assertEquals("Conta em aberto", posicao.titulos().get(0).descricao());
    verificarValor("Total em aberto", "100.00", posicao.totalEmAberto());
  }

  @Test
  @DisplayName("A posição separa o que está vencido do que ainda vai vencer")
  void posicaoSeparaVencidoDeAVencer() {
    LocalDate referencia = LocalDate.of(2026, 3, 15);

    criarTituloDespesa("Vencida há 5 dias", "200.00", LocalDate.of(2026, 3, 10));
    criarTituloDespesa("Vence hoje", "50.00", referencia);
    criarTituloDespesa("Vence em 10 dias", "300.00", LocalDate.of(2026, 3, 25));
    // 15/03 + 95 dias = 18/06: fica fora de uma janela de 90 dias.
    criarTituloDespesa("Vence em 95 dias", "900.00", LocalDate.of(2026, 6, 18));

    PosicaoTitulosResponse posicao =
        resumoFinanceiroService.contasAPagar(ANA, referencia, 30);

    assertEquals(4, posicao.quantidadeTitulos());
    verificarValor("Tudo o que está em aberto", "1450.00", posicao.totalEmAberto());

    // Vencido: só a que passou da data de referência.
    assertEquals(1, posicao.quantidadeVencidos());
    verificarValor("Total vencido", "200.00", posicao.totalVencido());
    assertEquals(SituacaoEmAberto.VENCIDO, posicao.vencidos().get(0).situacao());
    assertEquals(5L, posicao.vencidos().get(0).diasParaVencimento());

    // Próximos 30 dias: vence hoje (0) e vence em 10 dias. A de 95 dias fica fora.
    assertEquals(2, posicao.quantidadeAVencerProximosDias());
    verificarValor("Total a vencer em 30 dias", "350.00", posicao.totalAVencerProximosDias());

    // A janela é configurável: com 95 dias, a quarta parcela entra.
    PosicaoTitulosResponse comJanelaMaior =
        resumoFinanceiroService.contasAPagar(ANA, referencia, 95);
    assertEquals(3, comJanelaMaior.quantidadeAVencerProximosDias());
    verificarValor("Total a vencer em 95 dias", "1250.00",
        comJanelaMaior.totalAVencerProximosDias());

    // A janela é INCLUSIVA na última ponta: um título que vence exatamente no
    // 90º dia (13/06) entra em uma janela de 90 dias.
    criarTituloDespesa("Vence no limite", "77.00", referencia.plusDays(90));
    PosicaoTitulosResponse noLimite = resumoFinanceiroService.contasAPagar(ANA, referencia, 90);
    assertTrue(
        noLimite.aVencerProximosDias().stream()
            .anyMatch(t -> t.descricao().equals("Vence no limite")),
        "O título que vence no último dia da janela deve entrar nela");
  }

  @Test
  @DisplayName("O vencimento de hoje ainda não está vencido")
  void vencimentoDeHojeNaoEstaVencido() {
    LocalDate referencia = LocalDate.of(2026, 3, 15);
    criarTituloDespesa("Vence hoje", "100.00", referencia);

    PosicaoTitulosResponse posicao =
        resumoFinanceiroService.contasAPagar(ANA, referencia, null);

    assertEquals(0, posicao.quantidadeVencidos(), "O dia do vencimento ainda está no prazo");
    assertEquals(1, posicao.quantidadeAVencerProximosDias());
    assertEquals(SituacaoEmAberto.A_VENCER, posicao.titulos().get(0).situacao());
  }

  @Test
  @DisplayName("Contas a pagar e a receber são separadas pelo tipo do título")
  void contasAPagarEAReceberSaoSeparadasPeloTipo() {
    LocalDate referencia = LocalDate.of(2026, 3, 15);

    criarTituloDespesa("Aluguel", "1200.00", LocalDate.of(2026, 3, 20));
    criarTituloReceita("Salário", "5000.00", LocalDate.of(2026, 3, 5));

    PosicaoTitulosResponse aPagar = resumoFinanceiroService.contasAPagar(ANA, referencia, null);
    PosicaoTitulosResponse aReceber =
        resumoFinanceiroService.contasAReceber(ANA, referencia, null);

    assertEquals(1, aPagar.quantidadeTitulos());
    assertEquals("Aluguel", aPagar.titulos().get(0).descricao());
    verificarValor("Total a pagar", "1200.00", aPagar.totalEmAberto());

    assertEquals(1, aReceber.quantidadeTitulos());
    assertEquals("Salário", aReceber.titulos().get(0).descricao());
    verificarValor("Total a receber", "5000.00", aReceber.totalEmAberto());
  }

  @Test
  @DisplayName("O resumo consolidado cruza os dois lados no saldo previsto")
  void resumoConsolidadoCalculaSaldoPrevisto() {
    LocalDate referencia = LocalDate.of(2026, 3, 15);

    // A receber: salário de 1.500 ainda em aberto.
    criarTituloReceita("Salário", "1500.00", LocalDate.of(2026, 3, 20));

    // A pagar: 800 (aluguel parcial) + 150 (luz) = 950.
    Long aluguel = criarTituloDespesa("Aluguel", "1200.00", LocalDate.of(2026, 3, 20));
    tituloService.pagar(aluguel, new BigDecimal("400.00"), LocalDate.of(2026, 3, 10), CONTA_DA_ANA);
    criarTituloDespesa("Conta de luz", "150.00", LocalDate.of(2026, 3, 25));

    ResumoFinanceiroResponse resumo =
        resumoFinanceiroService.resumo(ANA, referencia, null);

    verificarValor("O que falta receber", "1500.00", resumo.getTotalAReceber());
    verificarValor("O que falta pagar", "950.00", resumo.getTotalAPagar());
    verificarValor("Saldo previsto = a receber - a pagar", "550.00", resumo.saldoPrevisto());
    assertTrue(resumo.saldoPrevistoPositivo(), "Receber 1.500 cobre pagar 950");
  }

  @Test
  @DisplayName("A posição não enxerga títulos de outro usuário")
  void posicaoNaoVazaDadosDeOutroUsuario() {
    LocalDate referencia = LocalDate.of(2026, 3, 15);
    criarTituloDespesa("Conta da Ana", "150.00", LocalDate.of(2026, 3, 20));

    PosicaoTitulosResponse doBruno = resumoFinanceiroService.contasAPagar(BRUNO, referencia, null);

    assertEquals(0, doBruno.quantidadeTitulos());
    verificarValor("Bruno não vê as contas da Ana", "0.00", doBruno.totalEmAberto());
  }

  // ==================================================================
  // Isolamento entre usuários
  // ==================================================================

  @Test
  @DisplayName("A DRE de um usuário não enxerga dados de outro")
  void dreNaoVazaDadosDeOutroUsuario() {
    criarTituloDespesa("Conta da Ana", "150.00", LocalDate.of(2026, 3, 10));

    DreResponse dreDoBruno = dreService.gerar(BRUNO, INICIO, FIM, "comparativo");

    verificarValor("Bruno não vê a previsão da Ana", "0.00", dreDoBruno.totalDespesasPrevistas());
    assertTrue(dreDoBruno.linhasDespesas().isEmpty());
  }
}
