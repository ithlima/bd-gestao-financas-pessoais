package com.example.financas.exception;

/**
 * Exceção base para violações de <b>regra de negócio</b>.
 *
 * <p>É lançada quando o pedido é sintaticamente válido (passou pelo Bean
 * Validation), mas viola uma regra do domínio financeiro — por exemplo, pagar
 * mais do que o valor em aberto de um título, ou classificar uma despesa em uma
 * categoria de receita.
 *
 * <p>O {@link GlobalExceptionHandler} a traduz em HTTP <b>422 Unprocessable
 * Entity</b>: a requisição foi entendida, mas não pode ser processada como está.
 */
public class RegraNegocioException extends RuntimeException {

  public RegraNegocioException(String mensagem) {
    super(mensagem);
  }
}
