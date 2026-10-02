package com.example.financas.exception;

/**
 * Lançada quando um recurso solicitado não existe. Traduzida em HTTP
 * <b>404 Not Found</b>.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

  public RecursoNaoEncontradoException(String mensagem) {
    super(mensagem);
  }

  /**
   * Atalho para o caso mais comum: buscar por id e não encontrar.
   *
   * <p>O gênero é passado explicitamente porque a língua exige concordância
   * ("Usuário não encontrado" × "Categoria não encontrada"), e um "não
   * encontrado(a)" genérico é pior de ler do que assumir o gênero.
   *
   * @param recurso nome legível do recurso no masculino, ex.: "Usuário"
   * @param id identificador procurado
   * @param feminino {@code true} para recursos femininos ("Categoria", "Conta",
   *     "Movimentação")
   */
  public static RecursoNaoEncontradoException porId(String recurso, Long id, boolean feminino) {
    String sufixo = feminino ? " não encontrada para o id " : " não encontrado para o id ";
    return new RecursoNaoEncontradoException(recurso + sufixo + id);
  }
}
