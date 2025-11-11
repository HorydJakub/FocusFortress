import React, { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Target, Clock, Trophy, CheckCircle2 } from 'lucide-react';
import api from '../../services/api';

const Dashboard = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState({
    activeHabits: 0,
    countersCount: 0,
    completedHabits: 0,
    bestStreak: 0
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchDashboardData();
    }, 100);

    return () => clearTimeout(timer);
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);

    try {
      // Fetch data with individual error handling
      let habits = [];
      let counters = [];

      try {
        const habitsRes = await api.get('/habits');
        habits = habitsRes.data || [];
      } catch (e) {
        console.error('Failed to fetch habits:', e);
      }

      try {
        const countersRes = await api.get('/counters');
        counters = countersRes.data || [];
      } catch (e) {
        console.error('Failed to fetch counters:', e);
      }

      // Calculate statistics
      const activeHabits = habits.filter(h => !h.done).length;
      const completedHabits = habits.filter(h => h.done).length;
      const countersCount = counters.length;

      // Find the best streak from all habits
      const bestStreak = habits.reduce((max, habit) => {
        const streak = habit.currentStreak || 0;
        return streak > max ? streak : max;
      }, 0);

      setStats({
        activeHabits,
        countersCount,
        completedHabits,
        bestStreak
      });
    } catch (e) {
      console.error('Failed to fetch dashboard data:', e);
      setError('Unable to load dashboard data. Please check if the backend is running.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '80px 40px', textAlign: 'center' }}>
        <div style={{ background: 'white', borderRadius: '24px', padding: '60px 40px', boxShadow: '0 20px 60px rgba(255, 107, 53, 0.15)' }}>
          <div style={{ fontSize: '20px', color: '#666' }}>Loading dashboard...</div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '80px 40px', textAlign: 'center' }}>
        <div style={{ background: 'white', borderRadius: '24px', padding: '60px 40px', boxShadow: '0 20px 60px rgba(255, 107, 53, 0.15)' }}>
          <div style={{ fontSize: '20px', color: '#dc3545', marginBottom: '16px' }}>{error}</div>
          <button
            onClick={fetchDashboardData}
            style={{
              padding: '12px 24px',
              background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
              color: 'white',
              border: 'none',
              borderRadius: '12px',
              fontSize: '16px',
              fontWeight: 'bold',
              cursor: 'pointer'
            }}
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  const statsData = [
    {
      title: 'Active Habits',
      count: stats.activeHabits,
      icon: Target,
      color: '#ff6b35',
      subtitle: stats.activeHabits === 1 ? 'habit' : 'habits'
    },
    {
      title: 'Counters',
      count: stats.countersCount,
      icon: Clock,
      color: '#ff8c42',
      subtitle: stats.countersCount === 1 ? 'counter' : 'counters'
    },
    {
      title: 'Completed Habits',
      count: stats.completedHabits,
      icon: CheckCircle2,
      color: '#4CAF50',
      subtitle: stats.completedHabits === 1 ? 'habit' : 'habits'
    },
    {
      title: 'Best Streak',
      count: stats.bestStreak,
      icon: Trophy,
      color: '#ffab73',
      suffix: stats.bestStreak === 1 ? ' day' : ' days'
    }
  ];

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '80px 40px', textAlign: 'center' }}>
      <div style={{ background: 'white', borderRadius: '24px', padding: '60px 40px', boxShadow: '0 20px 60px rgba(255, 107, 53, 0.15)' }}>
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
        <p style={{ fontSize: '20px', color: '#666', marginBottom: '40px' }}>
          Ready to continue building your fortress of good habits?
        </p>

        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
          gap: '24px',
          marginTop: '40px'
        }}>
          {statsData.map((stat, index) => {
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
                  cursor: 'pointer',
                  border: '2px solid transparent'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = 'translateY(-4px)';
                  e.currentTarget.style.boxShadow = '0 8px 24px rgba(0,0,0,0.1)';
                  e.currentTarget.style.borderColor = stat.color;
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = 'translateY(0)';
                  e.currentTarget.style.boxShadow = 'none';
                  e.currentTarget.style.borderColor = 'transparent';
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
                  color: 'white',
                  boxShadow: `0 4px 12px ${stat.color}40`
                }}>
                  <Icon size={28} />
                </div>
                <div style={{
                  fontSize: '36px',
                  fontWeight: 'bold',
                  color: '#333',
                  marginBottom: '8px'
                }}>
                  {stat.count}{stat.suffix || ''}
                </div>
                <div style={{
                  fontSize: '16px',
                  color: '#666',
                  fontWeight: '600',
                  marginBottom: '4px'
                }}>
                  {stat.title}
                </div>
                {stat.subtitle && (
                  <div style={{
                    fontSize: '13px',
                    color: '#999',
                    fontWeight: '400'
                  }}>
                    {stat.subtitle}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Motivational message based on stats */}
        {stats.activeHabits === 0 && stats.completedHabits === 0 && (
          <div style={{
            marginTop: '40px',
            padding: '24px',
            background: 'linear-gradient(135deg, #fff5eb 0%, #ffe8d6 100%)',
            borderRadius: '16px',
            border: '2px solid #ff6b35'
          }}>
            <p style={{
              margin: 0,
              fontSize: '18px',
              color: '#ff6b35',
              fontWeight: '600'
            }}>
              🎯 Start your journey today! Create your first habit to begin building your fortress.
            </p>
          </div>
        )}

        {stats.bestStreak >= 7 && (
          <div style={{
            marginTop: '40px',
            padding: '24px',
            background: 'linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%)',
            borderRadius: '16px',
            border: '2px solid #4CAF50'
          }}>
            <p style={{
              margin: 0,
              fontSize: '18px',
              color: '#2e7d32',
              fontWeight: '600'
            }}>
              🔥 Amazing! You have a {stats.bestStreak}-day streak! Keep up the great work!
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default Dashboard;