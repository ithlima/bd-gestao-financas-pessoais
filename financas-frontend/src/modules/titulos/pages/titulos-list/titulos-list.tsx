import { useEffect, useState } from 'react';
import { titulosService } from '../../services/titulos.service';
import type { ITitulo } from '../../services/titulos.service';
import { contasService, IConta } from '../../services/dashboard.service';
import { useAuthStore, getUsuarioIdFromToken } from '@/modules/auth';

export const TitulosListPage = () => {
  const [titulos, setTitulos] = useState<ITitulo[]>([]);
  const [contas, setContas] = useState<IConta[]>([]);
  const [loading, setLoading] = useState(true);
  const logout = useAuthStore(s => s.logout);
  const usuarioId = getUsuarioIdFromToken() || 1;

  const loadData = () => {
    setLoading(true);
    Promise.all([
      titulosService.listar(usuarioId),
      contasService.listar(usuarioId)
    ])
      .then(([t, c]) => {
        setTitulos(t);
        setContas(c);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadData();
  }, [usuarioId]);

  const handlePagarReceber = async (titulo: ITitulo) => {
    if (contas.length === 0) {
      alert("Você precisa criar uma conta antes de realizar operações.");
      return;
    }
    const valorStr = prompt(`Valor a ${titulo.tipo === 'DESPESA' ? 'pagar' : 'receber'} para "${titulo.descricao}":`, titulo.valorPrevisto.toString());
    if (!valorStr) return;
    
    const valor = parseFloat(valorStr);
    if (isNaN(valor) || valor <= 0) return alert("Valor inválido");

    const dataHoje = new Date().toISOString().split('T')[0];
    const contaId = contas[0].idConta; // Simplificação: usar primeira conta

    try {
      if (titulo.tipo === 'DESPESA') {
        await titulosService.pagar(titulo.idTitulo, valor, dataHoje, contaId, usuarioId);
      } else {
        await titulosService.receber(titulo.idTitulo, valor, dataHoje, contaId, usuarioId);
      }
      loadData();
    } catch (e: any) {
      alert("Erro ao processar: " + (e.response?.data?.message || e.message));
    }
  };

  const handleCancelar = async (titulo: ITitulo) => {
    if (!window.confirm(`Tem certeza que deseja cancelar "${titulo.descricao}"?`)) return;
    try {
      await titulosService.cancelar(titulo.idTitulo);
      loadData();
    } catch (e: any) {
      alert("Erro ao cancelar: " + (e.response?.data?.message || e.message));
    }
  };

  const handleCriarDadosDeTeste = async () => {
    try {
      const dataHoje = new Date().toISOString().split('T')[0];
      // Create conta
      const { data: conta } = await apiClient.post('/contas', {
        nome: "Conta Corrente",
        saldoInicial: 1000.00,
        tipo: "CORRENTE",
        usuarioId: usuarioId
      });
      // Create categoria
      const { data: catDespesa } = await apiClient.post('/categorias', {
        nome: "Alimentação",
        tipo: "DESPESA",
        usuarioId: usuarioId
      });
      const { data: catReceita } = await apiClient.post('/categorias', {
        nome: "Salário",
        tipo: "RECEITA",
        usuarioId: usuarioId
      });
      // Create titulos
      await apiClient.post('/titulos', {
        descricao: "Compra do mês",
        valorPrevisto: 300.00,
        dataVencimento: dataHoje,
        tipo: "DESPESA",
        categoriaId: catDespesa.idCategoria,
        usuarioId: usuarioId
      });
      await apiClient.post('/titulos', {
        descricao: "Adiantamento Salarial",
        valorPrevisto: 1500.00,
        dataVencimento: dataHoje,
        tipo: "RECEITA",
        categoriaId: catReceita.idCategoria,
        usuarioId: usuarioId
      });
      loadData();
    } catch (e) {
      alert("Erro ao criar dados de teste.");
    }
  };

  const totalSaldo = contas.reduce((acc, c) => acc + c.saldoAtual, 0);

  if (loading) return <div className="p-8 text-center text-gray-500">Carregando painel...</div>;

  return (
    <div className="p-8 max-w-6xl mx-auto">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-800">Painel Financeiro</h1>
          <p className="text-gray-500 mt-1">Saldo Total: <strong className={totalSaldo >= 0 ? 'text-green-600' : 'text-red-600'}>R$ {totalSaldo.toFixed(2)}</strong></p>
        </div>
        <div className="space-x-4">
          {titulos.length === 0 && contas.length === 0 && (
            <button onClick={handleCriarDadosDeTeste} className="px-4 py-2 bg-indigo-100 text-indigo-700 font-medium rounded hover:bg-indigo-200">
              + Gerar Dados de Teste
            </button>
          )}
          <button onClick={logout} className="px-4 py-2 bg-red-100 text-red-700 font-medium rounded hover:bg-red-200">
            Sair da Conta
          </button>
        </div>
      </div>

      <div className="bg-white shadow rounded-lg overflow-hidden border border-gray-200">
        <div className="px-6 py-4 border-b border-gray-200 bg-gray-50 flex justify-between items-center">
          <h2 className="text-xl font-semibold text-gray-700">Títulos (Contas a Pagar/Receber)</h2>
        </div>
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Descrição</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Tipo</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Vencimento</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Valor</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Ações</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {titulos.map((t) => (
              <tr key={t.idTitulo} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{t.descricao}</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  <span className={t.tipo === 'RECEITA' ? 'text-green-600' : 'text-red-600'}>{t.tipo}</span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{t.dataVencimento}</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">R$ {t.valorPrevisto.toFixed(2)}</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full 
                    ${t.situacaoEfetiva === 'PAGO' ? 'bg-green-100 text-green-800' : 
                      t.situacaoEfetiva === 'VENCIDO' ? 'bg-red-100 text-red-800' : 
                      t.situacaoEfetiva === 'CANCELADO' ? 'bg-gray-100 text-gray-800' : 'bg-yellow-100 text-yellow-800'}`}>
                    {t.situacaoEfetiva}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  {t.situacaoEfetiva !== 'PAGO' && t.situacaoEfetiva !== 'CANCELADO' && (
                    <>
                      <button 
                        onClick={() => handlePagarReceber(t)} 
                        className="text-blue-600 hover:text-blue-900 mr-3"
                      >
                        {t.tipo === 'DESPESA' ? 'Pagar' : 'Receber'}
                      </button>
                      <button 
                        onClick={() => handleCancelar(t)} 
                        className="text-red-600 hover:text-red-900"
                      >
                        Cancelar
                      </button>
                    </>
                  )}
                  {t.situacaoEfetiva === 'PAGO' && (
                    <span className="text-gray-400">Finalizado</span>
                  )}
                </td>
              </tr>
            ))}
            {titulos.length === 0 && (
              <tr><td colSpan={6} className="px-6 py-8 text-center text-gray-500">Nenhum título encontrado para o seu usuário.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
