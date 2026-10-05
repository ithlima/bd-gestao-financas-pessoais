import { apiClient } from '@/shared/api-client/apiClient';

export interface ITitulo {
  idTitulo: number;
  descricao: string;
  valorPrevisto: number;
  valorRealizado: number;
  valorEmAberto: number;
  dataVencimento: string;
  dataPagamento: string | null;
  tipo: 'DESPESA' | 'RECEITA';
  situacao: string;
  situacaoEfetiva: string;
  observacao: string;
  categoria: {
    idCategoria: number;
    nome: string;
    tipo: string;
  };
}

export const titulosService = {
  listar: async (usuarioId: number): Promise<ITitulo[]> => {
    const { data } = await apiClient.get<ITitulo[]>('/titulos', {
      params: { usuarioId }
    });
    return data;
  },
  
  pagar: async (id: number, valor: number, dataPagamento: string, contaId: number, usuarioId: number): Promise<ITitulo> => {
    const { data } = await apiClient.post<ITitulo>(`/titulos/${id}/pagar`, {
      valor,
      data: dataPagamento,
      contaId,
      usuarioId
    });
    return data;
  },

  receber: async (id: number, valor: number, dataRecebimento: string, contaId: number, usuarioId: number): Promise<ITitulo> => {
    const { data } = await apiClient.post<ITitulo>(`/titulos/${id}/receber`, {
      valor,
      data: dataRecebimento,
      contaId,
      usuarioId
    });
    return data;
  },

  cancelar: async (id: number): Promise<ITitulo> => {
    const { data } = await apiClient.post<ITitulo>(`/titulos/${id}/cancelar`);
    return data;
  }
};
