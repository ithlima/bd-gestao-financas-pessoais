package com.example.financas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "conta", uniqueConstraints = {
    @UniqueConstraint(name = "uq_conta_nome_usuario", columnNames = { "nome", "usuario_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Conta {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idConta;

  @Column(nullable = false, length = 100)
  private String nome;

  @Column(nullable = false, length = 50)
  private String tipo;  

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal saldoInicial = BigDecimal.ZERO;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_conta_usuario"))
  private Usuario usuario;
}