package com.example.financas.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(
    name = "ErroResponse",
    description =
        """
        Formato único de toda resposta de erro da API.

        O campo `detalhes` traz informação adicional quando existe — por exemplo, a lista
        de campos reprovados pela validação, ou qual parâmetro recebeu valor inválido.
        Quando não há detalhe, vem como lista vazia.

        **Nunca** contém SQL executado, nome de tabela ou constraint, caminho interno ou
        nome de classe Java: esses dados ficam no log do servidor.
        """)
public record ErroResponse(
    @Schema(
            description = "Momento em que o erro foi produzido.",
            example = "2026-10-01T19:59:58")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
    @Schema(description = "Código HTTP, repetido no corpo para conveniência do cliente.",
            example = "400")
        int status,
    @Schema(description = "Tipo curto do erro.", example = "Parâmetro inválido") String erro,
    @Schema(
            description = "Explicação do que aconteceu, em linguagem clara.",
            example = "O parâmetro 'tipo' recebeu um valor inválido. Valores aceitos: RECEITA, DESPESA")
        String mensagem,
    @Schema(description = "Rota que originou o erro.", example = "/api/v1/titulos")
        String caminho,
    @Schema(
            description = "Detalhes adicionais, quando existem. Lista vazia quando não há.",
            example = "[\"valorPrevisto: O valor previsto deve ser maior que zero\"]")
        List<String> detalhes) {

  public static ErroResponse of(int status, String erro, String mensagem, String caminho) {
    return new ErroResponse(LocalDateTime.now(), status, erro, mensagem, caminho, List.of());
  }

  public static ErroResponse of(
      int status, String erro, String mensagem, String caminho, List<String> detalhes) {
    return new ErroResponse(LocalDateTime.now(), status, erro, mensagem, caminho, detalhes);
  }
}
