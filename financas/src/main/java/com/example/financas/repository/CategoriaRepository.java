package com.example.financas.repository;

import com.example.financas.entity.Categoria;
import com.example.financas.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByUsuario(Usuario usuario);

    List<Categoria> findByUsuarioIdUsuario(Long usuarioId);

    boolean existsByNomeAndUsuarioIdUsuario(String nome, Long usuarioId);
}