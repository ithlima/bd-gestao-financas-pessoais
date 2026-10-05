import type { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/modules/auth';

export const Layout = ({ children }: { children: ReactNode }) => {
  const logout = useAuthStore((s) => s.logout);
  const location = useLocation();

  const links = [
    { to: '/', label: 'Títulos' },
    { to: '/movimentacoes', label: 'Extrato' },
    { to: '/dre', label: 'DRE (Relatório)' },
    { to: '/contas', label: 'Contas' },
    { to: '/categorias', label: 'Categorias' },
  ];

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col">
      <nav className="bg-blue-600 text-white shadow-md">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <span className="font-bold text-xl mr-8">Finanças Pessoais</span>
              <div className="flex space-x-4">
                {links.map((link) => (
                  <Link
                    key={link.to}
                    to={link.to}
                    className={`px-3 py-2 rounded-md text-sm font-medium ${
                      location.pathname === link.to ? 'bg-blue-800 text-white' : 'text-blue-100 hover:bg-blue-700'
                    }`}
                  >
                    {link.label}
                  </Link>
                ))}
              </div>
            </div>
            <div className="flex items-center">
              <button
                onClick={logout}
                className="px-3 py-2 rounded-md text-sm font-medium text-red-100 hover:bg-red-600 bg-red-500"
              >
                Sair
              </button>
            </div>
          </div>
        </div>
      </nav>
      <main className="flex-1 max-w-7xl w-full mx-auto p-6">
        {children}
      </main>
    </div>
  );
};
