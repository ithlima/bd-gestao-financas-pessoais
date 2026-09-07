package com.example.financas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idUsuario;

  @Column(nullable = false, length = 100)
  private String nome;

  @Column(nullable = false, unique = true, length = 150)
  private String email;

  @Column(nullable = false, length = 255)
  private String senha;

  @Column(nullable = false)
  private LocalDateTime dataCadastro;

  @Column(nullable = false)
  private Boolean ativo = true;

  @PrePersist
  protected void prePersist() {
    if (dataCadastro == null) {
      dataCadastro = LocalDateTime.now();
    }
  }
}