package com.example.financas.exception;

/**
 * Lançada ao tentar criar um registro que viola uma restrição de unicidade —
 * e-mail de usuário, nome de conta ou nome de categoria repetido para o mesmo
 * usuário.
 *
 * <p>Traduzida em HTTP <b>409 Conflict</b>. É separada de
 * {@link RegraNegocioException} porque representa um conflito com o estado
 * atual dos dados, e não uma regra de domínio malformada.
 */
public class RecursoDuplicadoException extends RuntimeException {

  public RecursoDuplicadoException(String mensagem) {
    super(mensagem);
  }
}
