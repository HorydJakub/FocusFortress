import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';
import { ChevronRight, CheckCircle, User, Smile } from 'lucide-react';

const OnboardingPage = () => {
  const [profileData, setProfileData] = useState({
    name: '',
    email: '',
    dateOfBirth: '',
    gender: ''
  });

  const [availableInterests, setAvailableInterests] = useState([]);
  const [selectedInterests, setSelectedInterests] = useState([]);

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const navigate = useNavigate();

  const MIN_REQUIRED_INTERESTS = 3;
  const MAX_ALLOWED_INTERESTS = 7;

  useEffect(() => {
    const fetchData = async () => {
      try {
        // 1. Fetch User Profile (to get name from Google)
        const profileRes = await api.get('/user/profile');
        const profile = profileRes.data;

        // 2. Fetch Available Interests
        const interestsRes = await api.get('/interests/subcategories');

        setProfileData({
          name: profile.name || '',
          email: profile.email || '',
          dateOfBirth: profile.dateOfBirth || '',
          gender: profile.gender || ''
        });

        setAvailableInterests(interestsRes.data || []);
      } catch (e) {
        console.error('Failed to load onboarding data', e);
        setError('Could not load profile data. Please refresh the page.');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const handleProfileChange = (e) => {
    setProfileData({
      ...profileData,
      [e.target.name]: e.target.value
    });
  };

  const toggleInterest = (interestName) => {
    if (selectedInterests.includes(interestName)) {
      setSelectedInterests(prev => prev.filter(i => i !== interestName));
    } else {
      if (selectedInterests.length >= MAX_ALLOWED_INTERESTS) return;
      setSelectedInterests(prev => [...prev, interestName]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (selectedInterests.length < MIN_REQUIRED_INTERESTS) {
      setError(`Please select at least ${MIN_REQUIRED_INTERESTS} interests.`);
      return;
    }

    if (!profileData.name.trim()) {
      setError('Name is required.');
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      // Step 1: Update Profile Details
      await api.put('/user/profile', {
        name: profileData.name,
        email: profileData.email,
        dateOfBirth: profileData.dateOfBirth || null, // Send null if empty
        gender: profileData.gender || null
      });

      // Step 2: Save Interests
      await api.put('/user/interests', {
        add: selectedInterests,
        remove: [] // New user, nothing to remove
      });

      // Step 3: Redirect to Dashboard
      navigate('/dashboard');
    } catch (e) {
      console.error('Onboarding failed', e);
      setError('Failed to save your profile. Please try again.');
      setSubmitting(false);
    }
  };

  if (loading) return (
    <div style={{ padding: '40px', textAlign: 'center', color: '#666', fontSize: '18px' }}>
      Loading setup...
    </div>
  );

  const inputStyle = {
    width: '100%',
    height: '50px',
    padding: '0 16px',
    fontSize: '16px',
    borderRadius: '12px',
    border: '2px solid #e0e0e0',
    outline: 'none',
    boxSizing: 'border-box',
    transition: 'border-color 0.2s',
    display: 'flex',
    alignItems: 'center',
    backgroundColor: 'white'
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #fff5eb 0%, #ffe8d6 100%)',
      padding: '40px 20px'
    }}>
      <div style={{
        maxWidth: '800px',
        width: '100%',
        background: 'white',
        borderRadius: '24px',
        boxShadow: '0 20px 60px rgba(255, 107, 53, 0.15)',
        padding: '40px',
        boxSizing: 'border-box'
      }}>
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <h1 style={{ color: '#ff6b35', fontSize: '36px', marginBottom: '12px', fontWeight: 'bold' }}>
            Complete Your Profile
          </h1>
          <p style={{ color: '#666', fontSize: '18px', margin: 0 }}>
            Let's personalize your FocusFortress experience.
          </p>
        </div>

        {error && (
          <div style={{
            padding: '16px',
            background: '#ffebee',
            color: '#c62828',
            borderRadius: '12px',
            marginBottom: '24px',
            textAlign: 'center',
            fontWeight: '500',
            border: '1px solid #ffcdd2'
          }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {/* --- PERSONAL DETAILS SECTION --- */}
          <div style={{ marginBottom: '32px', paddingBottom: '32px', borderBottom: '1px solid #f0f0f0' }}>
            <h3 style={{ fontSize: '20px', color: '#333', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '10px' }}>
              <User size={20} color="#ff6b35" /> Personal Details
            </h3>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
              {/* Name */}
              <div style={{ gridColumn: '1 / -1' }}>
                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#444' }}>
                  Name <span style={{ color: '#dc3545' }}>*</span>
                </label>
                <input
                  type="text"
                  name="name"
                  value={profileData.name}
                  onChange={handleProfileChange}
                  placeholder="Your display name"
                  style={inputStyle}
                  onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
                  onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
                />
              </div>

              {/* Date of Birth */}
              <div>
                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#444' }}>
                  Date of Birth <span style={{ fontWeight: 'normal', color: '#999', fontSize: '13px' }}>(Optional)</span>
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    type="date"
                    name="dateOfBirth"
                    value={profileData.dateOfBirth}
                    onChange={handleProfileChange}
                    style={inputStyle}
                    onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
                    onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
                  />
                </div>
              </div>

              {/* Gender */}
              <div>
                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#444' }}>
                  Gender <span style={{ fontWeight: 'normal', color: '#999', fontSize: '13px' }}>(Optional)</span>
                </label>
                <div style={{ position: 'relative' }}>
                  <select
                    name="gender"
                    value={profileData.gender}
                    onChange={handleProfileChange}
                    style={{
                      ...inputStyle,
                      appearance: 'none',
                      cursor: 'pointer'
                    }}
                    onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
                    onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
                  >
                    <option value="">Select gender</option>
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                    <option value="Prefer not to specify">Prefer not to specify</option>
                  </select>
                  <div style={{ position: 'absolute', right: '16px', top: '50%', transform: 'translateY(-50%)', pointerEvents: 'none' }}>
                    <Smile size={18} color="#999" />
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* --- INTERESTS SECTION --- */}
          <div style={{ marginBottom: '32px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3 style={{ fontSize: '20px', color: '#333', margin: 0, display: 'flex', alignItems: 'center', gap: '10px' }}>
                <CheckCircle size={20} color="#ff6b35" /> Select Interests
              </h3>
              <span style={{
                fontWeight: 'bold',
                color: selectedInterests.length >= MIN_REQUIRED_INTERESTS ? '#4caf50' : '#ff9800',
                background: selectedInterests.length >= MIN_REQUIRED_INTERESTS ? '#e8f5e9' : '#fff3e0',
                padding: '6px 14px',
                borderRadius: '20px',
                fontSize: '14px',
                border: `1px solid ${selectedInterests.length >= MIN_REQUIRED_INTERESTS ? '#c8e6c9' : '#ffe0b2'}`
              }}>
                {selectedInterests.length} / {MAX_ALLOWED_INTERESTS} selected
              </span>
            </div>

            <div style={{
              display: 'flex',
              flexWrap: 'wrap',
              gap: '12px',
              maxHeight: '300px',
              overflowY: 'auto',
              padding: '20px',
              border: '2px solid #f0f0f0',
              borderRadius: '16px',
              background: '#fafafa'
            }}>
              {availableInterests.map((interest) => {
                const isSelected = selectedInterests.includes(interest.name);
                return (
                  <button
                    key={interest.name}
                    type="button"
                    onClick={() => toggleInterest(interest.name)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      padding: '10px 18px',
                      borderRadius: '24px',
                      border: `2px solid ${isSelected ? '#ff6b35' : 'transparent'}`,
                      background: isSelected ? 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)' : 'white',
                      color: isSelected ? 'white' : '#555',
                      fontWeight: isSelected ? '600' : '500',
                      fontSize: '14px',
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                      boxShadow: isSelected ? '0 4px 12px rgba(255, 107, 53, 0.3)' : '0 2px 6px rgba(0,0,0,0.05)'
                    }}
                  >
                    <span style={{ fontSize: '18px' }}>{interest.icon}</span>
                    <span>{interest.name}</span>
                    {isSelected && <CheckCircle size={16} />}
                  </button>
                );
              })}
            </div>
            {selectedInterests.length < MIN_REQUIRED_INTERESTS && (
              <p style={{ color: '#999', fontSize: '14px', marginTop: '10px', fontStyle: 'italic' }}>
                * Please select at least {MIN_REQUIRED_INTERESTS - selectedInterests.length} more topic(s)
              </p>
            )}
          </div>

          <button
            type="submit"
            disabled={submitting || selectedInterests.length < MIN_REQUIRED_INTERESTS}
            style={{
              width: '100%',
              padding: '16px',
              background: (submitting || selectedInterests.length < MIN_REQUIRED_INTERESTS)
                ? '#ccc'
                : 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
              color: 'white',
              border: 'none',
              borderRadius: '12px',
              fontSize: '18px',
              fontWeight: 'bold',
              cursor: (submitting || selectedInterests.length < MIN_REQUIRED_INTERESTS) ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '10px',
              transition: 'transform 0.2s, box-shadow 0.2s',
              boxShadow: (submitting || selectedInterests.length < MIN_REQUIRED_INTERESTS) ? 'none' : '0 6px 20px rgba(255, 107, 53, 0.3)'
            }}
            onMouseEnter={(e) => {
              if (!submitting && selectedInterests.length >= MIN_REQUIRED_INTERESTS) {
                e.currentTarget.style.transform = 'translateY(-2px)';
              }
            }}
            onMouseLeave={(e) => {
              if (!submitting && selectedInterests.length >= MIN_REQUIRED_INTERESTS) {
                e.currentTarget.style.transform = 'translateY(0)';
              }
            }}
          >
            {submitting ? 'Setting up your account...' : 'Complete Setup'}
            {!submitting && <ChevronRight size={20} />}
          </button>
        </form>
      </div>
    </div>
  );
};

export default OnboardingPage;