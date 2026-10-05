import { useEffect, useState } from 'react';
import { apiClient } from '@/shared/api-client/apiClient';
import { Layout } from '@/shared/components/Layout';
import { getUsuarioIdFromToken } from '@/modules/auth';

interface IConta {
  idConta: number;
  nome: string;
  tipo: string;
  saldoAtual: number;
}

export const ContasListPage = () => {
  const [contas, setContas] = useState<IConta[]>([]);
  const usuarioId = getUsuarioIdFromToken();

  const load = () => {
    apiClient.get<IConta[]>('/contas', { params: { usuarioId } })
      .then(res => setContas(res.data))
      .catch(console.error);
  };

  useEffect(() => { load(); }, []);

  const handleCreate = async () => {
    const nome = prompt('Nome da Conta:');
    if (!nome) return;
    const tipo = prompt('Tipo (CORRENTE, POUPANCA, INVESTIMENTO):', 'CORRENTE');
    const saldo = prompt('Saldo Inicial:', '0.00');
    try {
      await apiClient.post('/contas', {
        nome, tipo, saldoInicial: parseFloat(saldo || '0'), usuarioId
      });
      load();
    } catch (e: any) {
      alert('Erro: ' + (e.response?.data?.message || e.message));
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Excluir conta?')) return;
    try {
      await apiClient.delete(`/contas/${id}`);
      load();
    } catch (e: any) {
      alert('Erro: ' + (e.response?.data?.message || e.message));
    }
  };

  return (
    <Layout>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-800">Minhas Contas</h1>
        <button onClick={handleCreate} className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">Nova Conta</button>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {contas.map(c => (
          <div key={c.idConta} className="bg-white shadow rounded-lg p-6 relative">
            <button onClick={() => handleDelete(c.idConta)} className="absolute top-4 right-4 text-red-500 hover:text-red-700 text-sm">Excluir</button>
            <h3 className="text-lg font-bold text-gray-800">{c.nome}</h3>
            <p className="text-gray-500 text-sm">{c.tipo}</p>
            <div className="mt-4">
              <span className="text-sm text-gray-500">Saldo Atual</span>
              <p className={`text-2xl font-bold ${c.saldoAtual >= 0 ? 'text-green-600' : 'text-red-600'}`}>R$ {c.saldoAtual.toFixed(2)}</p>
            </div>
          </div>
        ))}
      </div>
    </Layout>
  );
};
