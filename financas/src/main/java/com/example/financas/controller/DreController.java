package com.example.financas.controller;

import com.example.financas.dto.response.DreResponse;
import com.example.financas.service.DreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Endpoint que gera a <b>DRE para finanças pessoais</b>.
 *
 * <p>Exemplo de chamada:
 *
 * <pre>
 * GET /api/v1/dre?usuarioId=1&amp;inicio=2026-03-01&amp;fim=2026-03-31&amp;modo=comparativo
 * </pre>
 *
 * <p>O parâmetro {@code modo} escolhe a leitura das mesmas datas:
 *
 * <ul>
 *   <li>{@code previsto} — filtra títulos por <b>vencimento</b> (o que estava
 *       planejado para o período);</li>
 *   <li>{@code realizado} — filtra movimentações por <b>data</b> (o que de fato
 *       aconteceu no período);</li>
 *   <li>{@code comparativo} — as duas leituras lado a lado, com a variação.
 *       É o padrão.</li>
 * </ul>
 *
 * <p>Não existe endpoint para "gravar" a DRE: ela é sempre derivada dos fatos,
 * e por isso nunca diverge dos lançamentos que a originaram.
 */
@Tag(
    name = "Relatórios",
    description =
        "DRE (previsto, realizado e comparativo) e a posição de contas a pagar e a receber.")
@RestController
@RequestMapping("/api/v1/dre")
public class DreController {

  private final DreService dreService;

  public DreController(DreService dreService) {
    this.dreService = dreService;
  }

  @Operation(
      summary = "Gera a DRE do período",
      description =
          """
          Demonstração do Resultado adaptada para finanças pessoais, agrupada pelas
          **categorias** do usuário.

          ## Modos de apuração

          O parâmetro `modo` define qual data é filtrada — e é isso que separa previsão de
          realização:

          | modo | tabela lida | data filtrada | regime |
          |---|---|---|---|
          | `previsto` | título | `dataVencimento` | competência |
          | `realizado` | movimentação | `data` | caixa |
          | `comparativo` | as duas | ambas | os dois, lado a lado (**padrão**) |

          ## A regra central

          **Um título nunca entra na DRE realizada. Só a movimentação entra.**

          Na prática: uma conta de energia que vence dia 10 e **ainda não foi paga**
          aparece na DRE prevista e **não** na realizada. Ao registrar o pagamento, ela
          passa a compor o resultado realizado na **data do pagamento**, não na do
          vencimento.

          ## Estrutura da resposta

          - `linhasReceitas` e `linhasDespesas` — uma linha por categoria, com
            `valorPrevisto`, `valorRealizado` e `variacao`;
          - `resultadoPrevisto` e `resultadoRealizado` — o resultado do período;
          - `variacaoResultado` e `variacaoFavoravel` — a diferença, com sinal
            normalizado de modo que **positivo seja favorável**;
          - `titulosVencidos` — pendentes já atrasados, que **não** alteram o resultado.

          As linhas vêm das categorias cadastradas, não de uma lista fixa: o relatório se
          adapta a qualquer usuário.

          **Erros:** 400 para data em formato inválido; 404 se o usuário não existir;
          422 se a data de início for posterior à de fim.
          """)
  @GetMapping
  public DreResponse gerar(
      @RequestParam Long usuarioId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
      @RequestParam(defaultValue = "comparativo") String modo) {
    return dreService.gerar(usuarioId, inicio, fim, modo);
  }
}
