import { create } from 'zustand';

export interface IAuthUiState {
  token: string | null;
  setToken: (token: string) => void;
  logout: () => void;
  isAuthenticated: boolean;
}

const getStoredToken = () => localStorage.getItem('token');

export const useAuthStore = create<IAuthUiState>()((set) => ({
  token: getStoredToken(),
  isAuthenticated: !!getStoredToken(),
  setToken: (token: string) => {
    localStorage.setItem('token', token);
    set({ token, isAuthenticated: true });
  },
  logout: () => {
    localStorage.removeItem('token');
    set({ token: null, isAuthenticated: false });
  },
}));

export const getUsuarioIdFromToken = (): number | null => {
  const token = useAuthStore.getState().token;
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return parseInt(payload.sub, 10);
  } catch (e) {
    return null;
  }
};
