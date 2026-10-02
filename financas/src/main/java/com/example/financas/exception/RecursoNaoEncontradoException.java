package com.example.financas.exception;

public class RecursoNaoEncontradoException extends RuntimeException {

  public RecursoNaoEncontradoException(String mensagem) {
    super(mensagem);
  }

  public static RecursoNaoEncontradoException porId(String recurso, Long id, boolean feminino) {
    String sufixo = feminino ? " não encontrada para o id " : " não encontrado para o id ";
    return new RecursoNaoEncontradoException(recurso + sufixo + id);
  }
}
