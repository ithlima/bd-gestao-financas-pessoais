package com.example.financas.service;

import com.example.financas.dto.request.MovimentacaoRequest;
import com.example.financas.dto.response.MovimentacaoResponse;
import com.example.financas.entity.Categoria;
import com.example.financas.entity.Conta;
import com.example.financas.entity.Movimentacao;
import com.example.financas.entity.SituacaoTitulo;
import com.example.financas.dto.request.TransferenciaRequest;
import com.example.financas.dto.response.TransferenciaResponse;
import com.example.financas.entity.TipoMovimentacao;
import com.example.financas.entity.Titulo;
import com.example.financas.entity.Usuario;
import com.example.financas.exception.RecursoNaoEncontradoException;
import com.example.financas.exception.RegraNegocioException;
import com.example.financas.mapper.MovimentacaoMapper;
import com.example.financas.repository.MovimentacaoRepository;
import com.example.financas.repository.TituloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class MovimentacaoService {

  private final MovimentacaoRepository movimentacaoRepository;
  private final TituloRepository tituloRepository;
  private final UsuarioService usuarioService;
  private final ContaService contaService;
  private final CategoriaService categoriaService;

  public MovimentacaoService(
      MovimentacaoRepository movimentacaoRepository,
      TituloRepository tituloRepository,
      UsuarioService usuarioService,
      ContaService contaService,
      CategoriaService categoriaService) {
    this.movimentacaoRepository = movimentacaoRepository;
    this.tituloRepository = tituloRepository;
    this.usuarioService = usuarioService;
    this.contaService = contaService;
    this.categoriaService = categoriaService;
  }

  @Transactional(readOnly = true)
  public List<MovimentacaoResponse> listar(
      Long usuarioId, LocalDate inicio, LocalDate fim, TipoMovimentacao tipo) {

    List<Movimentacao> movimentacoes;

    if (inicio != null && fim != null) {
      movimentacoes =
          movimentacaoRepository.findByUsuarioIdUsuarioAndDataBetweenOrderByDataAsc(
              usuarioId, inicio, fim);
    } else {
      movimentacoes = movimentacaoRepository.findByUsuarioIdUsuario(usuarioId);
    }

    if (tipo != null) {
      movimentacoes = movimentacoes.stream().filter(m -> m.getTipo() == tipo).toList();
    }

    return movimentacoes.stream().map(MovimentacaoMapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public MovimentacaoResponse buscarPorId(Long id) {
    return MovimentacaoMapper.toResponse(buscarEntidade(id));
  }

  @Transactional(readOnly = true)
  public List<MovimentacaoResponse> extratoDaConta(Long contaId) {
    contaService.buscarEntidade(contaId);
    return movimentacaoRepository.findByContaIdConta(contaId).stream()
        .map(MovimentacaoMapper::toResponse)
        .toList();
  }

  @Transactional
  public MovimentacaoResponse registrarAvulsa(MovimentacaoRequest request) {
    Usuario usuario = usuarioService.buscarEntidade(request.usuarioId());
    Conta conta = contaService.validarContaDoUsuario(request.contaId(), request.usuarioId());
    Categoria categoria =
        categoriaService.validarCategoriaDoLancamento(
            request.categoriaId(), request.usuarioId(), request.tipo(), "movimentação");

    Movimentacao movimentacao = new Movimentacao();
    movimentacao.setDescricao(request.descricao());
    movimentacao.setValor(request.valor());
    movimentacao.setTipo(request.tipo());
    movimentacao.setData(request.data());
    movimentacao.setUsuario(usuario);
    movimentacao.setCategoria(categoria);
    movimentacao.setConta(conta);

    movimentacao.setTitulo(null);

    return MovimentacaoMapper.toResponse(movimentacaoRepository.save(movimentacao));
  }

  @Transactional
  public Movimentacao registrarQuitacaoDeTitulo(NovaMovimentacao dados) {
    Titulo titulo = dados.titulo();

    validarQuitacao(titulo, dados.valor());

    Conta conta =
        contaService.validarContaDoUsuario(
            dados.conta().getIdConta(), titulo.getUsuario().getIdUsuario());

    Movimentacao movimentacao = new Movimentacao();
    movimentacao.setDescricao(titulo.getDescricao());

    movimentacao.setTipo(titulo.getTipo());
    movimentacao.setCategoria(titulo.getCategoria());
    movimentacao.setValor(dados.valor());
    movimentacao.setData(dados.data());
    movimentacao.setUsuario(titulo.getUsuario());
    movimentacao.setConta(conta);
    movimentacao.setTitulo(titulo);

    Movimentacao salva = movimentacaoRepository.save(movimentacao);

    quitarTituloSeTotalmenteRealizado(titulo);

    return salva;
  }

  public void validarQuitacao(Titulo titulo, BigDecimal valor) {
    if (titulo.getSituacao() == SituacaoTitulo.CANCELADO) {
      throw new RegraNegocioException("Um título cancelado não pode ser pago ou recebido");
    }
    if (titulo.getSituacao() == SituacaoTitulo.PAGO) {
      throw new RegraNegocioException("Este título já está quitado");
    }
  }

  private void quitarTituloSeTotalmenteRealizado(Titulo titulo) {
    BigDecimal realizado = totalRealizadoDoTitulo(titulo.getIdTitulo());

    if (realizado.compareTo(titulo.getValorPrevisto()) >= 0) {
      LocalDate dataDaQuitacao =
          movimentacaoRepository.findByTituloIdTitulo(titulo.getIdTitulo()).stream()
              .map(Movimentacao::getData)
              .max(LocalDate::compareTo)
              .orElse(LocalDate.now());

      tituloRepository.marcarComoPago(titulo.getIdTitulo(), dataDaQuitacao);

      titulo.quitar(dataDaQuitacao);
    }
  }

  private Movimentacao buscarEntidade(Long id) {
    return movimentacaoRepository
        .findById(id)
        .orElseThrow(() -> RecursoNaoEncontradoException.porId("Movimentação", id, true));
  }

  @Transactional(readOnly = true)
  public BigDecimal totalRealizadoDoTitulo(Long tituloId) {
    BigDecimal total = movimentacaoRepository.somarRealizadoDoTitulo(tituloId);
    return total != null ? total : BigDecimal.ZERO;
  }

  @Transactional(readOnly = true)
  public int contarMovimentacoesDoTitulo(Long tituloId) {
    return movimentacaoRepository.findByTituloIdTitulo(tituloId).size();
  }

  @Transactional(readOnly = true)
  public boolean temMovimentacaoVinculada(Long tituloId) {
    return !movimentacaoRepository.findByTituloIdTitulo(tituloId).isEmpty();
  }

  @Transactional(readOnly = true)
  public LocalDate dataDoUltimoPagamento(Long tituloId) {
    return movimentacaoRepository.findByTituloIdTitulo(tituloId).stream()
        .map(Movimentacao::getData)
        .max(LocalDate::compareTo)
        .orElse(LocalDate.now());
  }

  @Transactional(readOnly = true)
  public List<Movimentacao> listarPorTitulo(Long tituloId) {
    return movimentacaoRepository.findByTituloIdTitulo(tituloId);
  }

  @Transactional
  public void estornar(Long id, Long usuarioId) {
    Movimentacao movimentacao = buscarEntidade(id);
    if (!movimentacao.getUsuario().getIdUsuario().equals(usuarioId)) throw new RegraNegocioException("Acesso negado");
    Titulo titulo = movimentacao.getTitulo();
    
    movimentacaoRepository.delete(movimentacao);
    
    if (titulo != null) {
      BigDecimal realizadoRestante = totalRealizadoDoTitulo(titulo.getIdTitulo());
      if (realizadoRestante.compareTo(titulo.getValorPrevisto()) < 0) {
        titulo.setSituacao(SituacaoTitulo.PENDENTE);
        titulo.setDataPagamento(null);
        tituloRepository.save(titulo);
      }
    }
  }

  @Transactional
  public TransferenciaResponse transferir(TransferenciaRequest request, Long usuarioId) {
    if (request.contaOrigemId().equals(request.contaDestinoId())) {
      throw new RegraNegocioException("A conta de origem não pode ser a mesma de destino");
    }

    Conta contaOrigem = contaService.validarContaDoUsuario(request.contaOrigemId(), usuarioId);
    Conta contaDestino = contaService.validarContaDoUsuario(request.contaDestinoId(), usuarioId);
    Usuario usuario = contaOrigem.getUsuario();

    String descricao = request.descricao() != null && !request.descricao().trim().isEmpty() 
        ? request.descricao() 
        : "Transferência";

    Movimentacao saida = new Movimentacao();
    saida.setConta(contaOrigem);
    saida.setUsuario(usuario);
    saida.setTipo(TipoMovimentacao.DESPESA);
    saida.setValor(request.valor());
    saida.setData(request.data());
    saida.setDescricao(descricao + " para " + contaDestino.getNome());
    saida.setIsTransferencia(true);
    saida = movimentacaoRepository.save(saida);

    Movimentacao entrada = new Movimentacao();
    entrada.setConta(contaDestino);
    entrada.setUsuario(usuario);
    entrada.setTipo(TipoMovimentacao.RECEITA);
    entrada.setValor(request.valor());
    entrada.setData(request.data());
    entrada.setDescricao(descricao + " de " + contaOrigem.getNome());
    entrada.setIsTransferencia(true);
    entrada = movimentacaoRepository.save(entrada);

    return new TransferenciaResponse(
        saida.getIdMovimentacao(),
        entrada.getIdMovimentacao(),
        contaOrigem.getIdConta(),
        contaDestino.getIdConta(),
        request.valor(),
        request.data(),
        descricao
    );
  }
}