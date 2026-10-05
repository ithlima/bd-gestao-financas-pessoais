import React, { useEffect, useState } from 'react';
import { titulosService, ITitulo } from '../../services/titulos.service';
import { useAuthStore } from '@/modules/auth';

export const TitulosListPage = () => {
  const [titulos, setTitulos] = useState<ITitulo[]>([]);
  const [loading, setLoading] = useState(true);
  const logout = useAuthStore(s => s.logout);

  useEffect(() => {
    // Para simplificar, estamos assumindo usuarioId = 1. Num caso real, viria do AuthStore (token decoded)
    titulosService.listar(1)
      .then(setTitulos)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="p-8">Carregando títulos...</div>;

  return (
    <div className="p-8 max-w-5xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-800">Meus Títulos</h1>
        <button onClick={logout} className="px-4 py-2 bg-red-100 text-red-700 rounded hover:bg-red-200">
          Sair
        </button>
      </div>

      <div className="bg-white shadow rounded-lg overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Descrição</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Vencimento</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Valor</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {titulos.map((t) => (
              <tr key={t.idTitulo}>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{t.descricao}</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{t.dataVencimento}</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">R$ {t.valorPrevisto.toFixed(2)}</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full 
                    ${t.situacaoEfetiva === 'PAGO' ? 'bg-green-100 text-green-800' : 
                      t.situacaoEfetiva === 'VENCIDO' ? 'bg-red-100 text-red-800' : 'bg-yellow-100 text-yellow-800'}`}>
                    {t.situacaoEfetiva}
                  </span>
                </td>
              </tr>
            ))}
            {titulos.length === 0 && (
              <tr><td colSpan={4} className="px-6 py-4 text-center text-gray-500">Nenhum título encontrado.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
