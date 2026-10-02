package com.example.financas.controller;

import com.example.financas.config.RespostasDeErro;
import com.example.financas.dto.request.ContaRequest;
import com.example.financas.dto.response.ContaResponse;
import com.example.financas.dto.response.MovimentacaoResponse;
import com.example.financas.service.ContaService;
import com.example.financas.service.MovimentacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de contas financeiras.
 *
 * <p>Além do CRUD, expõe o saldo atual de cada conta e o extrato (as
 * movimentações realizadas na conta). Os dois só são triviais de calcular porque
 * {@code movimentacao.conta_id} é obrigatório no novo modelo.
 */
@Tag(
    name = "Contas",
    description = "Onde o dinheiro está. O saldo é **calculado**, não armazenado.")
@RestController
@RequestMapping("/api/v1/contas")
public class ContaController {

  private final ContaService contaService;
  private final MovimentacaoService movimentacaoService;

  public ContaController(ContaService contaService, MovimentacaoService movimentacaoService) {
    this.contaService = contaService;
    this.movimentacaoService = movimentacaoService;
  }

  @Operation(
      summary = "Lista as contas com o saldo atual",
      description =
          """
          Lista as contas do usuário já com o **saldo atual** calculado:

          `saldoAtual = saldoInicial + receitas realizadas − despesas realizadas`

          O saldo não é uma coluna do banco: é derivado das movimentações, e por isso nunca
          fica desatualizado.
          """)
  @GetMapping
  public List<ContaResponse> listar(@RequestParam Long usuarioId) {
    return contaService.listar(usuarioId);
  }

  @Operation(
      summary = "Busca uma conta por id, com o saldo",
      description = "Responde 404 se o id não existir.")
  @GetMapping("/{id}")
  public ContaResponse buscarPorId(@PathVariable Long id) {
    return contaService.buscarPorId(id);
  }

  @Operation(
      summary = "Extrato da conta",
      description =
          """
          Lista as movimentações realizadas nesta conta, em ordem cronológica.

          É o equivalente ao extrato bancário: só aparecem eventos que **de fato
          aconteceram**. Títulos ainda pendentes não estão aqui.
          """)
  @GetMapping("/{id}/extrato")
  public List<MovimentacaoResponse> extrato(@PathVariable Long id) {
    return movimentacaoService.extratoDaConta(id);
  }

  @Operation(
      summary = "Cadastra uma conta",
      description =
          """
          Cria uma conta para o usuário. O nome deve ser único **por usuário**.

          `saldoInicial` é opcional: quando omitido, assume zero.

          **Erros:** 400 para campo inválido ou saldo negativo; 404 se o usuário não existir;
          409 se já houver uma conta com o mesmo nome para o usuário.
          """)
  @PostMapping
  public ResponseEntity<ContaResponse> criar(@Valid @RequestBody ContaRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(contaService.criar(request));
  }

  @Operation(
      summary = "Atualiza uma conta",
      description =
          """
          Altera nome, tipo e saldo inicial. O novo nome precisa continuar único para o
          usuário.

          **Erros:** 400, 404 ou 409 nas mesmas condições do cadastro.
          """)
  @PutMapping("/{id}")
  public ContaResponse atualizar(@PathVariable Long id, @Valid @RequestBody ContaRequest request) {
    return contaService.atualizar(id, request);
  }

  @Operation(
      summary = "Remove uma conta",
      description =
          """
          Exclui a conta **somente se não houver movimentações nela**.

          Apagar uma conta que já teve dinheiro movimentado destruiria o histórico do caixa.

          **Erros:** 404 se o id não existir; 409 se houver movimentações vinculadas, com a
          contagem na mensagem.
          """)
  @RespostasDeErro.Conflito
  @RespostasDeErro.NaoEncontrado
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> remover(@PathVariable Long id) {
    contaService.remover(id);
    return ResponseEntity.noContent().build();
  }
}
