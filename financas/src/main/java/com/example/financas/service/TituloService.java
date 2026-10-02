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

/**
 * Regras de negócio do <b>título</b> — o compromisso financeiro previsto.
 *
 * <p><b>O ponto mais importante desta classe:</b> nenhum método aqui cria uma
 * despesa. Cadastrar um título <i>não</i> movimenta dinheiro e <i>não</i> altera a
 * DRE realizada. O título só passa a valer como receita ou despesa realizada
 * quando é quitado — e quem cria a movimentação correspondente é o
 * {@link MovimentacaoService#registrarQuitacaoDeTitulo(NovaMovimentacao)}.
 *
 * <p>Daí decorre a regra central do sistema:
 *
 * <pre>
 *   DRE PREVISTA   -> lê titulo (por data_vencimento)      [regime de competência]
 *   DRE REALIZADA  -> lê movimentacao (por data)           [regime de caixa]
 * </pre>
 *
 * <p>É esse desenho que impede uma conta ainda não paga de ser contada como
 * despesa efetivamente realizada.
 *
 * <h2>Sobre as dependências</h2>
 *
 * <p>Este service depende de {@link MovimentacaoService} e <b>não</b> o
 * contrário. O {@code MovimentacaoService} não tem nenhuma referência de volta,
 * o que evita um ciclo de beans — ciclo esse que o Spring recusa criar por
 * padrão, e com razão: dois services que se conhecem mutuamente geralmente
 * indicam que a fronteira entre eles está mal traçada.
 *
 * <p>Foi por isso que as consultas a movimentações saíram daqui: tudo que lê
 * {@code movimentacao} pertence ao {@code MovimentacaoService}, e o
 * {@code TituloService} apenas pede a informação pronta.
 */
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

  // ------------------------------------------------------------------
  // Consultas
  // ------------------------------------------------------------------

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

  /**
   * Títulos <b>pendentes já vencidos</b>.
   *
   * <p>O filtro é {@code situacao = PENDENTE} no banco, e a condição "vencido" é
   * derivada em memória. Um título cancelado não é atraso; um título quitado
   * também não.
   */
  @Transactional(readOnly = true)
  public List<TituloResponse> listarVencidos(Long usuarioId, LocalDate referencia) {
    return listarEntidadesVencidas(usuarioId, referencia).stream()
        .map(this::toResponse)
        .toList();
  }

  /** Títulos pendentes já vencidos, como entidades — usado pela DRE. */
  @Transactional(readOnly = true)
  public List<Titulo> listarEntidadesVencidas(Long usuarioId, LocalDate referencia) {
    return tituloRepository
        .findByUsuarioIdUsuarioAndSituacaoAndDataVencimentoBeforeOrderByDataVencimentoAsc(
            usuarioId, SituacaoTitulo.PENDENTE, referencia);
  }

  /** Agregação da DRE prevista (soma por categoria, por vencimento). */
  @Transactional(readOnly = true)
  public List<DrePrevisaoProjection> somarPrevistoPorCategoria(
      Long usuarioId, LocalDate inicio, LocalDate fim) {
    return tituloRepository.somarPrevistoPorCategoria(usuarioId, inicio, fim);
  }

  // ------------------------------------------------------------------
  // CRUD
  // ------------------------------------------------------------------

  /**
   * Cadastra um título (RN01 a RN04).
   *
   * <p>O título nasce {@code PENDENTE}. Nenhuma movimentação é criada e nenhuma
   * despesa aparece na DRE realizada — o compromisso apenas passa a existir como
   * previsão.
   */
  @Transactional
  public TituloResponse criar(TituloRequest request) {
    Usuario usuario = usuarioService.buscarEntidade(request.usuarioId());

    // RN03: categoria do mesmo usuário e do mesmo tipo do título.
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

    // O tipo não pode mudar: ele define a direção do dinheiro e já está impresso
    // na categoria do título. Para mudar a direção, cadastre outro título.
    if (titulo.getTipo() != request.tipo()) {
      throw new RegraNegocioException(
          "O tipo de um título existente não pode ser alterado (era "
              + titulo.getTipo()
              + "). Cadastre um novo título, se necessário");
    }

    BigDecimal jaRealizado = movimentacaoService.totalRealizadoDoTitulo(id);

    // Não se pode reduzir o valor previsto abaixo do que já foi pago.
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

  /**
   * Cancela um título (RN05, RN07).
   *
   * <p>Só é permitido se <b>nada</b> foi realizado. Se já houve pagamento, o
   * dinheiro efetivamente se moveu e não se pode simplesmente apagar o
   * compromisso — seria preciso estornar a movimentação primeiro. Essa restrição
   * é o que garante que o histórico do caixa nunca fique inconsistente.
   */
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

  /**
   * Remove um título do banco.
   *
   * <p><b>Atenção:</b> como as movimentações têm chave estrangeira para o título,
   * um título que já foi pago não pode ser apagado — o banco recusaria a
   * operação. Para desfazer um compromisso que teve dinheiro envolvido, o caminho
   * correto é estornar as movimentações, não apagar o título. A verificação aqui
   * existe para devolver uma mensagem clara em vez de um erro de integridade.
   */
  @Transactional
  public void remover(Long id) {
    Titulo titulo = buscarEntidade(id);

    if (movimentacaoService.temMovimentacaoVinculada(id)) {
      throw new RegraNegocioException(
          "Este título possui movimentações vinculadas e não pode ser excluído. Cancele-o, se for o caso");
    }

    tituloRepository.delete(titulo);
  }

  // ------------------------------------------------------------------
  // Quitação — pagar / receber
  // ------------------------------------------------------------------

  /**
   * Registra o <b>pagamento</b> de um título de despesa.
   *
   * @param id título a pagar
   * @param valor valor pago nesta operação (pode ser parcial)
   * @param data data em que o pagamento aconteceu
   * @param contaId conta de onde o dinheiro saiu
   */
  @Transactional
  public TituloResponse pagar(Long id, BigDecimal valor, LocalDate data, Long contaId) {
    return quitar(id, valor, data, contaId, TipoMovimentacao.DESPESA);
  }

  /**
   * Registra o <b>recebimento</b> de um título de receita.
   *
   * <p>É a operação espelho de {@link #pagar}. Ambas chamam a mesma rotina
   * interna, porque a única diferença é a direção do dinheiro — que já está no
   * {@code tipo} do título. No modelo antigo, essa mesma lógica existia
   * duplicada em duas entidades ({@code Recebimento} e {@code Pagamento}).
   */
  @Transactional
  public TituloResponse receber(Long id, BigDecimal valor, LocalDate data, Long contaId) {
    return quitar(id, valor, data, contaId, TipoMovimentacao.RECEITA);
  }

  private TituloResponse quitar(
      Long id, BigDecimal valor, LocalDate data, Long contaId, TipoMovimentacao tipoEsperado) {

    Titulo titulo = buscarEntidade(id);

    if (titulo.getTipo() != tipoEsperado) {
      throw new RegraNegocioException(
          "Este título é do tipo "
              + titulo.getTipo()
              + ". Use "
              + (titulo.getTipo() == TipoMovimentacao.DESPESA ? "pagar" : "receber")
              + " para quitá-lo");
    }

    // A validação e a criação da movimentação acontecem no MovimentacaoService,
    // que é o dono das regras de movimentação. Aqui só se delega.
    movimentacaoService.registrarQuitacaoDeTitulo(
        new NovaMovimentacao(titulo, valor, data, contaService.validarContaDoUsuario(
            contaId, titulo.getUsuario().getIdUsuario())));

    return toResponse(titulo);
  }

  // ------------------------------------------------------------------
  // Regras compartilhadas com o MovimentacaoService
  // ------------------------------------------------------------------

  /**
   * Valida se o título pode receber uma quitação de {@code valor} (RN05, RN06,
   * RN09).
   *
   * <p>Público porque o {@link MovimentacaoService} reaplica essas regras no
   * momento de criar a movimentação. Concentrar a verificação aqui evita que ela
   * exista em dois lugares com textos diferentes.
   */
  public void validarQuitacao(Titulo titulo, BigDecimal valor) {
    if (titulo.getSituacao() == SituacaoTitulo.CANCELADO) {
      throw new RegraNegocioException("Um título cancelado não pode ser pago ou recebido");
    }
    if (titulo.getSituacao() == SituacaoTitulo.PAGO) {
      throw new RegraNegocioException("Este título já está quitado");
    }

    BigDecimal jaRealizado = movimentacaoService.totalRealizadoDoTitulo(titulo.getIdTitulo());
    BigDecimal emAberto = titulo.getValorPrevisto().subtract(jaRealizado);

    if (valor.compareTo(emAberto) > 0) {
      throw new RegraNegocioException(
          "O valor informado (R$ "
              + valor
              + ") é maior que o valor em aberto do título (R$ "
              + emAberto
              + ")");
    }
  }

  /**
   * Recalcula a situação do título depois de uma quitação (RN09, RN10).
   *
   * <p>Se a soma das movimentações atingiu o valor previsto, o título vira
   * {@code PAGO} e recebe a data de quitação. Se ainda falta, permanece
   * {@code PENDENTE} — é o caso do <b>pagamento parcial</b>, em que o título
   * continua em aberto exibindo o quanto falta.
   */
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

  // ------------------------------------------------------------------
  // Apoio
  // ------------------------------------------------------------------

  public Titulo buscarEntidade(Long id) {
    return tituloRepository
        .findById(id)
        .orElseThrow(() -> RecursoNaoEncontradoException.porId("Título", id, false));
  }

  /** Soma dos valores já realizados de um título. */
  @Transactional(readOnly = true)
  public BigDecimal totalRealizadoDoTitulo(Long tituloId) {
    return movimentacaoService.totalRealizadoDoTitulo(tituloId);
  }

  /** Monta o DTO do título com os valores calculados. */
  private TituloResponse toResponse(Titulo titulo) {
    return TituloMapper.toResponse(
        titulo,
        movimentacaoService.totalRealizadoDoTitulo(titulo.getIdTitulo()),
        movimentacaoService.contarMovimentacoesDoTitulo(titulo.getIdTitulo()));
  }
}
