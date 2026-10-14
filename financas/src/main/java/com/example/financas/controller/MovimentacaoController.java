package com.example.financas.controller;

import com.example.financas.dto.request.MovimentacaoRequest;
import com.example.financas.dto.request.TransferenciaRequest;
import com.example.financas.dto.response.TransferenciaResponse;
import com.example.financas.dto.response.MovimentacaoResponse;
import com.example.financas.entity.TipoMovimentacao;
import com.example.financas.service.MovimentacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.financas.entity.Usuario;

import java.time.LocalDate;
import java.util.List;

@Tag(
    name = "Movimentações",
    description =
        """
        Os **eventos realizados** — o que de fato aconteceu com o dinheiro.

        Uma movimentação é um fato consumado: não há edição nem exclusão. Correções exigem
        estorno e novo lançamento.
        """)
@RestController
@RequestMapping("/api/v1/movimentacoes")
public class MovimentacaoController {

  private final MovimentacaoService movimentacaoService;

  public MovimentacaoController(MovimentacaoService movimentacaoService) {
    this.movimentacaoService = movimentacaoService;
  }

  @Operation(
      summary = "Lista as movimentações realizadas",
      description =
          """
          Lista os eventos financeiros já realizados, com filtros opcionais.

          - `inicio` e `fim` filtram pela **data do evento** (quando o dinheiro se moveu);
          - `tipo` filtra entre RECEITA e DESPESA.

          Para filtrar por período informando apenas uma das datas, informe as duas.
          """)
  @GetMapping
  public List<MovimentacaoResponse> listar(
      @RequestParam Long usuarioId,
      @RequestParam(required = false) LocalDate inicio,
      @RequestParam(required = false) LocalDate fim,
      @RequestParam(required = false) TipoMovimentacao tipo,
      @AuthenticationPrincipal Usuario usuario) {
    Long idFinal = com.example.financas.config.SecurityUtils.validarUsuario(usuarioId, usuario);
    return movimentacaoService.listar(idFinal, inicio, fim, tipo);
  }

  @Operation(
      summary = "Busca uma movimentação por id",
      description = "Responde 404 se o id não existir.")
  @GetMapping("/{id}")
  public MovimentacaoResponse buscarPorId(
      @PathVariable Long id,
      @AuthenticationPrincipal Usuario usuario) {
    return movimentacaoService.buscarPorId(id, usuario != null ? usuario.getIdUsuario() : null);
  }

  @Operation(
      summary = "Registra um lançamento avulso (sem título)",
      description =
          """
          Registra um evento financeiro que aconteceu **sem previsão anterior** — por
          exemplo, "paguei um café em dinheiro".

          É o caminho para o lançamento direto, que não passou por um título.

          ## Quando usar o caminho do título

          Se o gasto ou recebimento **já era esperado**, prefira cadastrar um título e
          quitá-lo:

          - `POST /api/v1/titulos` e depois `POST /api/v1/titulos/{id}/pagar`
          - `POST /api/v1/titulos` e depois `POST /api/v1/titulos/{id}/receber`

          Assim o vínculo entre previsão e realização fica registrado, e o relatório
          consegue comparar o que foi planejado com o que aconteceu.

          ## Validações

          A `conta` é obrigatória (todo evento acontece em alguma conta) e a `categoria`
          precisa ser do mesmo tipo do lançamento — uma despesa não pode ser classificada
          como "Salários".

          **Erros:** 400 para campo inválido; 404 se usuário, conta ou categoria não
          existirem; 422 se a categoria for de outro usuário ou de tipo incompatível.
          """)
  @PostMapping
  public ResponseEntity<MovimentacaoResponse> registrar(
      @Valid @RequestBody MovimentacaoRequest request,
      @AuthenticationPrincipal Usuario usuario) {
    Long idFinal = com.example.financas.config.SecurityUtils.validarUsuario(request.usuarioId(), usuario);
    MovimentacaoRequest requestSegura = new MovimentacaoRequest(
        request.descricao(), request.valor(), request.tipo(), request.data(),
        request.contaId(), request.categoriaId(), idFinal);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(movimentacaoService.registrarAvulsa(requestSegura));
  }

  @Operation(
      summary = "Transfere dinheiro entre duas contas do usuário"
  )
  @PostMapping("/transferir")
  public ResponseEntity<TransferenciaResponse> transferir(
      @Valid @RequestBody TransferenciaRequest request,
      @AuthenticationPrincipal Usuario usuario) {
    return ResponseEntity.ok(movimentacaoService.transferir(request, usuario.getIdUsuario()));
  }
}