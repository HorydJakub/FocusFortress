import api from './api';

const authService = {
  register: async (userData) => {
    try {
      const response = await api.post('/auth/register', userData);
      return response.data;
    } catch (error) {
      throw error.response?.data?.message || 'Registration failed';
    }
  },

  login: async (email, password) => {
    try {
      const response = await api.post('/auth/login', { email, password });
      const { accessToken } = response.data;

      if (accessToken) {
        localStorage.setItem('token', accessToken);
        localStorage.setItem('user', email);
      }

      return response.data;
    } catch (error) {
      throw error.response?.data?.message || 'Login failed';
    }
  },

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  getCurrentUser: () => {
    return localStorage.getItem('user');
  },

  isAuthenticated: () => {
    return !!localStorage.getItem('token');
  },

  verify: async (token) => {
    try {
      const response = await api.get(`/auth/verify?token=${token}`);
      return response.data;
    } catch (error) {
      throw error.response?.data?.message || 'Verification failed';
    }
  }
};

export default authService;