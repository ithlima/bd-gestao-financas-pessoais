package com.example.financas.config;

import com.example.financas.dto.response.ErroResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Respostas de erro reutilizáveis para a documentação OpenAPI.
 *
 * <h2>Por que esta classe existe</h2>
 *
 * <p>O springdoc gera o schema de um DTO apenas quando alguma operação o
 * <b>referencia</b>. Como {@code ErroResponse} só aparecia no
 * {@code GlobalExceptionHandler} — e não em nenhum {@code @RestController} —, ele
 * ficava de fora do contrato. O resultado era um Swagger que documentava os
 * caminhos de sucesso e escondia completamente o formato de erro, justamente a
 * parte que o consumidor da API mais precisa consultar.
 *
 * <p>Declarar as respostas de erro nas operações resolve isso e, de quebra, deixa
 * o contrato honesto: cada endpoint mostra os status que pode devolver.
 *
 * <h2>Como usar</h2>
 *
 * <pre>{@code
 * @ApiResponses({
 *   @ApiResponse(responseCode = "200", description = "Lista devolvida"),
 *   RespostasDeErro.NAO_ENCONTRADO
 * })
 * }</pre>
 *
 * <p>As constantes são anotações prontas, para não repetir a mesma descrição e o
 * mesmo {@code content} em cada método.
 */
public final class RespostasDeErro {

  private RespostasDeErro() {
    // classe utilitária: não deve ser instanciada
  }

  /** Corpo padrão de erro, para referência nas respostas. */
  private static final String DESCRICAO_CORPO =
      "Corpo no formato único de erro: status, erro, mensagem, caminho e detalhes.";

  /** 400 — dado enviado pelo cliente é inválido (formato, tipo ou validação). */
  @ApiResponse(
      responseCode = "400",
      description =
          """
          Requisição inválida: JSON malformado, enum ou data inválidos, campo reprovado pela
          validação, parâmetro ausente ou com tipo incorreto.
          """,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErroResponse.class)))
  public @interface RequisicaoInvalida {}

  /** 404 — o recurso ou a rota não existe. */
  @ApiResponse(
      responseCode = "404",
      description = "Recurso não encontrado (id inexistente) ou rota inexistente.",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErroResponse.class)))
  public @interface NaoEncontrado {}

  /** 405 — o verbo HTTP não é aceito pela rota. */
  @ApiResponse(
      responseCode = "405",
      description = "Método HTTP não aceito nesta rota.",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErroResponse.class)))
  public @interface MetodoNaoPermitido {}

  /** 409 — conflito com o estado atual dos dados. */
  @ApiResponse(
      responseCode = "409",
      description =
          """
          Conflito: registro duplicado (e-mail ou nome repetido) ou exclusão bloqueada por
          registros dependentes.
          """,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErroResponse.class)))
  public @interface Conflito {}

  /** 422 — regra de negócio do domínio violada. */
  @ApiResponse(
      responseCode = "422",
      description = "Regra de negócio violada (ex.: pagar mais que o valor em aberto do título).",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErroResponse.class)))
  public @interface RegraDeNegocio {}

  /** 500 — erro inesperado. A causa é registrada no log, não devolvida ao cliente. */
  @ApiResponse(
      responseCode = "500",
      description = "Erro interno inesperado. A mensagem devolvida não expõe detalhe interno.",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErroResponse.class)))
  public @interface ErroInterno {}
}
