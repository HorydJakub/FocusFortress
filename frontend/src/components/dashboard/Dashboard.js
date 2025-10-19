import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { Home, Target, Clock } from 'lucide-react';

const Dashboard = () => {
  const { user } = useAuth();

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '80px 40px', textAlign: 'center' }}>
      <div style={{ background: 'white', borderRadius: '24px', padding: '60px 40px', boxShadow: '0 20px 60px rgba(255, 107, 53, 0.15)' }}>
        <div style={{ fontSize: '48px', fontWeight: 'bold', background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text', marginBottom: '16px' }}>
          Welcome back, {user ? user.split('@')[0] : 'User'}!
        </div>
        <p style={{ fontSize: '20px', color: '#666', marginBottom: '40px' }}>
          Ready to continue building your fortress of good habits?
        </p>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '24px', marginTop: '40px' }}>
          {[
            { title: 'Active Habits', count: '0', icon: Target, color: '#ff6b35' },
            { title: 'Counters', count: '0', icon: Clock, color: '#ff8c42' },
            { title: 'Current Streak', count: '0 days', icon: Home, color: '#ffab73' }
          ].map((stat, index) => {
            const Icon = stat.icon;
            return (
              <div key={index} style={{ background: '#f8f8f8', borderRadius: '16px', padding: '32px 24px', textAlign: 'center', transition: 'transform 0.2s, box-shadow 0.2s', cursor: 'pointer' }}
                onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-4px)'; e.currentTarget.style.boxShadow = '0 8px 24px rgba(0,0,0,0.1)'; }}
                onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = 'none'; }}>
                <div style={{ width: '60px', height: '60px', borderRadius: '50%', background: stat.color, display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px', color: 'white' }}>
                  <Icon size={28} />
                </div>
                <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#333', marginBottom: '8px' }}>{stat.count}</div>
                <div style={{ fontSize: '16px', color: '#666', fontWeight: '500' }}>{stat.title}</div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;