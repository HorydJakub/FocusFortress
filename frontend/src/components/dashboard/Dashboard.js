import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Home, Target, Clock, Image, LogOut } from 'lucide-react';

const Dashboard = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

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
    <div style={{ minHeight: '100vh', background: 'linear-gradient(135deg, #fff5eb 0%, #ffe8d6 100%)' }}>
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
            backgroundClip: 'text'
          }}>
            FocusFortress
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            {menuItems.map((item) => {
              const Icon = item.icon;
              const isActive = window.location.pathname === item.path;

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
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            padding: '8px 16px',
            background: '#f8f8f8',
            borderRadius: '12px'
          }}>
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
              {user ? user.charAt(0).toUpperCase() : 'U'}
            </div>
            <span style={{ fontWeight: '600', color: '#333' }}>
              {user ? user.split('@')[0] : 'User'}
            </span>
          </div>

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
      <div style={{
        maxWidth: '1200px',
        margin: '0 auto',
        padding: '80px 40px',
        textAlign: 'center'
      }}>
        <div style={{
          background: 'white',
          borderRadius: '24px',
          padding: '60px 40px',
          boxShadow: '0 20px 60px rgba(255, 107, 53, 0.15)'
        }}>
          <div style={{
            fontSize: '48px',
            fontWeight: 'bold',
            background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundClip: 'text',
            marginBottom: '16px'
          }}>
            Welcome back, {user ? user.split('@')[0] : 'User'}!
          </div>

          <p style={{
            fontSize: '20px',
            color: '#666',
            marginBottom: '40px'
          }}>
            Ready to continue building your fortress of good habits?
          </p>

          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
            gap: '24px',
            marginTop: '40px'
          }}>
            {[
              { title: 'Active Habits', count: '0', icon: Target, color: '#ff6b35' },
              { title: 'Counters', count: '0', icon: Clock, color: '#ff8c42' },
              { title: 'Current Streak', count: '0 days', icon: Home, color: '#ffab73' }
            ].map((stat, index) => {
              const Icon = stat.icon;
              return (
                <div
                  key={index}
                  style={{
                    background: '#f8f8f8',
                    borderRadius: '16px',
                    padding: '32px 24px',
                    textAlign: 'center',
                    transition: 'transform 0.2s, box-shadow 0.2s',
                    cursor: 'pointer'
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.transform = 'translateY(-4px)';
                    e.currentTarget.style.boxShadow = '0 8px 24px rgba(0,0,0,0.1)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.transform = 'translateY(0)';
                    e.currentTarget.style.boxShadow = 'none';
                  }}
                >
                  <div style={{
                    width: '60px',
                    height: '60px',
                    borderRadius: '50%',
                    background: stat.color,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    margin: '0 auto 16px',
                    color: 'white'
                  }}>
                    <Icon size={28} />
                  </div>
                  <div style={{
                    fontSize: '32px',
                    fontWeight: 'bold',
                    color: '#333',
                    marginBottom: '8px'
                  }}>
                    {stat.count}
                  </div>
                  <div style={{
                    fontSize: '16px',
                    color: '#666',
                    fontWeight: '500'
                  }}>
                    {stat.title}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;