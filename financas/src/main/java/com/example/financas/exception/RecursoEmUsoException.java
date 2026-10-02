package com.example.financas.exception;

public class RecursoEmUsoException extends RuntimeException {

  public RecursoEmUsoException(String mensagem) {
    super(mensagem);
  }

  public static RecursoEmUsoException porDependencia(
      String recurso,
      String recursoPlural,
      long quantidade,
      String singular,
      String plural,
      boolean dependenteFeminino) {

    boolean umSo = quantidade == 1;

    String nomeDoRecurso = umSo ? recurso : recursoPlural;
    String nomeDoDependente = umSo ? singular : plural;

    String vinculado;
    if (dependenteFeminino) {
      vinculado = umSo ? "vinculada" : "vinculadas";
    } else {
      vinculado = umSo ? "vinculado" : "vinculados";
    }

    return new RecursoEmUsoException(
        "Não dá para excluir "
            + nomeDoRecurso
            + " porque há "
            + quantidade
            + " "
            + nomeDoDependente
            + " "
            + vinculado
            + " a ele");
  }
}
