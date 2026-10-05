package com.example.financas.service;

import com.example.financas.dto.request.UsuarioRequest;
import com.example.financas.dto.response.UsuarioResponse;
import com.example.financas.entity.Usuario;
import com.example.financas.exception.RecursoDuplicadoException;
import com.example.financas.exception.RecursoEmUsoException;
import com.example.financas.exception.RecursoNaoEncontradoException;
import com.example.financas.mapper.UsuarioMapper;
import com.example.financas.repository.CategoriaRepository;
import com.example.financas.repository.ContaRepository;
import com.example.financas.repository.MovimentacaoRepository;
import com.example.financas.repository.TituloRepository;
import com.example.financas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

  private final UsuarioRepository usuarioRepository;
  private final TituloRepository tituloRepository;
  private final MovimentacaoRepository movimentacaoRepository;
  private final ContaRepository contaRepository;
  private final CategoriaRepository categoriaRepository;
  private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

  public UsuarioService(
      UsuarioRepository usuarioRepository,
      TituloRepository tituloRepository,
      MovimentacaoRepository movimentacaoRepository,
      ContaRepository contaRepository,
      CategoriaRepository categoriaRepository,
      org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
    this.usuarioRepository = usuarioRepository;
    this.tituloRepository = tituloRepository;
    this.movimentacaoRepository = movimentacaoRepository;
    this.contaRepository = contaRepository;
    this.categoriaRepository = categoriaRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public List<UsuarioResponse> listar() {
    return usuarioRepository.findAll().stream().map(UsuarioMapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public UsuarioResponse buscarPorId(Long id) {
    return UsuarioMapper.toResponse(buscarEntidade(id));
  }

  @Transactional
  public UsuarioResponse criar(UsuarioRequest request) {
    if (usuarioRepository.existsByEmail(request.email())) {
      throw new RecursoDuplicadoException("Já existe um usuário com o e-mail " + request.email());
    }

    Usuario usuario = new Usuario();
    usuario.setNome(request.nome());
    usuario.setEmail(request.email());
    usuario.setSenha(passwordEncoder.encode(request.senha()));
    usuario.setAtivo(request.ativoOuPadrao());

    return UsuarioMapper.toResponse(usuarioRepository.save(usuario));
  }

  @Transactional
  public UsuarioResponse atualizar(Long id, UsuarioRequest request) {
    Usuario usuario = buscarEntidade(id);

    if (!usuario.getEmail().equals(request.email())
        && usuarioRepository.existsByEmail(request.email())) {
      throw new RecursoDuplicadoException("Já existe um usuário com o e-mail " + request.email());
    }

    usuario.setNome(request.nome());
    usuario.setEmail(request.email());
    if (request.senha() != null && !request.senha().isBlank()) {
      usuario.setSenha(passwordEncoder.encode(request.senha()));
    }
    usuario.setAtivo(request.ativoOuPadrao());

    return UsuarioMapper.toResponse(usuarioRepository.save(usuario));
  }

  @Transactional
  public void remover(Long id) {
    Usuario usuario = buscarEntidade(id);

    long titulos = tituloRepository.countByUsuarioIdUsuario(id);
    if (titulos > 0) {
      throw RecursoEmUsoException.porDependencia("usuário", "usuários", titulos, "título", "títulos", false);
    }

    long movimentacoes = movimentacaoRepository.countByUsuarioIdUsuario(id);
    if (movimentacoes > 0) {
      throw RecursoEmUsoException.porDependencia("usuário", "usuários", movimentacoes, "movimentação", "movimentações", true);
    }

    long contas = contaRepository.findByUsuarioIdUsuario(id).size();
    if (contas > 0) {
      throw RecursoEmUsoException.porDependencia("usuário", "usuários", contas, "conta", "contas", true);
    }

    long categorias = categoriaRepository.findByUsuarioIdUsuario(id).size();
    if (categorias > 0) {
      throw RecursoEmUsoException.porDependencia("usuário", "usuários", categorias, "categoria", "categorias", true);
    }

    usuarioRepository.delete(usuario);
  }

  public Usuario buscarEntidade(Long id) {
    return usuarioRepository
        .findById(id)
        .orElseThrow(() -> RecursoNaoEncontradoException.porId("Usuário", id, false));
  }
}
