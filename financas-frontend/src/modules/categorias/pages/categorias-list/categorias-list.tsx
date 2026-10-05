import { useEffect, useState } from 'react';
import { apiClient } from '@/shared/api-client/apiClient';
import { Layout } from '@/shared/components/Layout';
import { getUsuarioIdFromToken } from '@/modules/auth';

interface ICategoria {
  idCategoria: number;
  nome: string;
  tipo: string;
}

export const CategoriasListPage = () => {
  const [categorias, setCategorias] = useState<ICategoria[]>([]);
  const usuarioId = getUsuarioIdFromToken();

  const load = () => {
    apiClient.get<ICategoria[]>('/categorias', { params: { usuarioId } })
      .then(res => setCategorias(res.data))
      .catch(console.error);
  };

  useEffect(() => { load(); }, []);

  const handleCreate = async () => {
    const nome = prompt('Nome da Categoria:');
    if (!nome) return;
    const tipo = prompt('Tipo (RECEITA ou DESPESA):', 'DESPESA');
    try {
      await apiClient.post('/categorias', { nome, tipo, usuarioId });
      load();
    } catch (e: any) {
      alert('Erro: ' + (e.response?.data?.message || e.message));
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Excluir categoria?')) return;
    try {
      await apiClient.delete(`/categorias/${id}`);
      load();
    } catch (e: any) {
      alert('Erro: ' + (e.response?.data?.message || e.message));
    }
  };

  return (
    <Layout>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-800">Categorias</h1>
        <button onClick={handleCreate} className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">Nova Categoria</button>
      </div>
      <div className="bg-white shadow rounded-lg overflow-hidden border border-gray-200">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Nome</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Tipo</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Ações</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {categorias.map(c => (
              <tr key={c.idCategoria}>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{c.nome}</td>
                <td className={`px-6 py-4 whitespace-nowrap text-sm font-bold ${c.tipo === 'RECEITA' ? 'text-green-600' : 'text-red-600'}`}>
                  {c.tipo}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm">
                  <button onClick={() => handleDelete(c.idCategoria)} className="text-red-600 hover:text-red-900">Excluir</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Layout>
  );
};
