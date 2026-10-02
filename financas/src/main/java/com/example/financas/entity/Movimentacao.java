package com.example.financas.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MOVIMENTAÇÃO — o evento financeiro realizado.
 *
 * <p><b>Conceito.</b> A movimentação responde à pergunta
 * <i>"o que aconteceu com o meu dinheiro?"</i>. É um <b>fato consumado</b>:
 * dinheiro que efetivamente entrou (RECEITA) ou saiu (DESPESA) de uma conta.
 *
 * <p>Ela é a <b>única</b> fonte de receita e despesa da DRE realizada. Um título
 * pendente, por mais vencido que esteja, nunca entra na DRE realizada — só a
 * movimentação entra. Ver {@link Titulo} para a distinção completa.
 *
 * <p><b>O que mudou nesta evolução.</b> Antes, a movimentação guardava um valor
 * e uma data <i>e ainda</i> era o lado "1" de uma relação com as tabelas
 * {@code recebimento} e {@code pagamento}, que tinham <i>outro</i> valor e
 * <i>outra</i> data. Havia, portanto, duas datas e dois valores concorrentes
 * para o mesmo dinheiro, sem nada no modelo dizendo qual era a previsão e qual
 * era a realização. Agora os campos são inequívocos:
 *
 * <ul>
 *   <li>{@code valor} — o valor <b>realizado</b> (o que de fato se moveu);</li>
 *   <li>{@code data} — a data do <b>evento</b> (quando de fato aconteceu);</li>
 *   <li>{@code conta} — <b>obrigatória</b>: todo evento financeiro acontece em
 *       alguma conta. Foi isso que permitiu absorver as tabelas
 *       recebimento/pagamento e transformar o cálculo de saldo em uma consulta
 *       única, sem {@code UNION}.</li>
 *   <li>{@code titulo} — <b>opcional</b>: liga a realização à previsão que a
 *       originou. Fica {@code null} em lançamentos avulsos (ex.: "paguei um café
 *       em dinheiro"), que nunca tiveram um título.</li>
 * </ul>
 */
@Entity
@Table(
    name = "movimentacao",
    indexes = {
      @Index(name = "idx_movimentacao_usuario_data", columnList = "usuario_id, data"),
      @Index(name = "idx_movimentacao_conta", columnList = "conta_id"),
      @Index(name = "idx_movimentacao_titulo", columnList = "titulo_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Movimentacao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idMovimentacao;

  @NotBlank(message = "A descrição da movimentação é obrigatória")
  @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
  @Column(nullable = false, length = 255)
  private String descricao;

  /**
   * Valor efetivamente movimentado.
   *
   * <p>Regra RN13: deve ser maior que zero. O sinal (entrada/saída) vem do
   * campo {@link #tipo}, nunca de um valor negativo — valor negativo é rejeitado.
   */
  @NotNull(message = "O valor é obrigatório")
  @Positive(message = "O valor da movimentação deve ser maior que zero")
  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal valor;

  @NotNull(message = "O tipo da movimentação é obrigatório")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TipoMovimentacao tipo;

  /** Data em que o dinheiro efetivamente se moveu. */
  @NotNull(message = "A data da movimentação é obrigatória")
  @Column(nullable = false)
  private LocalDate data;

  @NotNull(message = "O usuário é obrigatório")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "usuario_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_movimentacao_usuario"))
  private Usuario usuario;

  @NotNull(message = "A categoria é obrigatória")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "categoria_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_movimentacao_categoria"))
  private Categoria categoria;

  /**
   * Conta onde o dinheiro entrou ou saiu — obrigatória (RN14).
   *
   * <p>Absorveu o papel que antes era das tabelas {@code recebimento} e
   * {@code pagamento}.
   */
  @NotNull(message = "A conta é obrigatória")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "conta_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_movimentacao_conta"))
  private Conta conta;

  /**
   * Título que originou esta movimentação — opcional (RN18).
   *
   * <p>{@code null} significa lançamento avulso, sem previsão anterior.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "titulo_id", foreignKey = @ForeignKey(name = "fk_movimentacao_titulo"))
  private Titulo titulo;
}
