import React, { useEffect, useState } from 'react';
import api from '../../services/api';
import { Clock } from 'lucide-react';

const Counters = () => {
  const [counters, setCounters] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchCounters = async () => {
      setLoading(true);
      try {
        const res = await api.get('/counters');
        setCounters(res.data || []);
      } catch (e) {
        setError(e);
      } finally {
        setLoading(false);
      }
    };

    fetchCounters();
  }, []);

  return (
    <div style={{ padding: '80px 40px', maxWidth: '1000px', margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '24px' }}>
        <Clock size={28} color="#ff6b35" />
        <h1 style={{ margin: 0, color: '#ff6b35' }}>Counters</h1>
      </div>

      {loading && <p>Loading counters...</p>}
      {error && <p style={{ color: 'red' }}>Failed to load counters.</p>}

      {!loading && !error && counters.length === 0 && (
        <div style={{ padding: '40px', textAlign: 'center', background: 'white', borderRadius: 12 }}>
          <p style={{ margin: 0, color: '#666' }}>No counters yet. Create one to start tracking.</p>
        </div>
      )}

      {!loading && !error && counters.length > 0 && (
        <div style={{ display: 'grid', gap: 12 }}>
          {counters.map((c) => (
            <div key={c.id} style={{ background: 'white', padding: 16, borderRadius: 8 }}>
              <div style={{ fontWeight: 700 }}>{c.name}</div>
              <div style={{ color: '#666' }}>Value: {c.value}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default Counters;