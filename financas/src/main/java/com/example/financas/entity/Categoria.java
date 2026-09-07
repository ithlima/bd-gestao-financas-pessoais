package com.example.financas.entity;

import jakarta.persistence.*;
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
      columnNames = {"nome", "usuario_id"}
    )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long idCategoria;

  @Column(nullable = false, length = 10)
  private String nome;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TipoMovimentacao tipo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
    name = "usuario_id",
    nullable = false,
    foreignKey = @ForeignKey(name = "fk_categoria_usuario")
  )
  private Usuario usuario;
}
