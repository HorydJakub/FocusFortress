import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import AuthPage from './components/auth/AuthPage';
import Dashboard from './components/dashboard/Dashboard';
import Counters from './components/counters/Counters';
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
          <Route path="/habits" element={<ProtectedRoute><Layout><div style={{ padding: '80px 40px', textAlign: 'center' }}><h1 style={{ fontSize: '36px', color: '#ff6b35' }}>Habits Coming Soon!</h1></div></Layout></ProtectedRoute>} />
          <Route path="/media" element={<ProtectedRoute><Layout><div style={{ padding: '80px 40px', textAlign: 'center' }}><h1 style={{ fontSize: '36px', color: '#ff6b35' }}>My Media Coming Soon!</h1></div></Layout></ProtectedRoute>} />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;