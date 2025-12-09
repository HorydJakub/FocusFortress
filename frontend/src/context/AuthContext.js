import React, { createContext, useState, useContext, useEffect } from 'react';
import authService from '../services/authService';
import api from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const verifyToken = async () => {
      const token = localStorage.getItem('token') || sessionStorage.getItem('token');
      const currentUser = localStorage.getItem('user') || sessionStorage.getItem('user');

      if (!token || !currentUser) {
        setLoading(false);
        return;
      }

      try {
        // Validate token locally by checking JWT expiration
        const parts = token.split('.');
        if (parts.length === 3) {
          const tokenPayload = JSON.parse(atob(parts[1]));
          const expirationTime = tokenPayload.exp * 1000; // Convert to milliseconds

          if (Date.now() >= expirationTime) {
            console.log('Token expired, logging out...');
            authService.logout();
            setUser(null);
            setLoading(false);
            return;
          }

          // Token looks not expired -> accept user locally without calling a protected endpoint
          setUser(currentUser);
        } else {
          // Not a JWT: as a fallback, try a lightweight authenticated call to confirm token
          try {
            await api.get('/habits');
            setUser(currentUser);
          } catch (e) {
            console.error('Token verification failed:', e);
            authService.logout();
            setUser(null);
          }
        }
      } catch (error) {
        console.error('Token verification/decoding failed:', error);
        authService.logout();
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    verifyToken();
  }, []);

  const login = async (email, password, rememberMe = false) => {
    const data = await authService.login(email, password, rememberMe);
    setUser(email);
    return data;
  };

  const register = async (userData) => {
    return await authService.register(userData);
  };

  const logout = () => {
    authService.logout();
    setUser(null);
  };

  const value = {
    user,
    login,
    register,
    logout,
    isAuthenticated: !!user,
    loading
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};

export default AuthContext;