package com.example.financas.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CATEGORIA — como o usuário classifica suas receitas e despesas.
 *
 * <p>É a categoria que define as <b>linhas</b> da DRE: Salários, Freelances,
 * Rendimentos, Moradia, Alimentação, Transporte, Educação, Saúde, Lazer, entre
 * outras. O agrupamento do relatório é dinâmico — vem dos dados do usuário, não
 * de uma lista fixa no código.
 *
 * <p><b>Alteração desta evolução (correção de defeito).</b> O campo
 * {@code nome} era {@code VARCHAR(10)}. Com esse limite, categorias
 * absolutamente normais não podiam ser cadastradas:
 *
 * <pre>
 *   "Alimentação"      -> 11 caracteres  (não cabia)
 *   "Rendimentos"      -> 11 caracteres  (não cabia)
 *   "Outras receitas"  -> 15 caracteres  (não cabia)
 * </pre>
 *
 * Agora são 100 caracteres. A restrição {@code UNIQUE(nome, usuario_id)} foi
 * mantida — e passa a funcionar melhor, porque o usuário pode ter as categorias
 * que realmente usa sem que elas colidam.
 */
@Entity
@Table(
    name = "categoria",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_categoria_nome_usuario",
          columnNames = {"nome", "usuario_id"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idCategoria;

  @NotBlank(message = "O nome da categoria é obrigatório")
  @Size(max = 100, message = "O nome da categoria deve ter no máximo 100 caracteres")
  @Column(nullable = false, length = 100)
  private String nome;

  /**
   * RECEITA ou DESPESA.
   *
   * <p>É validado no service (RN03 e RN15) que o tipo da categoria seja igual
   * ao tipo do título ou da movimentação. Sem essa checagem, uma despesa poderia
   * ser classificada em "Salários" e a DRE somaria lixo.
   */
  @NotNull(message = "O tipo da categoria é obrigatório")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TipoMovimentacao tipo;

  @NotNull(message = "O usuário é obrigatório")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "usuario_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_categoria_usuario"))
  private Usuario usuario;
}
