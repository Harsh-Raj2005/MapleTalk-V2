import axios from "axios";
import { clearToken, getToken } from "./token";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

export const axiosInstance = axios.create({
  baseURL: BASE_URL,
});

// V2 uses stateless Bearer-token auth, not cookies — attach the stored JWT
// to every request instead of relying on withCredentials.
axiosInstance.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// A 401 means the stored token is missing/expired/invalid — drop it so we
// don't keep re-attaching a known-dead token on subsequent requests.
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearToken();
    }
    return Promise.reject(error);
  }
);
