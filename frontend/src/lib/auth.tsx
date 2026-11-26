"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { getToken, request, setToken, UNAUTHORIZED_EVENT } from "@/lib/api";
import type { LoginResponse, Usuario } from "@/lib/types";

type Status = "carregando" | "autenticado" | "anonimo";

interface AuthContextValue {
  status: Status;
  usuario: Usuario | null;
  entrar: (email: string, senha: string) => Promise<Usuario>;
  sair: () => void;
  atualizarUsuario: (usuario: Usuario) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<Status>("carregando");
  const [usuario, setUsuario] = useState<Usuario | null>(null);

  const sair = useCallback(() => {
    setToken(null);
    setUsuario(null);
    setStatus("anonimo");
    queryClient.clear();
  }, [queryClient]);

  useEffect(() => {
    let ativo = true;
    if (!getToken()) {
      queueMicrotask(() => ativo && setStatus("anonimo"));
      return () => {
        ativo = false;
      };
    }
    request<Usuario>("/usuarios/me")
      .then((me) => {
        if (!ativo) return;
        setUsuario(me);
        setStatus("autenticado");
      })
      .catch(() => {
        if (!ativo) return;
        setToken(null);
        setStatus("anonimo");
      });
    return () => {
      ativo = false;
    };
  }, []);

  useEffect(() => {
    window.addEventListener(UNAUTHORIZED_EVENT, sair);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, sair);
  }, [sair]);

  const entrar = useCallback(async (email: string, senha: string) => {
    const { token } = await request<LoginResponse>("/auth/login", {
      method: "POST",
      body: { email, senha },
    });
    setToken(token);
    const me = await request<Usuario>("/usuarios/me");
    setUsuario(me);
    setStatus("autenticado");
    return me;
  }, []);

  const value = useMemo(
    () => ({ status, usuario, entrar, sair, atualizarUsuario: setUsuario }),
    [status, usuario, entrar, sair],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth precisa estar dentro de AuthProvider");
  return context;
}

export function useUsuarioId() {
  const { usuario } = useAuth();
  return usuario?.idUsuario ?? 0;
}
