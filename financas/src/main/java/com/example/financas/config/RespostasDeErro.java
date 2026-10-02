package com.example.financas.config;

import com.example.financas.dto.response.ErroResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public final class RespostasDeErro {

  private RespostasDeErro() {

  }

  private static final String DESCRICAO_CORPO =
      "Corpo no formato único de erro: status, erro, mensagem, caminho e detalhes.";

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

  @ApiResponse(
      responseCode = "404",
      description = "Recurso não encontrado (id inexistente) ou rota inexistente.",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErroResponse.class)))
  public @interface NaoEncontrado {}

  @ApiResponse(
      responseCode = "405",
      description = "Método HTTP não aceito nesta rota.",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErroResponse.class)))
  public @interface MetodoNaoPermitido {}

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

  @ApiResponse(
      responseCode = "422",
      description = "Regra de negócio violada (ex.: pagar mais que o valor em aberto do título).",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErroResponse.class)))
  public @interface RegraDeNegocio {}

  @ApiResponse(
      responseCode = "500",
      description = "Erro interno inesperado. A mensagem devolvida não expõe detalhe interno.",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErroResponse.class)))
  public @interface ErroInterno {}
}
