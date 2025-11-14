import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import AuthPage from './components/auth/AuthPage';
import Dashboard from './components/dashboard/Dashboard';
import Counters from './components/counters/Counters';
import Habits from './components/habits/Habits';
import MyMedia from './components/media/MyMedia';
import Layout from './components/layout/Layout';

const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();
  if (loading) return <div>Loading...</div>;
  return isAuthenticated ? children : <Navigate to="/login" replace />;
};

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<AuthPage />} />
          <Route path="/dashboard" element={<ProtectedRoute><Layout><Dashboard /></Layout></ProtectedRoute>} />
          <Route path="/counters" element={<ProtectedRoute><Layout><Counters /></Layout></ProtectedRoute>} />
          <Route path="/habits" element={<ProtectedRoute><Layout><Habits /></Layout></ProtectedRoute>} />
          <Route path="/media" element={<ProtectedRoute><Layout><MyMedia /></Layout></ProtectedRoute>} />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
