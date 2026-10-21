import axios from 'axios';

export const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
export const BACKEND_URL = API_BASE_URL.replace(/\/api\/?$/, '');

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response) {
      // A 401 from /auth/* (e.g. wrong password) is handled by the form itself
      const isAuthRequest = error.config?.url?.startsWith('/auth/');
      if (error.response.status === 401 && !isAuthRequest) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        localStorage.removeItem('rememberMe');
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('user');
        window.location.href = '/login';
      }

      if (error.response.status === 403) {
        console.error('Access denied');
      }

      if (error.response.status === 404) {
        console.error('Resource not found');
      }
    } else if (error.request) {
      console.error('No response from server');
    } else {
      console.error('Error:', error.message);
    }

    return Promise.reject(error);
  }
);

export default api;