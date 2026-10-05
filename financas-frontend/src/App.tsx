
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { LoginPage, RegisterPage, useAuthStore } from '@/modules/auth';
import { TitulosListPage } from '@/modules/titulos';

import { MovimentacoesListPage } from '@/modules/movimentacoes';
import { DreDashboardPage } from '@/modules/dre';
import { ContasListPage } from '@/modules/contas';
import { CategoriasListPage } from '@/modules/categorias';

const PrivateRoute = ({ children }: { children: React.ReactNode }) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" />;
};

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/" element={<PrivateRoute><TitulosListPage /></PrivateRoute>} />
        <Route path="/movimentacoes" element={<PrivateRoute><MovimentacoesListPage /></PrivateRoute>} />
        <Route path="/dre" element={<PrivateRoute><DreDashboardPage /></PrivateRoute>} />
        <Route path="/contas" element={<PrivateRoute><ContasListPage /></PrivateRoute>} />
        <Route path="/categorias" element={<PrivateRoute><CategoriasListPage /></PrivateRoute>} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
