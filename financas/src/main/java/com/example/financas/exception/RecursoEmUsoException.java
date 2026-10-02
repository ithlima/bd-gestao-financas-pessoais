package com.example.financas.exception;

/**
 * Lançada quando uma operação é recusada porque o registro está em uso por
 * outros registros.
 *
 * <p>O caso típico é a exclusão: apagar um usuário que possui títulos, ou uma
 * categoria usada por movimentações. A operação é legítima em si, mas o estado
 * atual dos dados a impede.
 *
 * <p>É traduzida em HTTP <b>409 Conflict</b>.
 *
 * <h2>Por que esta exceção existe, se o banco já recusaria</h2>
 *
 * <p>O banco recusaria de qualquer forma, pela chave estrangeira — e é bom que
 * recuse, porque é a garantia final. Mas existe uma diferença importante entre
 * <b>antecipar</b> e <b>deixar o banco recusar</b>:
 *
 * <ul>
 *   <li>Antecipando, a mensagem é escrita com conhecimento do domínio:
 *       <i>"O categoria não pode ser excluído porque existem 3 títulos…"</i> —
 *       com a contagem;</li>
 *   <li>Deixando o banco recusar, a mensagem vem do driver e precisa ser
 *       traduzida a partir do nome de uma constraint:
 *       <i>"Não é possível excluir esta categoria porque existem títulos ou
 *       movimentações que a utilizam"</i> — sem a contagem.</li>
 * </ul>
 *
 * <p>As duas camadas existem: esta exceção para o caminho comum (o service
 * verifica antes) e o tratamento de {@code DataIntegrityViolationException} como
 * rede de segurança para o que escapar.
 */
public class RecursoEmUsoException extends RuntimeException {

  public RecursoEmUsoException(String mensagem) {
    super(mensagem);
  }

  /**
   * Monta a mensagem no formato mais útil para o usuário, com a quantidade de
   * registros que impedem a operação.
   *
   * <h2>A concordância é mais complicada do que parece</h2>
   *
   * <p>Escrever esta frase corretamente exige concordância em três pontos — e a
   * primeira versão errou, produzindo <i>"Não é possível excluir esta
   * usuário"</i>:
   *
   * <ol>
   *   <li><b>número do verbo</b> — "há 1 conta" × "há 3 contas" (o verbo "haver"
   *       no sentido de existir é impessoal e não varia);</li>
   *   <li><b>número do substantivo</b> — "1 título" × "3 títulos";</li>
   *   <li><b>número e gênero do adjetivo</b> — "1 conta vinculad<b>a</b>",
   *       "1 título vinculad<b>o</b>", "3 contas vinculad<b>as</b>".</li>
   * </ol>
   *
   * <p>Para não precisar acertar também o gênero do <i>recurso</i>, a mensagem
   * foi construída com <b>"Não dá para excluir"</b> em vez de "Não é possível
   * excluir este/esta". As duas construções que pareciam naturais traziam
   * gênero embutido e errariam em algum caso:
   *
   * <ul>
   *   <li><i>"Não é possível excluir <b>esta</b> usuário"</i> — o artigo
   *       "esta" não concorda com "usuário";</li>
   *   <li><i>"O categoria não pode ser excluíd<b>o</b>"</i> — o particípio não
   *       concorda com "categoria".</li>
   * </ul>
   *
   * <p>O único gênero que sobra para controlar é o do <b>dependente</b>, que vem
   * por parâmetro.
   *
   * @param recurso recurso em uso, sem artigo, ex.: "usuário", "categoria"
   * @param recursoPlural forma plural, ex.: "usuários", "categorias"
   * @param quantidade quantos registros dependem dele
   * @param singular dependente no singular, ex.: "título", "movimentação"
   * @param plural dependente no plural, ex.: "títulos", "movimentações"
   * @param dependenteFeminino {@code true} se o dependente é feminino
   *     ("conta", "movimentação", "categoria")
   */
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

    // O adjetivo varia com número e gênero do dependente.
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
