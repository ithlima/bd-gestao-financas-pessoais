import { apiClient } from '@/shared/api-client/apiClient';

export interface ILoginRequest {
  email: string;
  senha: string;
}

export interface ILoginResponse {
  token: string;
}

export const authService = {
  login: async (credentials: ILoginRequest): Promise<ILoginResponse> => {
    const { data } = await apiClient.post<ILoginResponse>('/auth/login', credentials);
    return data;
  },
};
