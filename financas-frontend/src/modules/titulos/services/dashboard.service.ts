import { apiClient } from '@/shared/api-client/apiClient';

export interface IConta {
  idConta: number;
  nome: string;
  tipo: string;
  saldoAtual: number;
}

export const contasService = {
  listar: async (usuarioId: number): Promise<IConta[]> => {
    const { data } = await apiClient.get<IConta[]>('/contas', { params: { usuarioId } });
    return data;
  }
};

export const movimentacoesService = {
  estornar: async (movimentacaoId: number): Promise<void> => {
    await apiClient.delete(`/movimentacoes/${movimentacaoId}`);
  }
};
