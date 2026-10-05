import { useEffect, useState } from 'react';
import { apiClient } from '@/shared/api-client/apiClient';
import { Layout } from '@/shared/components/Layout';
import { getUsuarioIdFromToken } from '@/modules/auth';

interface IDreLinha {
  categoriaId: number;
  categoriaNome: string;
  tipo: string;
  valorPrevisto: number;
  valorRealizado: number;
  variacao: number;
}

interface IDreResponse {
  modo: string;
  inicio: string;
  fim: string;
  linhasReceitas: IDreLinha[];
  totalReceitasPrevistas: number;
  totalReceitasRealizadas: number;
  linhasDespesas: IDreLinha[];
  totalDespesasPrevistas: number;
  totalDespesasRealizadas: number;
  resultadoPrevisto: number;
  resultadoRealizado: number;
  totalVencido: number;
}

export const DreDashboardPage = () => {
  const [dre, setDre] = useState<IDreResponse | null>(null);
  const usuarioId = getUsuarioIdFromToken();

  useEffect(() => {
    // Busca DRE do mes atual (competencia)
    const data = new Date();
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, '0');
    
    apiClient.get<IDreResponse>('/dre', { 
      params: { usuarioId, modo: 'COMPETENCIA', ano, mes } 
    })
      .then(res => setDre(res.data))
      .catch(console.error);
  }, []);

  if (!dre) return <Layout><div className="p-8">Carregando DRE...</div></Layout>;

  return (
    <Layout>
      <div className="mb-6 flex justify-between items-end">
        <div>
          <h1 className="text-3xl font-bold text-gray-800">DRE - Demonstrativo de Resultado</h1>
          <p className="text-gray-500">Período: {dre.inicio} até {dre.fim} (Competência)</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white p-6 rounded-lg shadow border-t-4 border-green-500">
          <h3 className="text-gray-500 font-medium text-sm">Receitas Realizadas</h3>
          <p className="text-2xl font-bold text-green-600">R$ {dre.totalReceitasRealizadas.toFixed(2)}</p>
          <p className="text-xs text-gray-400 mt-1">Previsto: R$ {dre.totalReceitasPrevistas.toFixed(2)}</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow border-t-4 border-red-500">
          <h3 className="text-gray-500 font-medium text-sm">Despesas Realizadas</h3>
          <p className="text-2xl font-bold text-red-600">R$ {dre.totalDespesasRealizadas.toFixed(2)}</p>
          <p className="text-xs text-gray-400 mt-1">Previsto: R$ {dre.totalDespesasPrevistas.toFixed(2)}</p>
        </div>
        <div className={`bg-white p-6 rounded-lg shadow border-t-4 ${dre.resultadoRealizado >= 0 ? 'border-blue-500' : 'border-orange-500'}`}>
          <h3 className="text-gray-500 font-medium text-sm">Resultado Final</h3>
          <p className={`text-2xl font-bold ${dre.resultadoRealizado >= 0 ? 'text-blue-600' : 'text-orange-600'}`}>
            R$ {dre.resultadoRealizado.toFixed(2)}
          </p>
          <p className="text-xs text-gray-400 mt-1">Previsto: R$ {dre.resultadoPrevisto.toFixed(2)}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-white shadow rounded-lg p-6">
          <h2 className="text-lg font-bold text-gray-800 mb-4 border-b pb-2">Receitas por Categoria</h2>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-gray-500">
                <th className="pb-2">Categoria</th>
                <th className="pb-2 text-right">Realizado</th>
                <th className="pb-2 text-right">Previsto</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {dre.linhasReceitas.map(r => (
                <tr key={r.categoriaId}>
                  <td className="py-2 font-medium">{r.categoriaNome}</td>
                  <td className="py-2 text-right text-green-600">R$ {r.valorRealizado.toFixed(2)}</td>
                  <td className="py-2 text-right text-gray-400">R$ {r.valorPrevisto.toFixed(2)}</td>
                </tr>
              ))}
              {dre.linhasReceitas.length === 0 && <tr><td colSpan={3} className="py-2 text-gray-400">Sem dados</td></tr>}
            </tbody>
          </table>
        </div>

        <div className="bg-white shadow rounded-lg p-6">
          <h2 className="text-lg font-bold text-gray-800 mb-4 border-b pb-2">Despesas por Categoria</h2>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-gray-500">
                <th className="pb-2">Categoria</th>
                <th className="pb-2 text-right">Realizado</th>
                <th className="pb-2 text-right">Previsto</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {dre.linhasDespesas.map(d => (
                <tr key={d.categoriaId}>
                  <td className="py-2 font-medium">{d.categoriaNome}</td>
                  <td className="py-2 text-right text-red-600">R$ {d.valorRealizado.toFixed(2)}</td>
                  <td className="py-2 text-right text-gray-400">R$ {d.valorPrevisto.toFixed(2)}</td>
                </tr>
              ))}
              {dre.linhasDespesas.length === 0 && <tr><td colSpan={3} className="py-2 text-gray-400">Sem dados</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </Layout>
  );
};
