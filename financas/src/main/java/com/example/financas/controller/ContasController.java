package com.example.financas.controller;

import com.example.financas.dto.response.PosicaoTitulosResponse;
import com.example.financas.dto.response.ResumoFinanceiroResponse;
import com.example.financas.service.ResumoFinanceiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Endpoints de <b>contas a pagar</b> e <b>contas a receber</b>.
 *
 * <p>Estes endpoints não criam nada: são <b>visões</b> sobre os títulos que já
 * existem. Cadastrar, pagar, receber e cancelar continuam sendo operações de
 * {@code /api/v1/titulos} — este controller apenas responde "quanto está em
 * aberto e com que urgência".
 *
 * <p>A data de referência é um parâmetro em todos eles. Isso tem duas
 * utilidades: permite simular cenários ("como fica minha posição no fim do
 * mês?") e torna o comportamento testável sem depender do relógio da máquina.
 */
@Tag(
    name = "Relatórios",
    description =
        "DRE (previsto, realizado e comparativo) e a posição de contas a pagar e a receber.")
@RestController
@RequestMapping("/api/v1")
public class ContasController {

  private final ResumoFinanceiroService resumoFinanceiroService;

  public ContasController(ResumoFinanceiroService resumoFinanceiroService) {
    this.resumoFinanceiroService = resumoFinanceiroService;
  }

  @Operation(
      summary = "Resumo consolidado: a pagar, a receber e saldo previsto",
      description =
          """
          Situação consolidada em uma única chamada: quanto falta pagar, quanto falta
          receber e a diferença entre os dois.

          ## O que entra na conta

          Somente títulos com `situacao = PENDENTE` — ou seja, **exclui o que já foi pago**
          (nada mais é devido) e **o que foi cancelado** (nunca foi obrigação de verdade).

          Os totais somam **`valorEmAberto`**, nunca o valor previsto: um aluguel de
          R$ 1.200,00 com R$ 600,00 já pagos entra como R$ 600,00. Somar o valor previsto
          inflaria a dívida.

          ## `saldoPrevisto` é separado do saldo das contas

          `saldoPrevisto = totalAReceber − totalAPagar`. Ele **não** considera o dinheiro
          que já está na conta.

          Para o dinheiro que já está lá, use `GET /api/v1/contas?usuarioId=`. Somar os dois
          produziria um número enganoso: o saldo bancário é passado (fatos consumados) e as
          contas em aberto são futuro (previsões).
          """)
  @GetMapping("/resumo-financeiro")
  public ResumoFinanceiroResponse resumo(
      @RequestParam Long usuarioId,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataReferencia,
      @RequestParam(required = false) Integer janelaDias) {
    return resumoFinanceiroService.resumo(usuarioId, dataReferencia, janelaDias);
  }

  @Operation(
      summary = "Contas a pagar: o que ainda falta pagar",
      description =
          """
          Lista os títulos de **DESPESA** com valor ainda em aberto, ordenados por
          vencimento — os vencidos aparecem no topo.

          ## Parâmetros

          - `dataReferencia` (opcional) — a data que define o que está vencido. Permite
            simular cenários ("como fica minha posição no fim do mês?") e torna o
            comportamento testável sem depender do relógio da máquina. Omitida, usa hoje.
          - `janelaDias` (opcional, padrão 30) — quantos dias à frente considerar em
            "próximos dias". A janela é **inclusiva** nas duas pontas.

          ## O que a resposta traz

          - `titulos` — todos os que têm valor em aberto;
          - `vencidos` e `aVencerProximosDias` — recortes já prontos e ordenados, para o
            cliente não reimplementar a mesma regra de corte;
          - `totalEmAberto`, `totalVencido` e `totalAVencerProximosDias`;
          - contagens de títulos, vencidos e parciais.

          Cada item traz `situacao`: A_VENCER, VENCIDO, PARCIAL ou PARCIAL_VENCIDO. O campo
          `diasParaVencimento` é **positivo quando já venceu** (dias de atraso) e negativo
          quando ainda falta.

          ## Por que não existe entidade "ContaAPagar"

          Contas a pagar são os títulos de despesa vistos por outro ângulo. Criar uma
          tabela própria duplicaria os mesmos campos (descrição, valor, vencimento,
          situação) e criaria duas fontes de verdade que podem divergir.
          """)
  @GetMapping("/contas-a-pagar")
  public PosicaoTitulosResponse contasAPagar(
      @RequestParam Long usuarioId,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataReferencia,
      @RequestParam(required = false) Integer janelaDias) {
    return resumoFinanceiroService.contasAPagar(usuarioId, dataReferencia, janelaDias);
  }

  @Operation(
      summary = "Contas a receber: o que ainda falta receber",
      description =
          """
          Operação espelho de `/contas-a-pagar`, para títulos de **RECEITA**.

          Mesmos parâmetros, mesma estrutura de resposta e mesmas regras: só entram títulos
          pendentes, e os totais somam o valor **em aberto**, não o previsto.

          Os dois endpoints existirem separados é apenas uma conveniência de leitura — no
          domínio, ambos são a mesma entidade `Titulo` vista de dois ângulos.
          """)
  @GetMapping("/contas-a-receber")
  public PosicaoTitulosResponse contasAReceber(
      @RequestParam Long usuarioId,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataReferencia,
      @RequestParam(required = false) Integer janelaDias) {
    return resumoFinanceiroService.contasAReceber(usuarioId, dataReferencia, janelaDias);
  }
}
