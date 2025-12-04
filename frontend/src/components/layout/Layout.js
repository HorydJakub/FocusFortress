import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Home, Target, Clock, Image, LogOut } from 'lucide-react';
import Settings from '../settings/Settings';
import api from '../../services/api'; // Import API to fetch the real user name

const Layout = ({ children }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [displayName, setDisplayName] = useState(''); // State to hold the real name from DB

  // Function to fetch the user's name from the backend
  const fetchUserName = async () => {
    try {
      if (user) {
        const response = await api.get('/user/profile');
        if (response.data && response.data.name) {
          setDisplayName(response.data.name);
        }
      }
    } catch (error) {
      console.error("Failed to fetch user name", error);
    }
  };

  useEffect(() => {
    // 1. Fetch name on component mount
    fetchUserName();

    // 2. Listen for 'profileUpdated' event triggered by Settings.js
    // This ensures the name updates immediately after editing
    const handleProfileUpdate = () => fetchUserName();
    window.addEventListener('profileUpdated', handleProfileUpdate);

    // Cleanup listener
    return () => {
      window.removeEventListener('profileUpdated', handleProfileUpdate);
    };
  }, [user]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const menuItems = [
    { name: 'Dashboard', icon: Home, path: '/dashboard' },
    { name: 'Habits', icon: Target, path: '/habits' },
    { name: 'Counters', icon: Clock, path: '/counters' },
    { name: 'My Media', icon: Image, path: '/media' }
  ];

  return (
    <div style={{ minHeight: '100vh', background: '#fff5eb' }}>
      {/* Top Navigation Bar */}
      <nav style={{
        background: 'white',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        padding: '0 40px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        height: '70px'
      }}>
        {/* Logo and Menu */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '40px' }}>
          <div style={{
            fontSize: '24px',
            fontWeight: 'bold',
            background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundClip: 'text',
            cursor: 'pointer'
          }}
          onClick={() => navigate('/dashboard')}
          >
            FocusFortress
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            {menuItems.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.path;

              return (
                <button
                  key={item.name}
                  onClick={() => navigate(item.path)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    padding: '10px 20px',
                    border: 'none',
                    borderRadius: '8px',
                    background: isActive ? 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)' : 'transparent',
                    color: isActive ? 'white' : '#666',
                    fontSize: '16px',
                    fontWeight: isActive ? '600' : '500',
                    cursor: 'pointer',
                    transition: 'all 0.2s'
                  }}
                  onMouseEnter={(e) => {
                    if (!isActive) {
                      e.currentTarget.style.background = '#f5f5f5';
                      e.currentTarget.style.color = '#ff6b35';
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (!isActive) {
                      e.currentTarget.style.background = 'transparent';
                      e.currentTarget.style.color = '#666';
                    }
                  }}
                >
                  <Icon size={20} />
                  {item.name}
                </button>
              );
            })}
          </div>
        </div>

        {/* User Menu */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <button
            onClick={() => setIsSettingsOpen(true)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              padding: '8px 16px',
              background: '#f8f8f8',
              borderRadius: '12px',
              border: 'none',
              cursor: 'pointer',
              transition: 'all 0.2s'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = '#e8e8e8';
              e.currentTarget.style.transform = 'translateY(-2px)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = '#f8f8f8';
              e.currentTarget.style.transform = 'translateY(0)';
            }}
          >
            <div style={{
              width: '36px',
              height: '36px',
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'white',
              fontWeight: 'bold',
              fontSize: '16px'
            }}>
              {displayName ? displayName.charAt(0).toUpperCase() : (user ? user.charAt(0).toUpperCase() : 'U')}
            </div>
            <span style={{ fontWeight: '600', color: '#333' }}>
              {displayName || (user ? user.split('@')[0] : 'User')}
            </span>
          </button>

          <button
            onClick={handleLogout}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '10px 16px',
              border: '2px solid #ff6b35',
              borderRadius: '8px',
              background: 'white',
              color: '#ff6b35',
              fontSize: '14px',
              fontWeight: '600',
              cursor: 'pointer',
              transition: 'all 0.2s'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = '#ff6b35';
              e.currentTarget.style.color = 'white';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'white';
              e.currentTarget.style.color = '#ff6b35';
            }}
          >
            <LogOut size={18} />
            Logout
          </button>
        </div>
      </nav>

      {/* Main Content */}
      {children}

      {/* Settings Modal */}
      <Settings isOpen={isSettingsOpen} onClose={() => setIsSettingsOpen(false)} />
    </div>
  );
};

export default Layout;