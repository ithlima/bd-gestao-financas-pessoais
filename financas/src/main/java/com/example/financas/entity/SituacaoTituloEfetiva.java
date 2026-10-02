package com.example.financas.entity;

/**
 * Situação EFETIVA de um título — a visão que a API entrega ao usuário.
 *
 * <p>É a união da situação armazenada ({@link SituacaoTitulo}) com o estado
 * derivado {@link #VENCIDO}. Enquanto o banco guarda apenas PENDENTE, PAGO e
 * CANCELADO, a resposta da API mostra os quatro valores que o usuário espera.
 */
public enum SituacaoTituloEfetiva {
  PENDENTE,
  PAGO,
  VENCIDO,
  CANCELADO
}
