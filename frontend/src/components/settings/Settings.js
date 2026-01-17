import React, { useState, useEffect } from 'react';
import { X, AlertCircle, ChevronRight } from 'lucide-react';
import authService from '../../services/authService';
import EmojiPicker from 'emoji-picker-react';

const Settings = ({ isOpen, onClose }) => {
  const [activeTab, setActiveTab] = useState('profile');
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [errors, setErrors] = useState({});

  // Profile form state
  const [profileData, setProfileData] = useState({
    name: '',
    email: '',
    dateOfBirth: '',
    gender: ''
  });

  // Interests state
  const [availableInterests, setAvailableInterests] = useState([]);
  const [userInterests, setUserInterests] = useState([]);
  const [customInterests, setCustomInterests] = useState([]); // Track custom interests separately
  const [customInterest, setCustomInterest] = useState({
    name: '',
    emoji: '⭐'
  });
  const [warningAccepted, setWarningAccepted] = useState(false);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);

  // Report settings state
  const [reportSettings, setReportSettings] = useState({
    includeActiveHabits: true,
    includeCompletedHabits: true,
    includeInterests: true,
    includeMediaLibrary: true,
    includeCounters: true,
    automaticReports: false,
    reportFrequency: 'weekly' // 'weekly' or 'monthly'
  });

  const MAX_TOTAL_INTERESTS = 7;

  // Fetch user profile data on component mount
  useEffect(() => {
    if (isOpen) {
      fetchUserProfile();
      fetchInterests();
      fetchReportSettings();
    }
  }, [isOpen]);

  const fetchUserProfile = async () => {
    try {
      const profile = await authService.getUserProfile();
      setProfileData({
        name: profile.name || '',
        email: profile.email || '',
        dateOfBirth: profile.dateOfBirth || '',
        gender: profile.gender || ''
      });
      setMessage('');
    } catch (error) {
      console.error('Failed to fetch profile:', error);
      setMessage('Failed to load profile data');
    }
  };

  const fetchInterests = async () => {
    try {
      // Fetch available interests
      const response = await fetch('http://localhost:8080/api/interests/subcategories');
      if (response.ok) {
        const availableData = await response.json();
        setAvailableInterests(Array.isArray(availableData) ? availableData : []);
      }

      // Fetch user's current interests
      const userInterestsData = await authService.getUserInterests();

      // DEBUG: Log the raw data from backend
      console.log('🔍 Raw user interests data:', userInterestsData);
      console.log('🔍 Each interest:', userInterestsData.map(i => ({
        name: i.subcategoryName,
        category: i.categoryName,
        isCustom: i.isCustom
      })));

      // Separate regular interests from custom interests
      const regularInterests = userInterestsData
        .filter(i => !i.isCustom)
        .map(i => i.subcategoryName);

      const customInterestsData = userInterestsData
        .filter(i => i.isCustom);

      console.log('📋 Regular interests:', regularInterests);
      console.log('✨ Custom interests:', customInterestsData);

      setUserInterests(regularInterests);
      setCustomInterests(customInterestsData);
    } catch (error) {
      console.error('Failed to fetch interests:', error);
      setAvailableInterests([]);
      setUserInterests([]);
      setCustomInterests([]);
    }
  };

  const handleProfileChange = (e) => {
    setProfileData({
      ...profileData,
      [e.target.name]: e.target.value
    });
    if (errors[e.target.name]) {
      setErrors({
        ...errors,
        [e.target.name]: ''
      });
    }
  };

  const validateProfileForm = () => {
    const newErrors = {};

    if (!profileData.name || profileData.name.length < 2) {
      newErrors.name = 'Name must be at least 2 characters';
    }
    if (!profileData.email || !profileData.email.includes('@')) {
      newErrors.email = 'Valid email is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setMessage('');

    if (!validateProfileForm()) {
      return;
    }

    setIsLoading(true);
    try {
      await authService.updateUserProfile(profileData);
      setMessage('✅ Profile updated successfully!');
      window.dispatchEvent(new Event('profileUpdated'));

      setTimeout(() => setMessage(''), 3000);
    } catch (error) {
      setMessage(error || 'Failed to update profile');
    } finally {
      setIsLoading(false);
    }
  };

  const handleEmojiClick = (emojiData) => {
    setCustomInterest({ ...customInterest, emoji: emojiData.emoji });
    setShowEmojiPicker(false);
  };

  const toggleInterest = (interestName) => {
    setUserInterests(prevInterests => {
      const isCurrentlySelected = prevInterests.includes(interestName);
      const totalSelected = prevInterests.length + customInterests.length;

      // If trying to add and already at max, prevent it
      if (!isCurrentlySelected && totalSelected >= MAX_TOTAL_INTERESTS) {
        setMessage(`⚠️ Maximum ${MAX_TOTAL_INTERESTS} interests allowed. Remove an interest first.`);
        setTimeout(() => setMessage(''), 4000);
        return prevInterests;
      }

      // Toggle the interest
      return isCurrentlySelected
        ? prevInterests.filter(i => i !== interestName)
        : [...prevInterests, interestName];
    });
  };

  const handleSaveInterests = async () => {
    setMessage('');
    setIsLoading(true);

    try {
      // Get current interests from API first
      const currentData = await authService.getUserInterests();
      const currentInterests = currentData
        .filter(i => !i.isCustom)
        .map(i => i.subcategoryName);

      // Calculate additions and removals
      const toAdd = userInterests.filter(i => !currentInterests.includes(i));
      const toRemove = currentInterests.filter(i => !userInterests.includes(i));

      // Send update
      await authService.manageUserInterests({
        add: toAdd,
        remove: toRemove
      });

      setMessage('✅ Interests updated successfully!');

      // Refresh media tiles by triggering a refresh event
      window.dispatchEvent(new CustomEvent('refreshMediaTiles'));

      setTimeout(() => setMessage(''), 3000);
    } catch (error) {
      setMessage(error || 'Failed to update interests');
    } finally {
      setIsLoading(false);
    }
  };

  const handleAddCustomInterest = async (e) => {
    e.preventDefault();
    setMessage('');

    if (!customInterest.name.trim()) {
      setErrors({ ...errors, customInterest: 'Please enter an interest name' });
      return;
    }

    if (!warningAccepted) {
      setErrors({ ...errors, customInterest: 'Please accept the warning to continue' });
      return;
    }

    // Check if adding this custom interest would exceed the limit
    const totalSelected = userInterests.length + customInterests.length;
    if (totalSelected >= MAX_TOTAL_INTERESTS) {
      setMessage(`⚠️ Maximum ${MAX_TOTAL_INTERESTS} interests allowed. You have ${userInterests.length} regular and ${customInterests.length} custom interests selected.`);
      setTimeout(() => setMessage(''), 5000);
      return;
    }

    setIsLoading(true);
    try {
      // Use the default star emoji if no emoji is selected
      const emojiToSend = customInterest.emoji || '⭐';
      await authService.addCustomInterest({ ...customInterest, emoji: emojiToSend });
      setMessage('✅ Custom interest added successfully!');
      setCustomInterest({ name: '', emoji: '⭐' });
      setWarningAccepted(false);
      setShowEmojiPicker(false);
      await fetchInterests(); // Refresh interests list

      // Refresh media tiles
      window.dispatchEvent(new CustomEvent('refreshMediaTiles'));

      setTimeout(() => setMessage(''), 3000);
    } catch (error) {
      setMessage(error || 'Failed to add custom interest');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteCustomInterest = async (interestId) => {
    if (!window.confirm('Are you sure you want to delete this custom interest?')) {
      return;
    }

    setIsLoading(true);
    try {
      await authService.deleteCustomInterest(interestId);
      setMessage('✅ Custom interest deleted successfully!');
      await fetchInterests();

      // Refresh media tiles
      window.dispatchEvent(new CustomEvent('refreshMediaTiles'));

      setTimeout(() => setMessage(''), 3000);
    } catch (error) {
      setMessage(error || 'Failed to delete custom interest');
    } finally {
      setIsLoading(false);
    }
  };

  // Report functions
  const fetchReportSettings = async () => {
    try {
      const settings = await authService.getReportSettings();
      if (settings) {
        setReportSettings(settings);
      }
    } catch (error) {
      console.error('Failed to fetch report settings:', error);
      // Keep default settings if fetch fails
    }
  };

  const handleReportSettingChange = (setting) => {
    setReportSettings(prev => ({
      ...prev,
      [setting]: !prev[setting]
    }));
  };

  const handleFrequencyChange = (frequency) => {
    setReportSettings(prev => ({
      ...prev,
      reportFrequency: frequency
    }));
  };

  const handleSaveReportSettings = async () => {
    setMessage('');
    setIsLoading(true);
    try {
      await authService.saveReportSettings(reportSettings);
      setMessage('✅ Report settings saved successfully!');
      setTimeout(() => setMessage(''), 3000);
    } catch (error) {
      setMessage(error || 'Failed to save report settings');
    } finally {
      setIsLoading(false);
    }
  };

  const handleGenerateReport = async () => {
    setMessage('');
    setIsLoading(true);
    try {
      await authService.generateReport(reportSettings);
      setMessage('✅ Report generated and sent to your email!');
      setTimeout(() => setMessage(''), 3000);
    } catch (error) {
      setMessage(error || 'Failed to generate report');
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) return null;

  const totalSelected = userInterests.length + customInterests.length;

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: 'rgba(0, 0, 0, 0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000,
      padding: '20px'
    }}>
      <div style={{
        background: 'white',
        borderRadius: '24px',
        boxShadow: '0 20px 60px rgba(255, 107, 53, 0.15)',
        width: '100%',
        maxWidth: '700px',
        maxHeight: '90vh',
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column'
      }}>
        {/* Header */}
        <div style={{
          background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
          color: 'white',
          padding: '24px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <h2 style={{ fontSize: '24px', fontWeight: 'bold', margin: 0 }}>Settings</h2>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              color: 'white',
              cursor: 'pointer',
              fontSize: '24px',
              padding: 0,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <X size={24} />
          </button>
        </div>

        {/* Tabs */}
        <div style={{
          display: 'flex',
          borderBottom: '2px solid #e0e0e0',
          background: '#f8f8f8'
        }}>
          <button
            onClick={() => setActiveTab('profile')}
            style={{
              flex: 1,
              padding: '16px',
              border: 'none',
              background: activeTab === 'profile' ? 'white' : 'transparent',
              color: activeTab === 'profile' ? '#ff6b35' : '#999',
              fontWeight: activeTab === 'profile' ? '600' : '500',
              fontSize: '16px',
              cursor: 'pointer',
              borderBottom: activeTab === 'profile' ? '3px solid #ff6b35' : 'none',
              transition: 'all 0.2s'
            }}
          >
            Profile
          </button>
          <button
            onClick={() => setActiveTab('interests')}
            style={{
              flex: 1,
              padding: '16px',
              border: 'none',
              background: activeTab === 'interests' ? 'white' : 'transparent',
              color: activeTab === 'interests' ? '#ff6b35' : '#999',
              fontWeight: activeTab === 'interests' ? '600' : '500',
              fontSize: '16px',
              cursor: 'pointer',
              borderBottom: activeTab === 'interests' ? '3px solid #ff6b35' : 'none',
              transition: 'all 0.2s'
            }}
          >
            Interests
          </button>
          <button
            onClick={() => setActiveTab('custom')}
            style={{
              flex: 1,
              padding: '16px',
              border: 'none',
              background: activeTab === 'custom' ? 'white' : 'transparent',
              color: activeTab === 'custom' ? '#ff6b35' : '#999',
              fontWeight: activeTab === 'custom' ? '600' : '500',
              fontSize: '16px',
              cursor: 'pointer',
              borderBottom: activeTab === 'custom' ? '3px solid #ff6b35' : 'none',
              transition: 'all 0.2s'
            }}
          >
            Custom Interests
          </button>
          <button
            onClick={() => setActiveTab('report')}
            style={{
              flex: 1,
              padding: '16px',
              border: 'none',
              background: activeTab === 'report' ? 'white' : 'transparent',
              color: activeTab === 'report' ? '#ff6b35' : '#999',
              fontWeight: activeTab === 'report' ? '600' : '500',
              fontSize: '16px',
              cursor: 'pointer',
              borderBottom: activeTab === 'report' ? '3px solid #ff6b35' : 'none',
              transition: 'all 0.2s'
            }}
          >
            Report
          </button>
        </div>

        {/* Content */}
        <div style={{
          flex: 1,
          overflowY: 'auto',
          padding: '32px 24px'
        }}>
          {message && (
            <div style={{
              padding: '16px',
              borderRadius: '12px',
              marginBottom: '24px',
              display: 'flex',
              alignItems: 'center',
              backgroundColor: message.includes('✅') ? '#d4edda' : message.includes('⚠️') ? '#fff3cd' : '#f8d7da',
              color: message.includes('✅') ? '#155724' : message.includes('⚠️') ? '#856404' : '#721c24',
              border: `2px solid ${message.includes('✅') ? '#c3e6cb' : message.includes('⚠️') ? '#ffeaa7' : '#f5c6cb'}`
            }}>
              <AlertCircle size={20} style={{ marginRight: '12px', flexShrink: 0 }} />
              <span>{message}</span>
            </div>
          )}

          {activeTab === 'profile' && (
            <form onSubmit={handleSaveProfile}>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                  Name
                </label>
                <input
                  type="text"
                  name="name"
                  value={profileData.name}
                  onChange={handleProfileChange}
                  disabled={isLoading}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    fontSize: '16px',
                    borderRadius: '12px',
                    border: `2px solid ${errors.name ? '#dc3545' : '#e0e0e0'}`,
                    outline: 'none',
                    transition: 'border-color 0.2s',
                    opacity: isLoading ? 0.6 : 1,
                    boxSizing: 'border-box'
                  }}
                  onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
                  onBlur={(e) => e.target.style.borderColor = errors.name ? '#dc3545' : '#e0e0e0'}
                />
                {errors.name && <div style={{ color: '#dc3545', fontSize: '14px', marginTop: '4px' }}>{errors.name}</div>}
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                  Email
                </label>
                <input
                  type="email"
                  name="email"
                  value={profileData.email}
                  onChange={handleProfileChange}
                  disabled={isLoading}
                  readOnly
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    fontSize: '16px',
                    borderRadius: '12px',
                    border: `2px solid ${errors.email ? '#dc3545' : '#e0e0e0'}`,
                    outline: 'none',
                    transition: 'border-color 0.2s',
                    opacity: 0.7,
                    backgroundColor: '#f9f9f9',
                    boxSizing: 'border-box'
                  }}
                />
                {errors.email && <div style={{ color: '#dc3545', fontSize: '14px', marginTop: '4px' }}>{errors.email}</div>}
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                  Date of Birth
                </label>
                <input
                  type="date"
                  name="dateOfBirth"
                  value={profileData.dateOfBirth || ''}
                  onChange={handleProfileChange}
                  disabled={isLoading}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    fontSize: '16px',
                    borderRadius: '12px',
                    border: `2px solid ${errors.dateOfBirth ? '#dc3545' : '#e0e0e0'}`,
                    outline: 'none',
                    transition: 'border-color 0.2s',
                    opacity: isLoading ? 0.6 : 1,
                    boxSizing: 'border-box'
                  }}
                  onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
                  onBlur={(e) => e.target.style.borderColor = errors.dateOfBirth ? '#dc3545' : '#e0e0e0'}
                />
                {errors.dateOfBirth && <div style={{ color: '#dc3545', fontSize: '14px', marginTop: '4px' }}>{errors.dateOfBirth}</div>}
              </div>

              <div style={{ marginBottom: '24px' }}>
                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                  Gender
                </label>
                <select
                  name="gender"
                  value={profileData.gender || ''}
                  onChange={handleProfileChange}
                  disabled={isLoading}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    fontSize: '16px',
                    borderRadius: '12px',
                    border: '2px solid #e0e0e0',
                    outline: 'none',
                    transition: 'border-color 0.2s',
                    backgroundColor: 'white',
                    opacity: isLoading ? 0.6 : 1,
                    boxSizing: 'border-box'
                  }}
                  onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
                  onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
                >
                  <option value="">Select gender</option>
                  <option value="Male">Male</option>
                  <option value="Female">Female</option>
                  <option value="Prefer not to specify">Prefer not to specify</option>
                </select>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                style={{
                  width: '100%',
                  padding: '14px',
                  fontSize: '16px',
                  fontWeight: 'bold',
                  color: 'white',
                  background: isLoading ? '#ccc' : 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
                  border: 'none',
                  borderRadius: '12px',
                  cursor: isLoading ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxShadow: isLoading ? 'none' : '0 4px 12px rgba(255, 107, 53, 0.3)',
                  transition: 'transform 0.2s, box-shadow 0.2s',
                  opacity: isLoading ? 0.6 : 1,
                  boxSizing: 'border-box'
                }}
                onMouseEnter={(e) => {
                  if (!isLoading) {
                    e.currentTarget.style.transform = 'translateY(-2px)';
                    e.currentTarget.style.boxShadow = '0 6px 16px rgba(255, 107, 53, 0.4)';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!isLoading) {
                    e.currentTarget.style.transform = 'translateY(0)';
                    e.currentTarget.style.boxShadow = '0 4px 12px rgba(255, 107, 53, 0.3)';
                  }
                }}
              >
                {isLoading ? 'Saving...' : 'Save Changes'}
              </button>
            </form>
          )}

          {activeTab === 'interests' && (
            <div>
              {/* Interest Counter Banner */}
              <div style={{
                background: totalSelected >= MAX_TOTAL_INTERESTS
                  ? 'linear-gradient(135deg, #ff6b35 0%, #e55a2b 100%)'
                  : 'linear-gradient(135deg, #ffb366 0%, #ff8c42 100%)',
                color: 'white',
                padding: '16px 20px',
                borderRadius: '16px',
                marginBottom: '24px',
                boxShadow: '0 4px 12px rgba(255, 107, 53, 0.2)'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                  <span style={{ fontSize: '18px', fontWeight: '700' }}>
                    Total Interests: {totalSelected} / {MAX_TOTAL_INTERESTS}
                  </span>
                  <span style={{ fontSize: '32px' }}>
                    {totalSelected >= MAX_TOTAL_INTERESTS ? '🔒' : '✨'}
                  </span>
                </div>
                <div style={{ fontSize: '13px', opacity: 0.95, display: 'flex', gap: '16px' }}>
                  <span>📋 Regular: {userInterests.length}</span>
                  <span>✨ Custom: {customInterests.length}</span>
                </div>
                {totalSelected >= MAX_TOTAL_INTERESTS && (
                  <div style={{
                    marginTop: '12px',
                    padding: '8px 12px',
                    background: 'rgba(255,255,255,0.2)',
                    borderRadius: '8px',
                    fontSize: '13px',
                    fontWeight: '600'
                  }}>
                    ⚠️ Maximum limit reached! Remove interests to add new ones.
                  </div>
                )}
              </div>

              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '16px'
              }}>
                <h3 style={{
                  fontSize: '20px',
                  fontWeight: '700',
                  color: '#333',
                  margin: 0,
                  letterSpacing: '-0.02em'
                }}>
                  Select Your Interests
                </h3>
              </div>

              {/* Interest Bubbles Container */}
              <div style={{
                display: 'flex',
                flexWrap: 'wrap',
                gap: '10px',
                maxHeight: '320px',
                overflowY: 'auto',
                padding: '20px',
                border: '2px solid #f0f0f0',
                borderRadius: '16px',
                background: '#fafafa',
                marginBottom: '16px'
              }}>
                {availableInterests.length === 0 ? (
                  <div style={{
                    width: '100%',
                    textAlign: 'center',
                    padding: '40px 20px',
                    color: '#999'
                  }}>
                    <div style={{ fontSize: '40px', marginBottom: '12px' }}>⏳</div>
                    <div style={{ fontSize: '15px', fontWeight: '500' }}>Loading interests...</div>
                  </div>
                ) : (
                  availableInterests.map((interest) => {
                    const isSelected = userInterests.includes(interest.name);
                    const isDisabled = !isSelected && totalSelected >= MAX_TOTAL_INTERESTS;

                    return (
                      <button
                        key={interest.name}
                        onClick={() => toggleInterest(interest.name)}
                        disabled={isLoading || isDisabled}
                        type="button"
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '6px',
                          padding: '10px 16px',
                          border: 'none',
                          borderRadius: '20px',
                          background: isSelected
                            ? 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)'
                            : isDisabled
                            ? '#e0e0e0'
                            : 'white',
                          color: isSelected ? 'white' : isDisabled ? '#999' : '#555',
                          fontSize: '14px',
                          fontWeight: isSelected ? '600' : '500',
                          cursor: (isLoading || isDisabled) ? 'not-allowed' : 'pointer',
                          transition: 'all 0.2s ease',
                          opacity: (isLoading || isDisabled) ? 0.6 : 1,
                          boxShadow: isSelected
                            ? '0 4px 12px rgba(255, 107, 53, 0.3)'
                            : '0 2px 6px rgba(0,0,0,0.08)',
                          transform: isSelected ? 'translateY(-1px)' : 'translateY(0)'
                        }}
                        onMouseEnter={(e) => {
                          if (!isLoading && !isDisabled) {
                            e.currentTarget.style.transform = 'translateY(-2px)';
                            e.currentTarget.style.boxShadow = isSelected
                              ? '0 6px 16px rgba(255, 107, 53, 0.4)'
                              : '0 4px 10px rgba(0,0,0,0.12)';
                          }
                        }}
                        onMouseLeave={(e) => {
                          if (!isLoading && !isDisabled) {
                            e.currentTarget.style.transform = isSelected ? 'translateY(-1px)' : 'translateY(0)';
                            e.currentTarget.style.boxShadow = isSelected
                              ? '0 4px 12px rgba(255, 107, 53, 0.3)'
                              : '0 2px 6px rgba(0,0,0,0.08)';
                          }
                        }}
                      >
                        <span style={{ fontSize: '18px' }}>{interest.icon}</span>
                        <span>{interest.name}</span>
                        {isSelected && <span style={{ fontSize: '14px' }}>✓</span>}
                      </button>
                    );
                  })
                )}
              </div>

              <button
                onClick={handleSaveInterests}
                disabled={isLoading}
                style={{
                  width: '100%',
                  padding: '14px',
                  fontSize: '16px',
                  fontWeight: '600',
                  color: 'white',
                  background: isLoading ? '#ccc' : 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
                  border: 'none',
                  borderRadius: '12px',
                  cursor: isLoading ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxShadow: isLoading ? 'none' : '0 4px 12px rgba(255, 107, 53, 0.3)',
                  transition: 'all 0.2s',
                  opacity: isLoading ? 0.6 : 1,
                  boxSizing: 'border-box'
                }}
                onMouseEnter={(e) => {
                  if (!isLoading) {
                    e.currentTarget.style.transform = 'translateY(-2px)';
                    e.currentTarget.style.boxShadow = '0 6px 16px rgba(255, 107, 53, 0.4)';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!isLoading) {
                    e.currentTarget.style.transform = 'translateY(0)';
                    e.currentTarget.style.boxShadow = '0 4px 12px rgba(255, 107, 53, 0.3)';
                  }
                }}
              >
                {isLoading ? 'Saving...' : 'Save Interests'}
                {!isLoading && <ChevronRight size={20} style={{ marginLeft: '8px' }} />}
              </button>
            </div>
          )}

          {activeTab === 'custom' && (
            <div>
              {/* Interest Counter Banner */}
              <div style={{
                background: totalSelected >= MAX_TOTAL_INTERESTS
                  ? 'linear-gradient(135deg, #ff6b35 0%, #e55a2b 100%)'
                  : 'linear-gradient(135deg, #ffb366 0%, #ff8c42 100%)',
                color: 'white',
                padding: '16px 20px',
                borderRadius: '16px',
                marginBottom: '24px',
                boxShadow: '0 4px 12px rgba(255, 107, 53, 0.2)'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                  <span style={{ fontSize: '18px', fontWeight: '700' }}>
                    Total Interests: {totalSelected} / {MAX_TOTAL_INTERESTS}
                  </span>
                  <span style={{ fontSize: '32px' }}>
                    {totalSelected >= MAX_TOTAL_INTERESTS ? '🔒' : '✨'}
                  </span>
                </div>
                <div style={{ fontSize: '13px', opacity: 0.95, display: 'flex', gap: '16px' }}>
                  <span>📋 Regular: {userInterests.length}</span>
                  <span>✨ Custom: {customInterests.length}</span>
                </div>
                {totalSelected >= MAX_TOTAL_INTERESTS && (
                  <div style={{
                    marginTop: '12px',
                    padding: '8px 12px',
                    background: 'rgba(255,255,255,0.2)',
                    borderRadius: '8px',
                    fontSize: '13px',
                    fontWeight: '600'
                  }}>
                    ⚠️ Maximum limit reached! Remove interests to add new ones.
                  </div>
                )}
              </div>

              {/* Existing Custom Interests */}
              {customInterests.length > 0 && (
                <div style={{ marginBottom: '32px' }}>
                  <h3 style={{
                    fontSize: '18px',
                    fontWeight: '700',
                    color: '#333',
                    marginBottom: '16px',
                    letterSpacing: '-0.02em'
                  }}>
                    Your Custom Interests
                  </h3>
                  <div style={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    gap: '10px',
                    padding: '16px',
                    border: '2px solid #ffe8d6',
                    borderRadius: '16px',
                    background: '#fff5eb'
                  }}>
                    {customInterests.map((interest) => (
                      <div
                        key={interest.id}
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '8px',
                          padding: '10px 16px',
                          borderRadius: '20px',
                          background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
                          color: 'white',
                          fontSize: '14px',
                          fontWeight: '600',
                          boxShadow: '0 4px 12px rgba(255, 107, 53, 0.3)'
                        }}
                      >
                        <span style={{ fontSize: '18px' }}>{interest.emoji || interest.subcategoryIcon}</span>
                        <span>{interest.subcategoryName}</span>
                        <button
                          onClick={() => handleDeleteCustomInterest(interest.id)}
                          disabled={isLoading}
                          style={{
                            background: 'rgba(255, 255, 255, 0.2)',
                            border: 'none',
                            color: 'white',
                            cursor: isLoading ? 'not-allowed' : 'pointer',
                            fontSize: '18px',
                            padding: '2px 6px',
                            borderRadius: '50%',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            opacity: isLoading ? 0.6 : 1,
                            transition: 'all 0.2s'
                          }}
                          onMouseEnter={(e) => {
                            if (!isLoading) {
                              e.currentTarget.style.background = 'rgba(255, 255, 255, 0.3)';
                            }
                          }}
                          onMouseLeave={(e) => {
                            if (!isLoading) {
                              e.currentTarget.style.background = 'rgba(255, 255, 255, 0.2)';
                            }
                          }}
                        >
                          ×
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Add New Custom Interest Form */}
              <div>
                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginBottom: '16px'
                }}>
                  <h3 style={{
                    fontSize: '20px',
                    fontWeight: '700',
                    color: '#333',
                    margin: 0,
                    letterSpacing: '-0.02em'
                  }}>
                    Add New Custom Interest
                  </h3>
                </div>

                <form onSubmit={handleAddCustomInterest}>
                  <div style={{
                    display: 'grid',
                    gridTemplateColumns: '1fr auto',
                    gap: '12px',
                    marginBottom: '16px'
                  }}>
                    <div>
                      <label style={{
                        display: 'block',
                        marginBottom: '8px',
                        fontWeight: '600',
                        color: '#555',
                        fontSize: '14px'
                      }}>
                        Interest Name
                      </label>
                      <input
                        type="text"
                        value={customInterest.name}
                        onChange={(e) => {
                          setCustomInterest({ ...customInterest, name: e.target.value });
                          if (errors.customInterest) setErrors({ ...errors, customInterest: '' });
                        }}
                        placeholder="e.g., Digital Art, Astronomy"
                        disabled={isLoading}
                        style={{
                          width: '100%',
                          padding: '12px 16px',
                          fontSize: '15px',
                          borderRadius: '12px',
                          border: `2px solid ${errors.customInterest ? '#dc3545' : '#e0e0e0'}`,
                          outline: 'none',
                          transition: 'border-color 0.2s',
                          opacity: isLoading ? 0.6 : 1,
                          boxSizing: 'border-box'
                        }}
                        onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
                        onBlur={(e) => e.target.style.borderColor = errors.customInterest ? '#dc3545' : '#e0e0e0'}
                      />
                    </div>

                    <div>
                      <label style={{
                        display: 'block',
                        marginBottom: '8px',
                        fontWeight: '600',
                        color: '#555',
                        fontSize: '14px'
                      }}>
                        Icon
                      </label>
                      <button
                        type="button"
                        onClick={() => setShowEmojiPicker(!showEmojiPicker)}
                        disabled={isLoading}
                        style={{
                          width: '70px',
                          height: '52px',
                          padding: '12px',
                          fontSize: '28px',
                          borderRadius: '12px',
                          border: `2px solid ${errors.customInterest ? '#dc3545' : '#e0e0e0'}`,
                          outline: 'none',
                          transition: 'all 0.2s',
                          opacity: isLoading ? 0.6 : 1,
                          textAlign: 'center',
                          boxSizing: 'border-box',
                          background: 'white',
                          cursor: isLoading ? 'not-allowed' : 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center'
                        }}
                        onMouseEnter={(e) => {
                          if (!isLoading) {
                            e.currentTarget.style.borderColor = '#ff6b35';
                            e.currentTarget.style.transform = 'scale(1.05)';
                          }
                        }}
                        onMouseLeave={(e) => {
                          if (!isLoading) {
                            e.currentTarget.style.borderColor = errors.customInterest ? '#dc3545' : '#e0e0e0';
                            e.currentTarget.style.transform = 'scale(1)';
                          }
                        }}
                      >
                        {customInterest.emoji || '⭐'}
                      </button>
                    </div>
                  </div>

                  {/* Emoji Picker Popup */}
                  {showEmojiPicker && (
                    <div style={{
                      marginBottom: '16px',
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

                  {/* Compact Warning + Checkbox */}
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '10px',
                    marginBottom: '16px'
                  }}>
                    <input
                      type="checkbox"
                      checked={warningAccepted}
                      onChange={(e) => {
                        setWarningAccepted(e.target.checked);
                        if (errors.customInterest) setErrors({ ...errors, customInterest: '' });
                      }}
                      disabled={isLoading}
                      style={{
                        cursor: isLoading ? 'not-allowed' : 'pointer',
                        width: '18px',
                        height: '18px',
                        flexShrink: 0,
                        accentColor: '#ff6b35',
                        opacity: isLoading ? 0.6 : 1
                      }}
                    />
                    <label
                      onClick={() => !isLoading && setWarningAccepted(!warningAccepted)}
                      style={{
                        fontSize: '13px',
                        color: '#666',
                        cursor: isLoading ? 'not-allowed' : 'pointer',
                        opacity: isLoading ? 0.6 : 1,
                        lineHeight: '1.5',
                        userSelect: 'none'
                      }}
                    >
                      <strong>⚠️ Content Warning:</strong> Custom interests may show unfiltered content. I understand and accept this risk.
                    </label>
                  </div>

                  {errors.customInterest && (
                    <div style={{
                      color: '#dc3545',
                      fontSize: '13px',
                      marginBottom: '12px',
                      padding: '8px 12px',
                      background: '#fee',
                      borderRadius: '8px',
                      border: '1px solid #fcc'
                    }}>
                      {errors.customInterest}
                    </div>
                  )}

                  <button
                    type="submit"
                    disabled={isLoading || !warningAccepted || !customInterest.name || totalSelected >= MAX_TOTAL_INTERESTS}
                    style={{
                      width: '100%',
                      padding: '12px',
                      fontSize: '15px',
                      fontWeight: '600',
                      color: 'white',
                      background: (isLoading || !warningAccepted || !customInterest.name || totalSelected >= MAX_TOTAL_INTERESTS)
                        ? '#ccc'
                        : 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
                      border: 'none',
                      borderRadius: '12px',
                      cursor: (isLoading || !warningAccepted || !customInterest.name || totalSelected >= MAX_TOTAL_INTERESTS)
                        ? 'not-allowed'
                        : 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      boxShadow: (isLoading || !warningAccepted || !customInterest.name || totalSelected >= MAX_TOTAL_INTERESTS)
                        ? 'none'
                        : '0 3px 10px rgba(255, 107, 53, 0.3)',
                      transition: 'all 0.2s',
                      opacity: (isLoading || !warningAccepted || !customInterest.name || totalSelected >= MAX_TOTAL_INTERESTS)
                        ? 0.6
                        : 1,
                      boxSizing: 'border-box'
                    }}
                    onMouseEnter={(e) => {
                      if (!isLoading && warningAccepted && customInterest.name && totalSelected < MAX_TOTAL_INTERESTS) {
                        e.currentTarget.style.transform = 'translateY(-2px)';
                        e.currentTarget.style.boxShadow = '0 5px 14px rgba(255, 107, 53, 0.4)';
                      }
                    }}
                    onMouseLeave={(e) => {
                      if (!isLoading && warningAccepted && customInterest.name && totalSelected < MAX_TOTAL_INTERESTS) {
                        e.currentTarget.style.transform = 'translateY(0)';
                        e.currentTarget.style.boxShadow = '0 3px 10px rgba(255, 107, 53, 0.3)';
                      }
                    }}
                  >
                    {isLoading ? 'Adding...' : totalSelected >= MAX_TOTAL_INTERESTS ? '🔒 Limit Reached' : '✨ Add Custom Interest'}
                  </button>
                </form>
              </div>
            </div>
          )}

          {/* REPORT TAB */}
          {activeTab === 'report' && (
            <div>
              {/* Clean Header */}
              <div style={{ marginBottom: '32px' }}>
                <h3 style={{
                  fontSize: '28px',
                  fontWeight: '700',
                  color: '#1d1d1f',
                  marginBottom: '8px',
                  letterSpacing: '-0.02em'
                }}>
                  Progress Report
                </h3>
                <p style={{
                  fontSize: '17px',
                  color: '#86868b',
                  marginBottom: 0,
                  lineHeight: '1.5',
                  fontWeight: '400'
                }}>
                  Generate insights about your journey.
                </p>
              </div>

              {/* Report Options - Minimalist Cards */}
              <div style={{ marginBottom: '32px' }}>
                <h4 style={{
                  fontSize: '13px',
                  fontWeight: '600',
                  color: '#86868b',
                  marginBottom: '16px',
                  textTransform: 'uppercase',
                  letterSpacing: '0.06em'
                }}>
                  Report Contents
                </h4>

                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
                  gap: '12px'
                }}>
                  {[
                    { key: 'includeActiveHabits', icon: '🎯', label: 'Active Habits' },
                    { key: 'includeCompletedHabits', icon: '✓', label: 'Completed' },
                    { key: 'includeInterests', icon: '⭐', label: 'Interests' },
                    { key: 'includeMediaLibrary', icon: '🎬', label: 'Media' },
                    { key: 'includeCounters', icon: '📊', label: 'Counters' }
                  ].map(option => {
                    const isSelected = reportSettings[option.key];
                    return (
                      <button
                        key={option.key}
                        onClick={() => !isLoading && handleReportSettingChange(option.key)}
                        disabled={isLoading}
                        type="button"
                        style={{
                          display: 'flex',
                          flexDirection: 'column',
                          alignItems: 'center',
                          justifyContent: 'center',
                          gap: '10px',
                          padding: '24px 16px',
                          border: isSelected ? '1.5px solid #ff6b35' : '1px solid #d2d2d7',
                          borderRadius: '12px',
                          background: isSelected ? 'rgba(255, 107, 53, 0.05)' : '#ffffff',
                          cursor: isLoading ? 'not-allowed' : 'pointer',
                          transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
                          opacity: isLoading ? 0.5 : 1,
                          position: 'relative'
                        }}
                        onMouseEnter={(e) => {
                          if (!isLoading) {
                            e.currentTarget.style.transform = 'translateY(-2px)';
                            e.currentTarget.style.borderColor = isSelected ? '#ff6b35' : '#a1a1a6';
                            e.currentTarget.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.08)';
                          }
                        }}
                        onMouseLeave={(e) => {
                          if (!isLoading) {
                            e.currentTarget.style.transform = 'translateY(0)';
                            e.currentTarget.style.borderColor = isSelected ? '#ff6b35' : '#d2d2d7';
                            e.currentTarget.style.boxShadow = 'none';
                          }
                        }}
                      >
                        {isSelected && (
                          <div style={{
                            position: 'absolute',
                            top: '8px',
                            right: '8px',
                            width: '18px',
                            height: '18px',
                            borderRadius: '50%',
                            background: '#ff6b35',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: 'white',
                            fontSize: '11px',
                            fontWeight: '700'
                          }}>
                            ✓
                          </div>
                        )}
                        <span style={{
                          fontSize: '28px',
                          filter: isSelected ? 'none' : 'grayscale(0.3) opacity(0.7)',
                          transition: 'filter 0.2s'
                        }}>
                          {option.icon}
                        </span>
                        <div style={{
                          fontSize: '15px',
                          fontWeight: isSelected ? '600' : '500',
                          color: isSelected ? '#1d1d1f' : '#6e6e73',
                          textAlign: 'center',
                          lineHeight: '1.3'
                        }}>
                          {option.label}
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Automatic Reports - Clean Section */}
              <div style={{
                background: '#f5f5f7',
                borderRadius: '16px',
                padding: '24px',
                marginBottom: '32px'
              }}>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  marginBottom: reportSettings.automaticReports ? '24px' : '0'
                }}>
                  <div>
                    <h4 style={{
                      fontSize: '17px',
                      fontWeight: '600',
                      color: '#1d1d1f',
                      margin: 0,
                      marginBottom: '4px'
                    }}>
                      Automatic Delivery
                    </h4>
                    <p style={{
                      fontSize: '14px',
                      color: '#86868b',
                      margin: 0
                    }}>
                      Schedule regular reports
                    </p>
                  </div>

                  <div
                    onClick={() => !isLoading && handleReportSettingChange('automaticReports')}
                    style={{
                      width: '51px',
                      height: '31px',
                      borderRadius: '15.5px',
                      background: reportSettings.automaticReports ? '#ff6b35' : '#d1d1d6',
                      cursor: isLoading ? 'not-allowed' : 'pointer',
                      position: 'relative',
                      transition: 'background-color 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                      opacity: isLoading ? 0.5 : 1
                    }}
                  >
                    <div style={{
                      width: '27px',
                      height: '27px',
                      borderRadius: '50%',
                      background: '#ffffff',
                      position: 'absolute',
                      top: '2px',
                      left: reportSettings.automaticReports ? '22px' : '2px',
                      transition: 'left 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                      boxShadow: '0 3px 8px rgba(0, 0, 0, 0.15), 0 1px 3px rgba(0, 0, 0, 0.3)'
                    }} />
                  </div>
                </div>

                {reportSettings.automaticReports && (
                  <div style={{
                    paddingTop: '24px',
                    borderTop: '1px solid #d2d2d7'
                  }}>
                    <p style={{
                      fontSize: '13px',
                      color: '#86868b',
                      marginBottom: '12px',
                      fontWeight: '600',
                      textTransform: 'uppercase',
                      letterSpacing: '0.06em'
                    }}>
                      Frequency
                    </p>
                    <div style={{ display: 'flex', gap: '12px', marginBottom: '16px' }}>
                      {[
                        { value: 'weekly', label: 'Weekly' },
                        { value: 'monthly', label: 'Monthly' }
                      ].map(freq => {
                        const isActive = reportSettings.reportFrequency === freq.value;
                        return (
                          <button
                            key={freq.value}
                            onClick={() => !isLoading && handleFrequencyChange(freq.value)}
                            disabled={isLoading}
                            style={{
                              flex: 1,
                              padding: '12px',
                              border: '1px solid #d2d2d7',
                              borderRadius: '10px',
                              background: isActive ? '#ffffff' : '#f5f5f7',
                              color: isActive ? '#1d1d1f' : '#86868b',
                              fontSize: '15px',
                              fontWeight: isActive ? '600' : '500',
                              cursor: isLoading ? 'not-allowed' : 'pointer',
                              transition: 'all 0.2s',
                              opacity: isLoading ? 0.5 : 1,
                              boxShadow: isActive ? '0 2px 8px rgba(0, 0, 0, 0.08)' : 'none'
                            }}
                            onMouseEnter={(e) => {
                              if (!isLoading && !isActive) {
                                e.currentTarget.style.background = '#ffffff';
                                e.currentTarget.style.boxShadow = '0 2px 6px rgba(0, 0, 0, 0.06)';
                              }
                            }}
                            onMouseLeave={(e) => {
                              if (!isLoading && !isActive) {
                                e.currentTarget.style.background = '#f5f5f7';
                                e.currentTarget.style.boxShadow = 'none';
                              }
                            }}
                          >
                            {freq.label}
                          </button>
                        );
                      })}
                    </div>
                    <div style={{
                      padding: '12px 16px',
                      background: '#ffffff',
                      borderRadius: '10px',
                      fontSize: '14px',
                      color: '#6e6e73',
                      lineHeight: '1.5'
                    }}>
                      Reports sent to <strong style={{ color: '#1d1d1f' }}>{profileData.email}</strong> every {reportSettings.reportFrequency === 'weekly' ? 'Monday' : '1st of the month'}.
                    </div>
                  </div>
                )}
              </div>

              {/* Action Buttons - Apple Style */}
              <div style={{ display: 'flex', gap: '12px' }}>
                <button
                  onClick={handleSaveReportSettings}
                  disabled={isLoading}
                  style={{
                    flex: 1,
                    padding: '14px 24px',
                    fontSize: '17px',
                    fontWeight: '500',
                    color: '#ff6b35',
                    background: '#ffffff',
                    border: '1px solid #d2d2d7',
                    borderRadius: '12px',
                    cursor: isLoading ? 'not-allowed' : 'pointer',
                    transition: 'all 0.2s',
                    opacity: isLoading ? 0.5 : 1
                  }}
                  onMouseEnter={(e) => {
                    if (!isLoading) {
                      e.currentTarget.style.background = '#f5f5f7';
                      e.currentTarget.style.borderColor = '#ff6b35';
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (!isLoading) {
                      e.currentTarget.style.background = '#ffffff';
                      e.currentTarget.style.borderColor = '#d2d2d7';
                    }
                  }}
                >
                  {isLoading ? 'Saving...' : 'Save Settings'}
                </button>
                <button
                  onClick={handleGenerateReport}
                  disabled={isLoading}
                  style={{
                    flex: 1,
                    padding: '14px 24px',
                    fontSize: '17px',
                    fontWeight: '600',
                    color: '#ffffff',
                    background: isLoading ? '#d2d2d7' : '#ff6b35',
                    border: 'none',
                    borderRadius: '12px',
                    cursor: isLoading ? 'not-allowed' : 'pointer',
                    transition: 'all 0.2s',
                    opacity: isLoading ? 0.5 : 1,
                    boxShadow: isLoading ? 'none' : '0 2px 8px rgba(255, 107, 53, 0.3)'
                  }}
                  onMouseEnter={(e) => {
                    if (!isLoading) {
                      e.currentTarget.style.background = '#ff8247';
                      e.currentTarget.style.transform = 'translateY(-1px)';
                      e.currentTarget.style.boxShadow = '0 4px 12px rgba(255, 107, 53, 0.4)';
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (!isLoading) {
                      e.currentTarget.style.background = '#ff6b35';
                      e.currentTarget.style.transform = 'translateY(0)';
                      e.currentTarget.style.boxShadow = '0 2px 8px rgba(255, 107, 53, 0.3)';
                    }
                  }}
                >
                  {isLoading ? 'Generating...' : 'Generate Report'}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Settings;