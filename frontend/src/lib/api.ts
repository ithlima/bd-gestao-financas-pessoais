import type { ErroResponse } from "@/lib/types";

const BASE_URL = "/api/v1";
const TOKEN_KEY = "financas.token";
export const UNAUTHORIZED_EVENT = "financas:nao-autorizado";

export class ApiError extends Error {
  status: number;
  titulo: string;
  detalhes: string[];

  constructor(status: number, titulo: string, mensagem: string, detalhes: string[] = []) {
    super(mensagem);
    this.status = status;
    this.titulo = titulo;
    this.detalhes = detalhes;
  }
}

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  try {
    return window.localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token: string | null) {
  try {
    if (token) window.localStorage.setItem(TOKEN_KEY, token);
    else window.localStorage.removeItem(TOKEN_KEY);
  } catch {}
}

type Params = Record<string, string | number | boolean | null | undefined>;

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: unknown;
  params?: Params;
}

function buildUrl(path: string, params?: Params) {
  const search = new URLSearchParams();
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== "") {
        search.set(key, String(value));
      }
    }
  }
  const query = search.toString();
  return `${BASE_URL}${path}${query ? `?${query}` : ""}`;
}

async function parseError(response: Response): Promise<ApiError> {
  try {
    const data = (await response.json()) as Partial<ErroResponse>;
    return new ApiError(
      response.status,
      data.erro ?? "Erro",
      data.mensagem ?? "Não foi possível concluir a operação.",
      data.detalhes ?? [],
    );
  } catch {
    if (response.status >= 500) {
      return new ApiError(
        response.status,
        "Servidor indisponível",
        "Não foi possível se comunicar com a API. Verifique se o back-end está em execução.",
      );
    }
    return new ApiError(response.status, "Erro", "Não foi possível concluir a operação.");
  }
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = { Accept: "application/json" };
  if (options.body !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(buildUrl(path, options.params), {
      method: options.method ?? "GET",
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    });
  } catch {
    throw new ApiError(
      0,
      "Sem conexão",
      "Não foi possível se comunicar com a API. Verifique se o back-end está em execução.",
    );
  }

  if (!response.ok) {
    const error = await parseError(response);
    if (token && response.status === 401) {
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    }
    throw error;
  }

  if (response.status === 204) return undefined as T;
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
