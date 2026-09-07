package com.example.financas.repository;

import com.example.financas.entity.Conta;
import com.example.financas.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContaRepository extends JpaRepository<Conta, Long> {

    List<Conta> findByUsuario(Usuario usuario);

    List<Conta> findByUsuarioIdUsuario(Long usuarioId);

    boolean existsByNomeAndUsuarioIdUsuario(String nome, Long usuarioId);
}