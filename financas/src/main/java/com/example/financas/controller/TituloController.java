package com.example.financas.controller;

import com.example.financas.dto.request.PagamentoRequest;
import com.example.financas.dto.response.ErroResponse;
import com.example.financas.dto.request.RecebimentoRequest;
import com.example.financas.dto.request.TituloRequest;
import com.example.financas.dto.response.TituloResponse;
import com.example.financas.entity.SituacaoTitulo;
import com.example.financas.entity.TipoMovimentacao;
import com.example.financas.service.TituloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints de <b>tÃ­tulos</b> â€” os compromissos financeiros previstos.
 *
 * <p>Este Ã© o controller que materializa a separaÃ§Ã£o entre previsÃ£o e
 * realizaÃ§Ã£o:
 *
 * <pre>
 *   POST /titulos                  -> cria a PREVISÃƒO. Nada acontece no caixa
 *                                     e nada muda na DRE realizada.
 *   POST /titulos/{id}/pagar       -> registra a REALIZAÃ‡ÃƒO (despesa paga):
 *                                     cria a movimentaÃ§Ã£o e atualiza o tÃ­tulo.
 *   POST /titulos/{id}/receber     -> registra a REALIZAÃ‡ÃƒO (receita recebida).
 *   POST /titulos/{id}/cancelar    -> desfaz a previsÃ£o, se nada foi realizado.
 * </pre>
 *
 * <p>Repare que nÃ£o existe endpoint para "criar a despesa de um tÃ­tulo". Essa
 * operaÃ§Ã£o simplesmente nÃ£o existe: despesa Ã© consequÃªncia de pagamento, nunca
 * de cadastro.
 */
@Tag(
    name = "TÃ­tulos",
    description =
        """
        Os **compromissos previstos** â€” o que se tem a pagar ou a receber.

        Cadastrar um tÃ­tulo **nÃ£o** cria despesa: nenhum valor sai ou entra, e a DRE
        realizada permanece inalterada. A despesa passa a existir quando o tÃ­tulo Ã© pago ou
        recebido.
        """)
@RestController
@RequestMapping("/api/v1/titulos")
public class TituloController {

  private final TituloService tituloService;

  public TituloController(TituloService tituloService) {
    this.tituloService = tituloService;
  }

  @Operation(
      summary = "Lista os tÃ­tulos (previstos)",
      description =
          """
          Lista os tÃ­tulos do usuÃ¡rio, com filtros opcionais:

          - `tipo` â€” RECEITA (a receber) ou DESPESA (a pagar);
          - `situacao` â€” a situaÃ§Ã£o **armazenada**: PENDENTE, PAGO ou CANCELADO.

          Para a situaÃ§Ã£o **efetiva** (que inclui VENCIDO), consulte o campo
          `situacaoEfetiva` de cada item. VENCIDO nÃ£o Ã© armazenado: Ã© derivado da
          comparaÃ§Ã£o entre o vencimento e a data de hoje.
          """)
  @GetMapping
  public List<TituloResponse> listar(
      @RequestParam Long usuarioId,
      @RequestParam(required = false) TipoMovimentacao tipo,
      @RequestParam(required = false) SituacaoTitulo situacao) {
    return tituloService.listar(usuarioId, tipo, situacao);
  }

  @Operation(
      summary = "Busca um tÃ­tulo por id",
      description = "Responde 404 se o id nÃ£o existir.")
  @GetMapping("/{id}")
  public TituloResponse buscarPorId(@PathVariable Long id) {
    return tituloService.buscarPorId(id);
  }

  @Operation(
      summary = "Lista os tÃ­tulos vencidos",
      description =
          """
          Lista os tÃ­tulos **pendentes com vencimento jÃ¡ ultrapassado**.

          Um tÃ­tulo cancelado nÃ£o Ã© atraso, e um jÃ¡ quitado tambÃ©m nÃ£o â€” por isso sÃ³ os
          pendentes aparecem.

          O parÃ¢metro `referencia` permite simular outra data; quando omitido, usa hoje.
          """)
  @GetMapping("/vencidos")
  public List<TituloResponse> vencidos(
      @RequestParam Long usuarioId,
      @RequestParam(required = false) LocalDate referencia) {
    return tituloService.listarVencidos(
        usuarioId, referencia != null ? referencia : LocalDate.now());
  }

  @Operation(
      summary = "Cadastra um tÃ­tulo (previsÃ£o)",
      description =
          """
          Registra um compromisso futuro. **Nada acontece no caixa** e a DRE realizada nÃ£o
          muda: o tÃ­tulo passa a existir apenas como previsÃ£o.

          A categoria Ã© obrigatÃ³ria e precisa ser do mesmo tipo do tÃ­tulo (RN03) â€” uma
          receita nÃ£o pode ser classificada em "Moradia", por exemplo.

          O tÃ­tulo nasce com `situacao = PENDENTE` e `valorRealizado = 0`. A situaÃ§Ã£o
          `situacaoEfetiva` jÃ¡ vem como VENCIDO se o vencimento informado for no passado.

          **Erros:** 400 para campo invÃ¡lido; 404 se usuÃ¡rio ou categoria nÃ£o existirem;
          422 se a categoria for de outro usuÃ¡rio ou de tipo incompatÃ­vel.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Título criado, com situação PENDENTE e valor realizado zerado.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = TituloResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "RequisiÃ§Ã£o invÃ¡lida: campo reprovado, JSON malformado, enum ou data invÃ¡lidos.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Recurso nÃ£o encontrado (id inexistente).",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description = "Regra de negÃ³cio violada.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "Erro interno inesperado, sem expor detalhe interno.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class)))
  })
  @PostMapping
  public ResponseEntity<TituloResponse> criar(@Valid @RequestBody TituloRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(tituloService.criar(request));
  }

  @Operation(
      summary = "Atualiza um tÃ­tulo pendente",
      description =
          """
          Altera descriÃ§Ã£o, valor previsto, vencimento, categoria e observaÃ§Ã£o.

          **RestriÃ§Ãµes** (422 se violadas):

          - o `tipo` **nÃ£o pode ser alterado** â€” para mudar a direÃ§Ã£o, cadastre outro tÃ­tulo;
          - um tÃ­tulo **cancelado** nÃ£o pode ser alterado;
          - um tÃ­tulo **quitado** nÃ£o pode ser alterado;
          - o novo `valorPrevisto` nÃ£o pode ser **menor que o valor jÃ¡ realizado**.
          """)
  @PutMapping("/{id}")
  public TituloResponse atualizar(
      @PathVariable Long id, @Valid @RequestBody TituloRequest request) {
    return tituloService.atualizar(id, request);
  }

  @Operation(
      summary = "Paga um tÃ­tulo de despesa",
      description =
          """
          **Esta Ã© a operaÃ§Ã£o que transforma previsÃ£o em realizaÃ§Ã£o.**

          Registra o pagamento, cria a **movimentaÃ§Ã£o** correspondente (valor realizado,
          data do pagamento, conta utilizada) e, se a soma dos pagamentos atingir o valor
          previsto, marca o tÃ­tulo como PAGO.

          ## Pagamento parcial Ã© permitido

          O `valor` pode ser **menor** que o valor previsto. Nesse caso o tÃ­tulo continua
          PENDENTE e o quanto falta aparece em `valorEmAberto`. Ã‰ assim que se registra um
          aluguel pago em duas vezes.

          "PAGO" significa **quitado**, nÃ£o "teve algum pagamento".

          ## O que esta operaÃ§Ã£o valida

          - o tÃ­tulo precisa ser do tipo DESPESA â€” para receita, use `/receber` (422);
          - o tÃ­tulo nÃ£o pode estar cancelado nem jÃ¡ quitado (422);
          - o valor nÃ£o pode ultrapassar o que estÃ¡ em aberto (422);
          - a conta precisa pertencer ao mesmo usuÃ¡rio do tÃ­tulo (422).

          Tudo acontece em uma Ãºnica transaÃ§Ã£o: ou a movimentaÃ§Ã£o Ã© criada **e** o tÃ­tulo
          atualizado, ou nada Ã© gravado.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "OperaÃ§Ã£o concluÃ­da. O tÃ­tulo devolvido traz a situaÃ§Ã£o atualizada.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = TituloResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "RequisiÃ§Ã£o invÃ¡lida: campo reprovado, JSON malformado, enum ou data invÃ¡lidos.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Recurso nÃ£o encontrado (id inexistente).",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description = "Regra de negÃ³cio violada.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "Erro interno inesperado, sem expor detalhe interno.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class)))
  })
  @PostMapping("/{id}/pagar")
  public TituloResponse pagar(
      @PathVariable Long id, @Valid @RequestBody PagamentoRequest request) {
    return tituloService.pagar(id, request.valor(), request.data(), request.contaId());
  }

  @Operation(
      summary = "Recebe um tÃ­tulo de receita",
      description =
          """
          OperaÃ§Ã£o espelho de `/pagar`, para tÃ­tulos do tipo RECEITA.

          Registra que o dinheiro **efetivamente entrou**, cria a movimentaÃ§Ã£o de RECEITA e
          quita o tÃ­tulo quando o total recebido atinge o valor previsto.

          Aceita recebimento parcial, pelas mesmas regras do pagamento: o tÃ­tulo permanece
          PENDENTE enquanto faltar valor.

          **Erros:** 422 se o tÃ­tulo for do tipo DESPESA (use `/pagar`), se estiver
          cancelado ou quitado, se o valor ultrapassar o em aberto, ou se a conta for de
          outro usuÃ¡rio.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "OperaÃ§Ã£o concluÃ­da. O tÃ­tulo devolvido traz a situaÃ§Ã£o atualizada.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = TituloResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "RequisiÃ§Ã£o invÃ¡lida: campo reprovado, JSON malformado, enum ou data invÃ¡lidos.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Recurso nÃ£o encontrado (id inexistente).",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description = "Regra de negÃ³cio violada.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "Erro interno inesperado, sem expor detalhe interno.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class)))
  })
  @PostMapping("/{id}/receber")
  public TituloResponse receber(
      @PathVariable Long id, @Valid @RequestBody RecebimentoRequest request) {
    return tituloService.receber(id, request.valor(), request.data(), request.contaId());
  }

  @Operation(
      summary = "Cancela um tÃ­tulo",
      description =
          """
          Desfaz a previsÃ£o. O tÃ­tulo sai da DRE prevista e nunca chegou a existir na DRE
          realizada.

          **SÃ³ Ã© permitido se nada foi realizado** (422): se jÃ¡ houve pagamento, o dinheiro
          efetivamente se moveu e nÃ£o se pode simplesmente apagar o compromisso â€” seria
          preciso estornar a movimentaÃ§Ã£o antes.

          TambÃ©m responde 422 se o tÃ­tulo jÃ¡ estiver cancelado ou quitado.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "OperaÃ§Ã£o concluÃ­da. O tÃ­tulo devolvido traz a situaÃ§Ã£o atualizada.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = TituloResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "RequisiÃ§Ã£o invÃ¡lida: campo reprovado, JSON malformado, enum ou data invÃ¡lidos.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Recurso nÃ£o encontrado (id inexistente).",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description = "Regra de negÃ³cio violada.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "Erro interno inesperado, sem expor detalhe interno.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class)))
  })
  @PostMapping("/{id}/cancelar")
  public TituloResponse cancelar(@PathVariable Long id) {
    return tituloService.cancelar(id);
  }

  @Operation(
      summary = "Remove um tÃ­tulo",
      description =
          """
          Exclui o tÃ­tulo **somente se ele nÃ£o tiver movimentaÃ§Ãµes vinculadas**.

          Como as movimentaÃ§Ãµes tÃªm chave estrangeira para o tÃ­tulo, um tÃ­tulo que jÃ¡ foi
          pago nÃ£o pode ser apagado. Para desfazer um compromisso que teve dinheiro
          envolvido, o caminho correto Ã© estornar as movimentaÃ§Ãµes, nÃ£o apagar o tÃ­tulo.

          Se o compromisso apenas deixou de existir, use `/cancelar`.

          **Erros:** 404 se o id nÃ£o existir; 422 se houver movimentaÃ§Ãµes vinculadas.
          """)
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> remover(@PathVariable Long id) {
    tituloService.remover(id);
    return ResponseEntity.noContent().build();
  }
}
