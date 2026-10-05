package com.example.financas.service;

import com.example.financas.dto.request.TituloRequest;
import com.example.financas.dto.response.TituloResponse;
import com.example.financas.entity.Categoria;
import com.example.financas.entity.Conta;
import com.example.financas.entity.SituacaoTitulo;
import com.example.financas.entity.TipoMovimentacao;
import com.example.financas.entity.Titulo;
import com.example.financas.entity.Usuario;
import com.example.financas.exception.RecursoNaoEncontradoException;
import com.example.financas.exception.RegraNegocioException;
import com.example.financas.mapper.TituloMapper;
import com.example.financas.repository.TituloRepository;
import com.example.financas.repository.projection.DrePrevisaoProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TituloService {

  private final TituloRepository tituloRepository;
  private final UsuarioService usuarioService;
  private final CategoriaService categoriaService;
  private final ContaService contaService;
  private final MovimentacaoService movimentacaoService;

  public TituloService(
      TituloRepository tituloRepository,
      UsuarioService usuarioService,
      CategoriaService categoriaService,
      ContaService contaService,
      MovimentacaoService movimentacaoService) {
    this.tituloRepository = tituloRepository;
    this.usuarioService = usuarioService;
    this.categoriaService = categoriaService;
    this.contaService = contaService;
    this.movimentacaoService = movimentacaoService;
  }

  @Transactional(readOnly = true)
  public List<TituloResponse> listar(
      Long usuarioId, TipoMovimentacao tipo, SituacaoTitulo situacao) {

    List<Titulo> titulos = tituloRepository.findByUsuarioIdUsuario(usuarioId);

    if (tipo != null) {
      titulos = titulos.stream().filter(t -> t.getTipo() == tipo).toList();
    }
    if (situacao != null) {
      titulos = titulos.stream().filter(t -> t.getSituacao() == situacao).toList();
    }

    return titulos.stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public TituloResponse buscarPorId(Long id) {
    return toResponse(buscarEntidade(id));
  }

  @Transactional(readOnly = true)
  public List<TituloResponse> listarVencidos(Long usuarioId, LocalDate referencia) {
    return listarEntidadesVencidas(usuarioId, referencia).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<Titulo> listarEntidadesVencidas(Long usuarioId, LocalDate referencia) {
    return tituloRepository
        .findByUsuarioIdUsuarioAndSituacaoAndDataVencimentoBeforeOrderByDataVencimentoAsc(
            usuarioId, SituacaoTitulo.PENDENTE, referencia);
  }

  @Transactional(readOnly = true)
  public List<DrePrevisaoProjection> somarPrevistoPorCategoria(
      Long usuarioId, LocalDate inicio, LocalDate fim) {
    return tituloRepository.somarPrevistoPorCategoria(usuarioId, inicio, fim);
  }

  @Transactional
  public TituloResponse criar(TituloRequest request) {
    Usuario usuario = usuarioService.buscarEntidade(request.usuarioId());

    Categoria categoria =
        categoriaService.validarCategoriaDoLancamento(
            request.categoriaId(), request.usuarioId(), request.tipo(), "título");

    Titulo titulo = new Titulo();
    titulo.setDescricao(request.descricao());
    titulo.setValorPrevisto(request.valorPrevisto());
    titulo.setDataVencimento(request.dataVencimento());
    titulo.setTipo(request.tipo());
    titulo.setObservacao(request.observacao());
    titulo.setCategoria(categoria);
    titulo.setUsuario(usuario);
    titulo.setSituacao(SituacaoTitulo.PENDENTE);

    return toResponse(tituloRepository.save(titulo));
  }

  @Transactional
  public TituloResponse atualizar(Long id, TituloRequest request) {
    Titulo titulo = buscarEntidade(id);

    if (titulo.getSituacao() == SituacaoTitulo.CANCELADO) {
      throw new RegraNegocioException("Um título cancelado não pode ser alterado");
    }
    if (titulo.getSituacao() == SituacaoTitulo.PAGO) {
      throw new RegraNegocioException(
          "Um título já quitado não pode ser alterado. Registre um novo lançamento, se necessário");
    }

    if (titulo.getTipo() != request.tipo()) {
      throw new RegraNegocioException(
          "O tipo de um título existente não pode ser alterado (era "
              + titulo.getTipo()
              + "). Cadastre um novo título, se necessário");
    }

    BigDecimal jaRealizado = movimentacaoService.totalRealizadoDoTitulo(id);

    if (request.valorPrevisto().compareTo(jaRealizado) < 0) {
      throw new RegraNegocioException(
          "O novo valor previsto (R$ "
              + request.valorPrevisto()
              + ") é menor que o valor já realizado (R$ "
              + jaRealizado
              + ")");
    }

    Categoria categoria =
        categoriaService.validarCategoriaDoLancamento(
            request.categoriaId(), request.usuarioId(), request.tipo(), "título");

    titulo.setDescricao(request.descricao());
    titulo.setValorPrevisto(request.valorPrevisto());
    titulo.setDataVencimento(request.dataVencimento());
    titulo.setObservacao(request.observacao());
    titulo.setCategoria(categoria);

    return toResponse(tituloRepository.save(titulo));
  }

  @Transactional
  public TituloResponse cancelar(Long id) {
    Titulo titulo = buscarEntidade(id);

    if (titulo.getSituacao() == SituacaoTitulo.CANCELADO) {
      throw new RegraNegocioException("Este título já está cancelado");
    }
    if (titulo.getSituacao() == SituacaoTitulo.PAGO) {
      throw new RegraNegocioException("Um título já quitado não pode ser cancelado");
    }

    BigDecimal jaRealizado = movimentacaoService.totalRealizadoDoTitulo(id);
    if (jaRealizado.compareTo(BigDecimal.ZERO) > 0) {
      throw new RegraNegocioException(
          "Este título já possui R$ "
              + jaRealizado
              + " realizados e não pode ser cancelado. Estorne as movimentações primeiro");
    }

    titulo.setSituacao(SituacaoTitulo.CANCELADO);

    return toResponse(tituloRepository.save(titulo));
  }

  @Transactional
  public void remover(Long id) {
    Titulo titulo = buscarEntidade(id);

    if (movimentacaoService.temMovimentacaoVinculada(id)) {
      throw new RegraNegocioException(
          "Este título possui movimentações vinculadas e não pode ser excluído. Cancele-o, se for o caso");
    }

    tituloRepository.delete(titulo);
  }

  @Transactional
  public TituloResponse pagar(Long id, BigDecimal valor, LocalDate data, Long contaId, Long usuarioId) {
    return quitar(id, valor, data, contaId, usuarioId, TipoMovimentacao.DESPESA);
  }

  @Transactional
  public TituloResponse receber(Long id, BigDecimal valor, LocalDate data, Long contaId, Long usuarioId) {
    return quitar(id, valor, data, contaId, usuarioId, TipoMovimentacao.RECEITA);
  }

  private TituloResponse quitar(
      Long id, BigDecimal valor, LocalDate data, Long contaId, Long usuarioId, TipoMovimentacao tipoEsperado) {

    Titulo titulo = tituloRepository.findByIdWithLock(id)
        .orElseThrow(() -> RecursoNaoEncontradoException.porId("Título", id, false));

    if (!titulo.getUsuario().getIdUsuario().equals(usuarioId)) {
      throw new RegraNegocioException("Este título pertence a outro usuário");
    }

    if (titulo.getTipo() != tipoEsperado) {
      throw new RegraNegocioException(
          "Este título é do tipo "
              + titulo.getTipo()
              + ". Use "
              + (titulo.getTipo() == TipoMovimentacao.DESPESA ? "pagar" : "receber")
              + " para quitá-lo");
    }

    movimentacaoService.registrarQuitacaoDeTitulo(
        new NovaMovimentacao(titulo, valor, data, contaService.validarContaDoUsuario(
            contaId, titulo.getUsuario().getIdUsuario())));

    return toResponse(titulo);
  }

  public void validarQuitacao(Titulo titulo, BigDecimal valor) {
    if (titulo.getSituacao() == SituacaoTitulo.CANCELADO) {
      throw new RegraNegocioException("Um título cancelado não pode ser pago ou recebido");
    }
    if (titulo.getSituacao() == SituacaoTitulo.PAGO) {
      throw new RegraNegocioException("Este título já está quitado");
    }
  }

  @Transactional
  public void atualizarSituacaoAposQuitacao(Titulo titulo) {
    BigDecimal realizado = movimentacaoService.totalRealizadoDoTitulo(titulo.getIdTitulo());

    if (realizado.compareTo(titulo.getValorPrevisto()) >= 0) {
      LocalDate dataDaQuitacao =
          movimentacaoService.dataDoUltimoPagamento(titulo.getIdTitulo());

      titulo.quitar(dataDaQuitacao);
      tituloRepository.save(titulo);
    }
  }

  public Titulo buscarEntidade(Long id) {
    return tituloRepository
        .findById(id)
        .orElseThrow(() -> RecursoNaoEncontradoException.porId("Título", id, false));
  }

  @Transactional(readOnly = true)
  public BigDecimal totalRealizadoDoTitulo(Long tituloId) {
    return movimentacaoService.totalRealizadoDoTitulo(tituloId);
  }

  private TituloResponse toResponse(Titulo titulo) {
    return TituloMapper.toResponse(
        titulo,
        movimentacaoService.totalRealizadoDoTitulo(titulo.getIdTitulo()),
        movimentacaoService.contarMovimentacoesDoTitulo(titulo.getIdTitulo()));
  }
}
