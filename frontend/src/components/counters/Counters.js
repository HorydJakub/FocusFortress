import React, { useEffect, useState } from 'react';
import api from '../../services/api';
import { Clock, Plus, X, Trash2, RotateCcw, Smile } from 'lucide-react';
import EmojiPicker from 'emoji-picker-react';

const DEFAULT_COUNTERS = [
  { name: 'Vodka', icon: '🍾' },
  { name: 'Cigarettes', icon: '🚬' },
  { name: 'THC', icon: '🌿' },
  { name: 'Negative Media', icon: '📱' }
];

const Counters = () => {
  const [counters, setCounters] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [useDefaults, setUseDefaults] = useState(true);
  const [newCounter, setNewCounter] = useState({
    name: '',
    description: '',
    icon: ''
  });
  const [selectedDefault, setSelectedDefault] = useState('');
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [currentTimes, setCurrentTimes] = useState({});

  useEffect(() => {
    fetchCounters();
  }, []);

  useEffect(() => {
    const interval = setInterval(() => {
      const times = {};
      counters.forEach(counter => {
        times[counter.id] = calculateTimeDifference(counter.startDateTime);
      });
      setCurrentTimes(times);
    }, 1000);

    return () => clearInterval(interval);
  }, [counters]);

  const fetchCounters = async () => {
    setLoading(true);
    try {
      const res = await api.get('/counters');
      setCounters(res.data || []);
      setError(null);
    } catch (e) {
      setError('Failed to load counters');
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const calculateTimeDifference = (startDateTime) => {
    const start = new Date(startDateTime);
    const now = new Date();
    const diff = now - start;

    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff % (1000 * 60)) / 1000);

    return { days, hours, minutes, seconds };
  };

  const formatTime = (time) => {
    if (!time) return '00::00::00::00';
    const { days, hours, minutes, seconds } = time;
    return `${String(days).padStart(2, '0')}::${String(hours).padStart(2, '0')}::${String(minutes).padStart(2, '0')}::${String(seconds).padStart(2, '0')}`;
  };

  const handleEmojiClick = (emojiData) => {
    setNewCounter({ ...newCounter, icon: emojiData.emoji });
    setShowEmojiPicker(false);
  };

  const handleAddCounter = async () => {
    try {
      const counterData = {
        name: useDefaults ? selectedDefault : newCounter.name,
        description: newCounter.description,
        icon: useDefaults
          ? DEFAULT_COUNTERS.find(c => c.name === selectedDefault)?.icon
          : newCounter.icon
      };

      if (!counterData.name) {
        alert('Please provide a counter name');
        return;
      }

      await api.post('/counters', counterData);
      await fetchCounters();
      setShowAddModal(false);
      resetForm();
    } catch (e) {
      console.error('Failed to create counter:', e);
      alert(e.response?.data?.message || 'Failed to create counter');
    }
  };

  const handleDeleteCounter = async (counterId) => {
    if (!window.confirm('Are you sure you want to delete this counter?')) {
      return;
    }
    try {
      await api.delete(`/counters/${counterId}`);
      await fetchCounters();
    } catch (e) {
      console.error('Failed to delete counter:', e);
      alert('Failed to delete counter');
    }
  };

  const handleResetCounter = async (counterId) => {
    if (!window.confirm('Are you sure you want to reset this counter?')) {
      return;
    }
    try {
      await api.patch(`/counters/${counterId}/reset`);
      await fetchCounters();
    } catch (e) {
      console.error('Failed to reset counter:', e);
      alert('Failed to reset counter');
    }
  };

  const resetForm = () => {
    setNewCounter({ name: '', description: '', icon: '' });
    setSelectedDefault('');
    setShowEmojiPicker(false);
    setUseDefaults(true);
  };

  return (
    <div style={{ padding: '80px 40px', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <Clock size={32} color="#ff6b35" />
          <h1 style={{ margin: 0, color: '#ff6b35', fontSize: '36px' }}>Counters</h1>
        </div>
        <button
          onClick={() => setShowAddModal(true)}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            padding: '12px 24px',
            background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
            color: 'white',
            border: 'none',
            borderRadius: '12px',
            fontSize: '16px',
            fontWeight: 'bold',
            cursor: 'pointer',
            boxShadow: '0 4px 12px rgba(255, 107, 53, 0.3)',
            transition: 'transform 0.2s, box-shadow 0.2s'
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.transform = 'translateY(-2px)';
            e.currentTarget.style.boxShadow = '0 6px 16px rgba(255, 107, 53, 0.4)';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.transform = 'translateY(0)';
            e.currentTarget.style.boxShadow = '0 4px 12px rgba(255, 107, 53, 0.3)';
          }}
        >
          <Plus size={20} />
          Add Counter
        </button>
      </div>

      {loading && <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>Loading counters...</div>}
      {error && <div style={{ textAlign: 'center', padding: '40px', color: '#dc3545' }}>{error}</div>}

      {!loading && !error && counters.length === 0 && (
        <div style={{
          padding: '60px 40px',
          textAlign: 'center',
          background: 'white',
          borderRadius: '16px',
          boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
        }}>
          <Clock size={64} color="#ff6b35" style={{ opacity: 0.3, marginBottom: '16px' }} />
          <h2 style={{ color: '#333', marginBottom: '8px' }}>No counters yet</h2>
          <p style={{ margin: '0 0 24px 0', color: '#666' }}>Create your first counter to start tracking your progress</p>
          <button
            onClick={() => setShowAddModal(true)}
            style={{
              padding: '12px 32px',
              background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
              color: 'white',
              border: 'none',
              borderRadius: '12px',
              fontSize: '16px',
              fontWeight: 'bold',
              cursor: 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '8px'
            }}
          >
            <Plus size={20} />
            Create Counter
          </button>
        </div>
      )}

      {!loading && !error && counters.length > 0 && (
        <div style={{ display: 'grid', gap: '16px' }}>
          {counters.map((counter) => (
            <div
              key={counter.id}
              style={{
                background: 'white',
                borderRadius: '16px',
                padding: '20px',
                display: 'flex',
                alignItems: 'center',
                gap: '20px',
                boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                transition: 'transform 0.2s, box-shadow 0.2s'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = '0 6px 16px rgba(0,0,0,0.15)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.1)';
              }}
            >
              <div style={{
                width: '80px',
                height: '80px',
                borderRadius: '12px',
                background: '#f8f8f8',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '48px',
                flexShrink: 0
              }}>
                {counter.icon || '⏱️'}
              </div>

              <div style={{ flex: 1 }}>
                <h3 style={{ margin: '0 0 8px 0', fontSize: '20px', color: '#333' }}>{counter.name}</h3>
                <div style={{ fontSize: '28px', fontWeight: 'bold', color: '#ff6b35', fontFamily: 'monospace' }}>
                  {formatTime(currentTimes[counter.id])}
                </div>
                <div style={{ fontSize: '14px', color: '#666', marginTop: '4px' }}>
                  Days :: Hours :: Minutes :: Seconds without {counter.name}
                </div>
              </div>

              <div style={{ display: 'flex', gap: '8px', flexShrink: 0 }}>
                <button
                  onClick={() => handleResetCounter(counter.id)}
                  style={{
                    padding: '12px 20px',
                    background: '#fff',
                    border: '2px solid #ff8c42',
                    borderRadius: '10px',
                    color: '#ff8c42',
                    fontSize: '14px',
                    fontWeight: '600',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    transition: 'all 0.2s'
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.background = '#ff8c42';
                    e.currentTarget.style.color = 'white';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.background = '#fff';
                    e.currentTarget.style.color = '#ff8c42';
                  }}
                >
                  <RotateCcw size={16} />
                  Reset
                </button>
                <button
                  onClick={() => handleDeleteCounter(counter.id)}
                  style={{
                    padding: '12px 20px',
                    background: '#fff',
                    border: '2px solid #dc3545',
                    borderRadius: '10px',
                    color: '#dc3545',
                    fontSize: '14px',
                    fontWeight: '600',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    transition: 'all 0.2s'
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.background = '#dc3545';
                    e.currentTarget.style.color = 'white';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.background = '#fff';
                    e.currentTarget.style.color = '#dc3545';
                  }}
                >
                  <Trash2 size={16} />
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Counter Modal */}
      {showAddModal && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '20px'
          }}
          onClick={(e) => {
            // Close modal when clicking outside
            if (e.target === e.currentTarget) {
              setShowAddModal(false);
              resetForm();
            }
          }}
        >
          <div style={{
            background: 'white',
            borderRadius: '16px',
            padding: '32px',
            maxWidth: '500px',
            width: '100%',
            maxHeight: '90vh',
            overflow: 'auto',
            position: 'relative'
          }}
          onClick={(e) => e.stopPropagation()}
          >
            <button
              onClick={() => {
                setShowAddModal(false);
                resetForm();
              }}
              style={{
                position: 'absolute',
                top: '16px',
                right: '16px',
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                padding: '8px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                borderRadius: '50%',
                transition: 'background 0.2s'
              }}
              onMouseEnter={(e) => e.currentTarget.style.background = '#f0f0f0'}
              onMouseLeave={(e) => e.currentTarget.style.background = 'none'}
            >
              <X size={24} color="#666" />
            </button>

            <h2 style={{ margin: '0 0 24px 0', color: '#ff6b35', fontSize: '28px' }}>Add New Counter</h2>

            <div style={{ marginBottom: '24px' }}>
              <label style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                cursor: 'pointer',
                fontSize: '16px',
                fontWeight: '600'
              }}>
                <input
                  type="checkbox"
                  checked={useDefaults}
                  onChange={(e) => {
                    setUseDefaults(e.target.checked);
                    setShowEmojiPicker(false);
                    if (e.target.checked) {
                      setNewCounter({ ...newCounter, name: '' });
                    } else {
                      setSelectedDefault('');
                    }
                  }}
                  style={{ width: '20px', height: '20px', cursor: 'pointer' }}
                />
                Select from defaults
              </label>
            </div>

            {useDefaults ? (
              <div style={{ marginBottom: '24px' }}>
                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                  Choose a counter
                </label>
                    <div style={{
                      fontSize: '13px',
                      color: '#666',
                      marginTop: '-4px',
                      marginBottom: '8px',
                      fontWeight: '400'
                    }}>
                      I want to get rid off...
                    </div>
                <select
                  value={selectedDefault}
                  onChange={(e) => setSelectedDefault(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    fontSize: '16px',
                    borderRadius: '12px',
                    border: '2px solid #e0e0e0',
                    outline: 'none',
                    transition: 'border-color 0.2s',
                    backgroundColor: 'white'
                  }}
                  onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
                  onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
                >
                  <option value="">Select a counter</option>
                  {DEFAULT_COUNTERS.map((counter) => (
                    <option key={counter.name} value={counter.name}>
                      {counter.icon} {counter.name}
                    </option>
                  ))}
                </select>
              </div>
            ) : (
              <>
                <div style={{ marginBottom: '24px' }}>
                  <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                      Counter name
                    </label>
                    <div style={{
                      fontSize: '13px',
                      color: '#666',
                      marginTop: '-4px',
                      marginBottom: '8px',
                      fontWeight: '400'
                    }}>
                      I want to get rid off...
                    </div>
                  <input
                    type="text"
                    value={newCounter.name}
                    onChange={(e) => setNewCounter({ ...newCounter, name: e.target.value })}
                    placeholder="Type the name of the counter to track"
                    style={{
                      width: '100%',
                      padding: '12px 16px',
                      fontSize: '16px',
                      borderRadius: '12px',
                      border: '2px solid #e0e0e0',
                      outline: 'none',
                      transition: 'border-color 0.2s',
                      boxSizing: 'border-box'
                    }}
                    onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
                    onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
                  />
                </div>

                <div style={{ marginBottom: '24px' }}>
                  <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                    Choose an emoji
                  </label>
                  <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                    <div
                      style={{
                        width: '60px',
                        height: '60px',
                        borderRadius: '12px',
                        border: '2px solid #e0e0e0',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '36px',
                        background: '#f8f8f8',
                        flexShrink: 0
                      }}
                    >
                      {newCounter.icon || '❓'}
                    </div>
                    <button
                      type="button"
                      onClick={() => setShowEmojiPicker(!showEmojiPicker)}
                      style={{
                        flex: 1,
                        padding: '12px 16px',
                        background: showEmojiPicker ? '#fff5eb' : '#f8f8f8',
                        border: `2px solid ${showEmojiPicker ? '#ff6b35' : '#e0e0e0'}`,
                        borderRadius: '12px',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '8px',
                        fontWeight: '600',
                        color: showEmojiPicker ? '#ff6b35' : '#666',
                        fontSize: '16px',
                        transition: 'all 0.2s'
                      }}
                      onMouseEnter={(e) => {
                        if (!showEmojiPicker) {
                          e.currentTarget.style.borderColor = '#ff6b35';
                          e.currentTarget.style.color = '#ff6b35';
                        }
                      }}
                      onMouseLeave={(e) => {
                        if (!showEmojiPicker) {
                          e.currentTarget.style.borderColor = '#e0e0e0';
                          e.currentTarget.style.color = '#666';
                        }
                      }}
                    >
                      <Smile size={20} />
                      {showEmojiPicker ? 'Close Picker' : (newCounter.icon ? 'Change Emoji' : 'Add Custom Emoji')}
                    </button>
                  </div>
                </div>

                {showEmojiPicker && (
                  <div style={{
                    marginBottom: '24px',
                    border: '2px solid #ff6b35',
                    borderRadius: '12px',
                    overflow: 'hidden',
                    boxShadow: '0 4px 12px rgba(255, 107, 53, 0.2)'
                  }}>
                    <EmojiPicker
                      onEmojiClick={handleEmojiClick}
                      width="100%"
                      height={320}
                      searchPlaceHolder="Search emoji..."
                      previewConfig={{ showPreview: false }}
                    />
                  </div>
                )}
              </>
            )}

            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                Description (optional)
              </label>
              <textarea
                value={newCounter.description}
                onChange={(e) => setNewCounter({ ...newCounter, description: e.target.value })}
                placeholder="Add a description..."
                rows={3}
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  fontSize: '16px',
                  borderRadius: '12px',
                  border: '2px solid #e0e0e0',
                  outline: 'none',
                  transition: 'border-color 0.2s',
                  resize: 'vertical',
                  fontFamily: 'inherit',
                  boxSizing: 'border-box'
                }}
                onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
                onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
              />
            </div>

            <div style={{ display: 'flex', gap: '12px', marginTop: '32px' }}>
              <button
                onClick={() => {
                  setShowAddModal(false);
                  resetForm();
                }}
                style={{
                  flex: 1,
                  padding: '14px',
                  background: '#f0f0f0',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '16px',
                  fontWeight: 'bold',
                  color: '#666',
                  cursor: 'pointer',
                  transition: 'background 0.2s'
                }}
                onMouseEnter={(e) => e.currentTarget.style.background = '#e0e0e0'}
                onMouseLeave={(e) => e.currentTarget.style.background = '#f0f0f0'}
              >
                Cancel
              </button>
              <button
                onClick={handleAddCounter}
                style={{
                  flex: 1,
                  padding: '14px',
                  background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '16px',
                  fontWeight: 'bold',
                  color: 'white',
                  cursor: 'pointer',
                  boxShadow: '0 4px 12px rgba(255, 107, 53, 0.3)',
                  transition: 'transform 0.2s, box-shadow 0.2s'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = 'translateY(-2px)';
                  e.currentTarget.style.boxShadow = '0 6px 16px rgba(255, 107, 53, 0.4)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = 'translateY(0)';
                  e.currentTarget.style.boxShadow = '0 4px 12px rgba(255, 107, 53, 0.3)';
                }}
              >
                Create Counter
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Counters;