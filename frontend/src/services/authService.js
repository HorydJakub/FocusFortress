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
  },

  // User Profile Management
  getUserProfile: async () => {
    try {
      const response = await api.get('/user/profile');
      return response.data;
    } catch (error) {
      throw error.response?.data?.message || 'Failed to fetch profile';
    }
  },

  updateUserProfile: async (profileData) => {
    try {
      const response = await api.put('/user/profile', profileData);
      return response.data;
    } catch (error) {
      throw error.response?.data?.message || 'Failed to update profile';
    }
  },

  // User Interests Management
  getUserInterests: async () => {
    try {
      const response = await api.get('/user/interests');
      return response.data;
    } catch (error) {
      throw error.response?.data?.message || 'Failed to fetch interests';
    }
  },

  manageUserInterests: async (interestsData) => {
    try {
      const response = await api.put('/user/interests', interestsData);
      return response.data;
    } catch (error) {
      throw error.response?.data?.message || 'Failed to update interests';
    }
  },

  addCustomInterest: async (customInterestData) => {
    try {
      const response = await api.post('/user/interests/custom', customInterestData);
      return response.data;
    } catch (error) {
      throw error.response?.data?.message || 'Failed to add custom interest';
    }
  },

  deleteCustomInterest: async (interestId) => {
    try {
      const response = await api.delete(`/user/interests/${interestId}`);
      return response.data;
    } catch (error) {
      throw error.response?.data?.message || 'Failed to delete custom interest';
    }
  }
};

export default authService;