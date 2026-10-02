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

@Tag(
    name = "Títulos",
    description =
        """
        Os **compromissos previstos** â€” o que se tem a pagar ou a receber.

        Cadastrar um título **não** cria despesa: nenhum valor sai ou entra, e a DRE
        realizada permanece inalterada. A despesa passa a existir quando o título é pago ou
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
      summary = "Lista os títulos (previstos)",
      description =
          """
          Lista os títulos do usuário, com filtros opcionais:

          - `tipo` â€” RECEITA (a receber) ou DESPESA (a pagar);
          - `situacao` â€” a situação **armazenada**: PENDENTE, PAGO ou CANCELADO.

          Para a situação **efetiva** (que inclui VENCIDO), consulte o campo
          `situacaoEfetiva` de cada item. VENCIDO não é armazenado: é derivado da
          comparação entre o vencimento e a data de hoje.
          """)
  @GetMapping
  public List<TituloResponse> listar(
      @RequestParam Long usuarioId,
      @RequestParam(required = false) TipoMovimentacao tipo,
      @RequestParam(required = false) SituacaoTitulo situacao) {
    return tituloService.listar(usuarioId, tipo, situacao);
  }

  @Operation(
      summary = "Busca um título por id",
      description = "Responde 404 se o id não existir.")
  @GetMapping("/{id}")
  public TituloResponse buscarPorId(@PathVariable Long id) {
    return tituloService.buscarPorId(id);
  }

  @Operation(
      summary = "Lista os títulos vencidos",
      description =
          """
          Lista os títulos **pendentes com vencimento já ultrapassado**.

          Um título cancelado não é atraso, e um já quitado também não â€” por isso só os
          pendentes aparecem.

          O parâmetro `referencia` permite simular outra data; quando omitido, usa hoje.
          """)
  @GetMapping("/vencidos")
  public List<TituloResponse> vencidos(
      @RequestParam Long usuarioId,
      @RequestParam(required = false) LocalDate referencia) {
    return tituloService.listarVencidos(
        usuarioId, referencia != null ? referencia : LocalDate.now());
  }

  @Operation(
      summary = "Cadastra um título (previsão)",
      description =
          """
          Registra um compromisso futuro. **Nada acontece no caixa** e a DRE realizada não
          muda: o título passa a existir apenas como previsão.

          A categoria é obrigatória e precisa ser do mesmo tipo do título (RN03) â€” uma
          receita não pode ser classificada em "Moradia", por exemplo.

          O título nasce com `situacao = PENDENTE` e `valorRealizado = 0`. A situação
          `situacaoEfetiva` já vem como VENCIDO se o vencimento informado for no passado.

          **Erros:** 400 para campo inválido; 404 se usuário ou categoria não existirem;
          422 se a categoria for de outro usuário ou de tipo incompatível.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Título criado, com situação PENDENTE e valor realizado zerado.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = TituloResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Requisição inválida: campo reprovado, JSON malformado, enum ou data inválidos.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Recurso não encontrado (id inexistente).",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description = "Regra de negócio violada.",
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
      summary = "Atualiza um título pendente",
      description =
          """
          Altera descrição, valor previsto, vencimento, categoria e observação.

          **Restrições** (422 se violadas):

          - o `tipo` **não pode ser alterado** â€” para mudar a direção, cadastre outro título;
          - um título **cancelado** não pode ser alterado;
          - um título **quitado** não pode ser alterado;
          - o novo `valorPrevisto` não pode ser **menor que o valor já realizado**.
          """)
  @PutMapping("/{id}")
  public TituloResponse atualizar(
      @PathVariable Long id, @Valid @RequestBody TituloRequest request) {
    return tituloService.atualizar(id, request);
  }

  @Operation(
      summary = "Paga um título de despesa",
      description =
          """
          **Esta é a operação que transforma previsão em realização.**

          Registra o pagamento, cria a **movimentação** correspondente (valor realizado,
          data do pagamento, conta utilizada) e, se a soma dos pagamentos atingir o valor
          previsto, marca o título como PAGO.

          ## Pagamento parcial é permitido

          O `valor` pode ser **menor** que o valor previsto. Nesse caso o título continua
          PENDENTE e o quanto falta aparece em `valorEmAberto`. Ã‰ assim que se registra um
          aluguel pago em duas vezes.

          "PAGO" significa **quitado**, não "teve algum pagamento".

          ## O que esta operação valida

          - o título precisa ser do tipo DESPESA â€” para receita, use `/receber` (422);
          - o título não pode estar cancelado nem já quitado (422);
          - o valor não pode ultrapassar o que está em aberto (422);
          - a conta precisa pertencer ao mesmo usuário do título (422).

          Tudo acontece em uma única transação: ou a movimentação é criada **e** o título
          atualizado, ou nada é gravado.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operação concluída. O título devolvido traz a situação atualizada.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = TituloResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Requisição inválida: campo reprovado, JSON malformado, enum ou data inválidos.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Recurso não encontrado (id inexistente).",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description = "Regra de negócio violada.",
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
      summary = "Recebe um título de receita",
      description =
          """
          Operação espelho de `/pagar`, para títulos do tipo RECEITA.

          Registra que o dinheiro **efetivamente entrou**, cria a movimentação de RECEITA e
          quita o título quando o total recebido atinge o valor previsto.

          Aceita recebimento parcial, pelas mesmas regras do pagamento: o título permanece
          PENDENTE enquanto faltar valor.

          **Erros:** 422 se o título for do tipo DESPESA (use `/pagar`), se estiver
          cancelado ou quitado, se o valor ultrapassar o em aberto, ou se a conta for de
          outro usuário.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operação concluída. O título devolvido traz a situação atualizada.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = TituloResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Requisição inválida: campo reprovado, JSON malformado, enum ou data inválidos.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Recurso não encontrado (id inexistente).",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description = "Regra de negócio violada.",
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
      summary = "Cancela um título",
      description =
          """
          Desfaz a previsão. O título sai da DRE prevista e nunca chegou a existir na DRE
          realizada.

          **Só é permitido se nada foi realizado** (422): se já houve pagamento, o dinheiro
          efetivamente se moveu e não se pode simplesmente apagar o compromisso â€” seria
          preciso estornar a movimentação antes.

          Também responde 422 se o título já estiver cancelado ou quitado.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operação concluída. O título devolvido traz a situação atualizada.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = TituloResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Requisição inválida: campo reprovado, JSON malformado, enum ou data inválidos.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Recurso não encontrado (id inexistente).",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ErroResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description = "Regra de negócio violada.",
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
      summary = "Remove um título",
      description =
          """
          Exclui o título **somente se ele não tiver movimentações vinculadas**.

          Como as movimentações têm chave estrangeira para o título, um título que já foi
          pago não pode ser apagado. Para desfazer um compromisso que teve dinheiro
          envolvido, o caminho correto é estornar as movimentações, não apagar o título.

          Se o compromisso apenas deixou de existir, use `/cancelar`.

          **Erros:** 404 se o id não existir; 422 se houver movimentações vinculadas.
          """)
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> remover(@PathVariable Long id) {
    tituloService.remover(id);
    return ResponseEntity.noContent().build();
  }
}
