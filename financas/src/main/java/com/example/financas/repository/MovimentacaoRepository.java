package com.example.financas.repository;

import com.example.financas.entity.Movimentacao;
import com.example.financas.entity.TipoMovimentacao;
import com.example.financas.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    List<Movimentacao> findByUsuario(Usuario usuario);

    List<Movimentacao> findByUsuarioIdUsuario(Long usuarioId);

    List<Movimentacao> findByUsuarioIdUsuarioAndTipo(
            Long usuarioId,
            TipoMovimentacao tipo);

    List<Movimentacao> findByUsuarioIdUsuarioAndDataBetween(
            Long usuarioId,
            LocalDate dataInicio,
            LocalDate dataFim);

    List<Movimentacao> findByUsuarioIdUsuarioAndCategoriaIdCategoria(
            Long usuarioId,
            Long categoriaId);
}