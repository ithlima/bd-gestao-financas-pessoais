import { useEffect, useState } from 'react';
import { apiClient } from '@/shared/api-client/apiClient';
import { Layout } from '@/shared/components/Layout';
import { getUsuarioIdFromToken } from '@/modules/auth';

interface IMovimentacao {
  idMovimentacao: number;
  data: string;
  valor: number;
  tipo: 'RECEITA' | 'DESPESA';
  descricao: string;
  contaNome: string;
  categoriaNome: string;
}

export const MovimentacoesListPage = () => {
  const [movimentacoes, setMovimentacoes] = useState<IMovimentacao[]>([]);
  const usuarioId = getUsuarioIdFromToken();

  const load = () => {
    apiClient.get<IMovimentacao[]>('/movimentacoes', { params: { usuarioId } })
      .then(res => setMovimentacoes(res.data))
      .catch(console.error);
  };

  useEffect(() => { load(); }, []);

  const handleEstornar = async (id: number) => {
    if (!window.confirm('Deseja realmente estornar esta movimentação? (Isso pode reabrir um título pago)')) return;
    try {
      await apiClient.delete(`/movimentacoes/${id}`);
      load();
    } catch (e: any) {
      alert('Erro ao estornar: ' + (e.response?.data?.message || e.message));
    }
  };

  return (
    <Layout>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-800">Extrato (Movimentações)</h1>
      </div>
      <div className="bg-white shadow rounded-lg overflow-hidden border border-gray-200">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Data</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Descrição</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Conta / Cat</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Valor</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Ações</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {movimentacoes.map(m => (
              <tr key={m.idMovimentacao} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{m.data}</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{m.descricao}</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{m.contaNome} / {m.categoriaNome}</td>
                <td className={`px-6 py-4 whitespace-nowrap text-sm font-bold ${m.tipo === 'RECEITA' ? 'text-green-600' : 'text-red-600'}`}>
                  {m.tipo === 'RECEITA' ? '+' : '-'} R$ {m.valor.toFixed(2)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <button onClick={() => handleEstornar(m.idMovimentacao)} className="text-red-600 hover:text-red-900">Estornar</button>
                </td>
              </tr>
            ))}
            {movimentacoes.length === 0 && (
              <tr><td colSpan={5} className="px-6 py-8 text-center text-gray-500">Nenhuma movimentação registrada.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </Layout>
  );
};
