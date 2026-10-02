package com.example.financas.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
