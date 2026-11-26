export type TipoMovimentacao = "RECEITA" | "DESPESA";

export type SituacaoTitulo = "PENDENTE" | "PAGO" | "CANCELADO";

export type SituacaoTituloEfetiva = "PENDENTE" | "PAGO" | "VENCIDO" | "CANCELADO";

export type SituacaoEmAberto = "A_VENCER" | "VENCIDO" | "PARCIAL" | "PARCIAL_VENCIDO";

export type ModoDre = "previsto" | "realizado" | "comparativo";

export interface ErroResponse {
  timestamp: string;
  status: number;
  erro: string;
  mensagem: string;
  caminho: string;
  detalhes: string[];
}

export interface LoginResponse {
  token: string;
}

export interface Usuario {
  idUsuario: number;
  nome: string;
  email: string;
  dataCadastro: string;
  ativo: boolean;
}

export interface UsuarioRequest {
  nome: string;
  email: string;
  senha: string;
  ativo?: boolean;
}

export interface Conta {
  idConta: number;
  nome: string;
  tipo: string;
  saldoInicial: number;
  saldoAtual: number;
  usuarioId: number;
}

export interface ContaRequest {
  nome: string;
  tipo: string;
  saldoInicial?: number;
  usuarioId: number;
}

export interface Categoria {
  idCategoria: number;
  nome: string;
  tipo: TipoMovimentacao;
  usuarioId: number;
}

export interface CategoriaRequest {
  nome: string;
  tipo: TipoMovimentacao;
  usuarioId: number;
}

export interface Titulo {
  idTitulo: number;
  descricao: string;
  valorPrevisto: number;
  valorRealizado: number;
  valorEmAberto: number;
  dataVencimento: string;
  dataPagamento: string | null;
  tipo: TipoMovimentacao;
  situacao: SituacaoTitulo;
  situacaoEfetiva: SituacaoTituloEfetiva;
  podeRegistrarPagamento: boolean;
  quantidadeMovimentacoes: number;
  observacao: string | null;
  categoriaId: number;
  categoriaNome: string;
  usuarioId: number;
}

export interface TituloRequest {
  descricao: string;
  valorPrevisto: number;
  dataVencimento: string;
  tipo: TipoMovimentacao;
  observacao?: string;
  categoriaId: number;
  usuarioId: number;
}

export interface LiquidacaoRequest {
  valor: number;
  data: string;
  contaId: number;
  usuarioId: number;
}

export interface Movimentacao {
  idMovimentacao: number;
  descricao: string;
  valor: number;
  tipo: TipoMovimentacao;
  data: string;
  contaId: number;
  contaNome: string;
  categoriaId: number | null;
  categoriaNome: string | null;
  tituloId: number | null;
  usuarioId: number;
}

export interface MovimentacaoRequest {
  descricao: string;
  valor: number;
  tipo: TipoMovimentacao;
  data: string;
  categoriaId: number;
  contaId: number;
  usuarioId: number;
}

export interface TransferenciaRequest {
  contaOrigemId: number;
  contaDestinoId: number;
  valor: number;
  data: string;
  descricao?: string;
}

export interface TransferenciaResponse {
  idMovimentacaoOrigem: number;
  idMovimentacaoDestino: number;
  contaOrigemId: number;
  contaDestinoId: number;
  valor: number;
  data: string;
  descricao: string;
}

export interface TituloEmAberto {
  idTitulo: number;
  descricao: string;
  tipo: TipoMovimentacao;
  valorPrevisto: number;
  valorRealizado: number;
  valorEmAberto: number;
  dataVencimento: string;
  diasParaVencimento: number;
  situacao: SituacaoEmAberto;
  categoriaNome: string;
  observacao: string | null;
}

export interface PosicaoTitulos {
  usuarioId: number;
  tipo: TipoMovimentacao;
  dataReferencia: string;
  titulos: TituloEmAberto[];
  totalEmAberto: number;
  totalVencido: number;
  totalAVencerProximosDias: number;
  quantidadeTitulos: number;
  quantidadeVencidos: number;
  quantidadeParciais: number;
  quantidadeAVencerProximosDias: number;
  vencidos: TituloEmAberto[];
  aVencerProximosDias: TituloEmAberto[];
  janelaDias: number;
}

export interface ResumoFinanceiro {
  usuarioId: number;
  dataReferencia: string;
  contasAPagar: PosicaoTitulos;
  contasAReceber: PosicaoTitulos;
  saldoPrevisto: number;
  saldoPrevistoPositivo: boolean;
  totalAPagar: number;
  totalAReceber: number;
}

export interface DreLinha {
  categoriaId: number;
  categoriaNome: string;
  tipo: TipoMovimentacao;
  valorPrevisto: number | null;
  valorRealizado: number | null;
  variacao: number | null;
}

export interface DreTituloVencido {
  idTitulo: number;
  descricao: string;
  valorPrevisto: number;
  dataVencimento: string;
  diasDeAtraso: number;
  tipo: TipoMovimentacao;
  categoriaNome: string;
  situacaoEfetiva: SituacaoTituloEfetiva;
}

export interface Dre {
  usuarioId: number;
  modo: ModoDre;
  inicio: string;
  fim: string;
  linhasReceitas: DreLinha[];
  totalReceitasPrevistas: number | null;
  totalReceitasRealizadas: number | null;
  linhasDespesas: DreLinha[];
  totalDespesasPrevistas: number | null;
  totalDespesasRealizadas: number | null;
  resultadoPrevisto: number | null;
  resultadoRealizado: number | null;
  variacaoResultado: number | null;
  variacaoFavoravel: boolean;
  titulosVencidos: DreTituloVencido[];
  totalVencido: number;
}
