import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { AlertCircle, ChevronRight } from 'lucide-react';

const AuthPage = () => {
  const [isLogin, setIsLogin] = useState(true);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    dateOfBirth: '',
    gender: '',
    password: '',
    confirmPassword: '',
    selectedInterests: []
  });
  const [rememberMe, setRememberMe] = useState(false);
  const [availableInterests, setAvailableInterests] = useState([]);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const { login, register } = useAuth();
  const navigate = useNavigate();

  // Fetch available interests when component mounts (only needed for registration)
  useEffect(() => {
    const fetchInterests = async () => {
      try {
        console.log('Fetching interests from API...');
        const response = await fetch('http://localhost:8080/api/interests/subcategories');
        console.log('Response status:', response.status);

        if (!response.ok) {
          throw new Error('Failed to fetch interests');
        }

        const data = await response.json();
        console.log('Received data:', data);
        console.log('Is array?', Array.isArray(data));

        if (Array.isArray(data)) {
          setAvailableInterests(data);
          console.log('Set interests:', data.length, 'items');
        } else {
          console.error('Data is not an array:', data);
          setAvailableInterests([]);
        }
      } catch (error) {
        console.error('Error fetching interests:', error);
        setAvailableInterests([]);
      }
    };

    if (!isLogin) {
      fetchInterests();
    }
  }, [isLogin]);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
    if (errors[e.target.name]) {
      setErrors({
        ...errors,
        [e.target.name]: ''
      });
    }
  };

  const toggleInterest = (interestName) => {
    const currentInterests = formData.selectedInterests;
    let newInterests;

    if (currentInterests.includes(interestName)) {
      newInterests = currentInterests.filter(i => i !== interestName);
    } else {
      if (currentInterests.length >= 7) {
        setErrors({ ...errors, interests: 'Maximum 7 interests allowed' });
        return;
      }
      newInterests = [...currentInterests, interestName];
    }

    setFormData({
      ...formData,
      selectedInterests: newInterests
    });

    if (errors.interests) {
      setErrors({ ...errors, interests: '' });
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!isLogin) {
      if (!formData.name || formData.name.length < 2) {
        newErrors.name = 'Name must be at least 2 characters';
      }
      if (formData.password !== formData.confirmPassword) {
        newErrors.confirmPassword = 'Passwords do not match';
      }
      if (formData.selectedInterests.length < 3) {
        newErrors.interests = 'Please select at least 3 interests';
      }
      if (formData.selectedInterests.length > 7) {
        newErrors.interests = 'Please select maximum 7 interests';
      }
    }

    if (!formData.email || !formData.email.includes('@')) {
      newErrors.email = 'Valid email is required';
    }
    if (!formData.password || formData.password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');

    if (!validateForm()) {
      return;
    }

    setIsLoading(true);

    try {
      if (isLogin) {
        await login(formData.email, formData.password, rememberMe);
        navigate('/dashboard');
      } else {
        await register({
          name: formData.name,
          email: formData.email,
          dateOfBirth: formData.dateOfBirth || null,
          gender: formData.gender || null,
          password: formData.password,
          confirmPassword: formData.confirmPassword,
          selectedInterests: formData.selectedInterests
        });
        setMessage('Registration successful! Please check your email to verify your account.');
        setTimeout(() => {
          setIsLogin(true);
          setFormData({
            name: '',
            email: '',
            dateOfBirth: '',
            gender: '',
            password: '',
            confirmPassword: '',
            selectedInterests: []
          });
        }, 3000);
      }
    } catch (error) {
      setMessage(error.toString() || 'An error occurred. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const toggleMode = () => {
    setIsLogin(!isLogin);
    setErrors({});
    setMessage('');
    setRememberMe(false);
    setFormData({
      name: '',
      email: '',
      dateOfBirth: '',
      gender: '',
      password: '',
      confirmPassword: '',
      selectedInterests: []
    });
  };

  const selectedCount = formData.selectedInterests.length;
  const minRequired = 3;
  const maxAllowed = 7;

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '20px',
      background: 'linear-gradient(135deg, #fff5eb 0%, #ffe8d6 100%)'
    }}>
      <div style={{ maxWidth: '700px', width: '100%' }}>
        <div style={{
          background: 'white',
          borderRadius: '24px',
          boxShadow: '0 20px 60px rgba(255, 107, 53, 0.15)',
          overflow: 'hidden'
        }}>
          <div style={{
            background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
            color: 'white',
            padding: '50px 20px',
            textAlign: 'center'
          }}>
            <h1 style={{ fontSize: '48px', fontWeight: 'bold', margin: '0 0 20px 0', letterSpacing: '-1px' }}>
              FocusFortress
            </h1>
            <p style={{ margin: 0, opacity: 0.95, fontSize: '18px', fontStyle: 'italic', fontWeight: '300' }}>
              "The journey of a thousand miles begins with one step."
            </p>
            <p style={{ margin: '8px 0 0 0', opacity: 0.85, fontSize: '14px', fontWeight: '500' }}>
              — Lao Tzu
            </p>
          </div>

          <div style={{ padding: '40px 20px' }}>
            {message && (
              <div style={{
                padding: '16px',
                borderRadius: '12px',
                marginBottom: '24px',
                display: 'flex',
                alignItems: 'center',
                backgroundColor: message.includes('successful') ? '#d4edda' : '#f8d7da',
                color: message.includes('successful') ? '#155724' : '#721c24'
              }}>
                <AlertCircle size={20} style={{ marginRight: '12px', flexShrink: 0 }} />
                <span>{message}</span>
              </div>
            )}

            <div style={{ textAlign: 'center', marginBottom: '32px' }}>
              <h2 style={{ fontSize: '24px', fontWeight: 'bold', color: '#ff6b35', margin: '0 0 8px 0' }}>
                {isLogin ? 'Welcome Back!' : 'Create Account'}
              </h2>
              <p style={{ margin: 0, color: '#666' }}>
                {isLogin ? 'Sign in to continue your journey' : 'Start your habit-building journey'}
              </p>
            </div>

            <form onSubmit={handleSubmit}>
              {!isLogin && (
                <div style={{ marginBottom: '20px' }}>
                  <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                    Name
                  </label>
                  <input
                    type="text"
                    name="name"
                    value={formData.name}
                    onChange={handleChange}
                    placeholder="Enter your name"
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
              )}

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                  Email
                </label>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  placeholder="Enter your email"
                  disabled={isLoading}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    fontSize: '16px',
                    borderRadius: '12px',
                    border: `2px solid ${errors.email ? '#dc3545' : '#e0e0e0'}`,
                    outline: 'none',
                    transition: 'border-color 0.2s',
                    opacity: isLoading ? 0.6 : 1,
                    boxSizing: 'border-box'
                  }}
                  onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
                  onBlur={(e) => e.target.style.borderColor = errors.email ? '#dc3545' : '#e0e0e0'}
                />
                {errors.email && <div style={{ color: '#dc3545', fontSize: '14px', marginTop: '4px' }}>{errors.email}</div>}
              </div>

              {!isLogin && (
                <>
                  <div style={{ marginBottom: '20px' }}>
                    <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                      Date of Birth <span style={{ fontWeight: 'normal', color: '#999', fontSize: '14px' }}>(Optional)</span>
                    </label>
                    <input
                      type="date"
                      name="dateOfBirth"
                      value={formData.dateOfBirth}
                      onChange={handleChange}
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
                  </div>

                  <div style={{ marginBottom: '20px' }}>
                    <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                      Gender <span style={{ fontWeight: 'normal', color: '#999', fontSize: '14px' }}>(Optional)</span>
                    </label>
                    <select
                      name="gender"
                      value={formData.gender}
                      onChange={handleChange}
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
                </>
              )}

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                  Password
                </label>
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="Enter your password"
                  disabled={isLoading}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    fontSize: '16px',
                    borderRadius: '12px',
                    border: `2px solid ${errors.password ? '#dc3545' : '#e0e0e0'}`,
                    outline: 'none',
                    transition: 'border-color 0.2s',
                    opacity: isLoading ? 0.6 : 1,
                    boxSizing: 'border-box'
                  }}
                  onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
                  onBlur={(e) => e.target.style.borderColor = errors.password ? '#dc3545' : '#e0e0e0'}
                />
                {errors.password && <div style={{ color: '#dc3545', fontSize: '14px', marginTop: '4px' }}>{errors.password}</div>}
              </div>

              {!isLogin && (
                <>
                  <div style={{ marginBottom: '24px' }}>
                    <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                      Confirm Password
                    </label>
                    <input
                      type="password"
                      name="confirmPassword"
                      value={formData.confirmPassword}
                      onChange={handleChange}
                      placeholder="Confirm your password"
                      disabled={isLoading}
                      style={{
                        width: '100%',
                        padding: '12px 16px',
                        fontSize: '16px',
                        borderRadius: '12px',
                        border: `2px solid ${errors.confirmPassword ? '#dc3545' : '#e0e0e0'}`,
                        outline: 'none',
                        transition: 'border-color 0.2s',
                        opacity: isLoading ? 0.6 : 1,
                        boxSizing: 'border-box'
                      }}
                      onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
                      onBlur={(e) => e.target.style.borderColor = errors.confirmPassword ? '#dc3545' : '#e0e0e0'}
                    />
                    {errors.confirmPassword && <div style={{ color: '#dc3545', fontSize: '14px', marginTop: '4px' }}>{errors.confirmPassword}</div>}
                  </div>

                  {/* INTERESTS SECTION */}
                  <div style={{ marginBottom: '24px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                      <label style={{ fontWeight: '600', color: '#333', margin: 0, fontSize: '15px' }}>
                        Select Your Interests
                      </label>
                      <div style={{
                        fontSize: '15px',
                        fontWeight: '700',
                        color: selectedCount >= minRequired ? '#4caf50' : '#ff9800',
                        background: selectedCount >= minRequired
                          ? 'linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%)'
                          : 'linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%)',
                        padding: '4px 12px',
                        borderRadius: '12px',
                        border: `2px solid ${selectedCount >= minRequired ? '#4caf50' : '#ff9800'}`
                      }}>
                        {selectedCount} / {maxAllowed}
                      </div>
                    </div>

                    <div style={{
                      fontSize: '13px',
                      color: '#666',
                      marginBottom: '16px',
                      fontStyle: 'italic'
                    }}>
                      {selectedCount < minRequired
                        ? `Choose at least ${minRequired - selectedCount} more topic${minRequired - selectedCount > 1 ? 's' : ''} to continue`
                        : selectedCount === maxAllowed
                        ? '✓ Perfect selection!'
                        : `Great! You can pick ${maxAllowed - selectedCount} more`}
                    </div>

                    {errors.interests && (
                      <div style={{
                        padding: '10px 14px',
                        borderRadius: '10px',
                        marginBottom: '12px',
                        backgroundColor: '#fee',
                        color: '#c33',
                        fontSize: '13px',
                        fontWeight: '500',
                        border: '2px solid #fcc'
                      }}>
                        ⚠️ {errors.interests}
                      </div>
                    )}

                    {/* Interest Bubbles */}
                    <div style={{
                      display: 'flex',
                      flexWrap: 'wrap',
                      gap: '12px',
                      maxHeight: '320px',
                      overflowY: 'auto',
                      padding: '16px',
                      border: `2px solid ${errors.interests ? '#fcc' : '#e8e8e8'}`,
                      borderRadius: '16px',
                      background: 'linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%)',
                      boxShadow: 'inset 0 2px 4px rgba(0,0,0,0.05)'
                    }}>
                      {availableInterests.length === 0 ? (
                        <div style={{
                          width: '100%',
                          textAlign: 'center',
                          padding: '30px 20px',
                          color: '#999',
                          fontSize: '14px'
                        }}>
                          <div style={{ fontSize: '32px', marginBottom: '12px' }}>🔄</div>
                          Loading interests...
                        </div>
                      ) : (
                        availableInterests.map((interest) => {
                          const isSelected = formData.selectedInterests.includes(interest.name);

                          return (
                            <button
                              key={interest.name}
                              onClick={() => toggleInterest(interest.name)}
                              disabled={isLoading}
                              type="button"
                              style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px',
                                padding: '12px 18px',
                                border: `2px solid ${isSelected ? '#ff6b35' : 'transparent'}`,
                                borderRadius: '24px',
                                background: isSelected
                                  ? 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)'
                                  : 'white',
                                color: isSelected ? 'white' : '#333',
                                fontSize: '14.5px',
                                fontWeight: isSelected ? '600' : '500',
                                cursor: isLoading ? 'not-allowed' : 'pointer',
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                opacity: isLoading ? 0.6 : 1,
                                boxShadow: isSelected
                                  ? '0 8px 16px rgba(255, 107, 53, 0.35), 0 3px 6px rgba(255, 107, 53, 0.25)'
                                  : '0 2px 4px rgba(0,0,0,0.08)',
                                transform: isSelected ? 'translateY(-2px) scale(1.02)' : 'translateY(0) scale(1)'
                              }}
                              onMouseEnter={(e) => {
                                if (!isLoading) {
                                  if (!isSelected) {
                                    e.currentTarget.style.borderColor = '#ff6b35';
                                    e.currentTarget.style.background = 'linear-gradient(135deg, #fff5eb 0%, #ffe8d6 100%)';
                                    e.currentTarget.style.transform = 'translateY(-3px) scale(1.05)';
                                    e.currentTarget.style.boxShadow = '0 6px 12px rgba(255, 107, 53, 0.2)';
                                  } else {
                                    e.currentTarget.style.transform = 'translateY(-3px) scale(1.05)';
                                    e.currentTarget.style.boxShadow = '0 12px 24px rgba(255, 107, 53, 0.4), 0 6px 12px rgba(255, 107, 53, 0.3)';
                                  }
                                }
                              }}
                              onMouseLeave={(e) => {
                                if (!isLoading) {
                                  if (!isSelected) {
                                    e.currentTarget.style.borderColor = 'transparent';
                                    e.currentTarget.style.background = 'white';
                                    e.currentTarget.style.transform = 'translateY(0) scale(1)';
                                    e.currentTarget.style.boxShadow = '0 2px 4px rgba(0,0,0,0.08)';
                                  } else {
                                    e.currentTarget.style.transform = 'translateY(-2px) scale(1.02)';
                                    e.currentTarget.style.boxShadow = '0 8px 16px rgba(255, 107, 53, 0.35), 0 3px 6px rgba(255, 107, 53, 0.25)';
                                  }
                                }
                              }}
                            >
                              <span style={{
                                fontSize: '20px',
                                filter: isSelected ? 'drop-shadow(0 2px 2px rgba(0,0,0,0.2))' : 'none',
                                transition: 'transform 0.3s ease',
                                display: 'inline-block'
                              }}>
                                {interest.icon}
                              </span>
                              <span style={{
                                letterSpacing: '0.01em',
                                textShadow: isSelected ? '0 1px 2px rgba(0,0,0,0.1)' : 'none'
                              }}>
                                {interest.name}
                              </span>
                              {isSelected && (
                                <span style={{
                                  marginLeft: '4px',
                                  fontSize: '16px',
                                  animation: 'checkmark 0.3s ease-in-out'
                                }}>
                                  ✓
                                </span>
                              )}
                            </button>
                          );
                        })
                      )}
                    </div>
                  </div>
                </>
              )}

              {isLogin && (
                <div style={{
                  marginBottom: '20px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  cursor: isLoading ? 'not-allowed' : 'pointer',
                  opacity: isLoading ? 0.6 : 1
                }}
                onClick={() => !isLoading && setRememberMe(!rememberMe)}
                >
                  <div style={{
                    width: '20px',
                    height: '20px',
                    borderRadius: '4px',
                    border: `2px solid ${rememberMe ? '#ff6b35' : '#ddd'}`,
                    background: rememberMe ? 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)' : 'white',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    transition: 'all 0.2s ease',
                    flexShrink: 0,
                    boxShadow: rememberMe ? '0 2px 8px rgba(255, 107, 53, 0.3)' : 'none'
                  }}>
                    {rememberMe && (
                      <svg
                        width="14"
                        height="14"
                        viewBox="0 0 14 14"
                        fill="none"
                        xmlns="http://www.w3.org/2000/svg"
                      >
                        <path
                          d="M11.6666 3.5L5.24992 9.91667L2.33325 7"
                          stroke="white"
                          strokeWidth="2"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                      </svg>
                    )}
                  </div>
                  <label
                    style={{
                      fontSize: '14px',
                      color: '#666',
                      cursor: isLoading ? 'not-allowed' : 'pointer',
                      userSelect: 'none',
                      fontWeight: '500'
                    }}
                  >
                    Remember me on this device
                  </label>
                </div>
              )}

              <button
                type="submit"
                disabled={isLoading}
                style={{
                  width: '100%',
                  padding: '14px',
                  fontSize: '18px',
                  fontWeight: 'bold',
                  color: 'white',
                  background: isLoading ? '#ccc' : 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
                  border: '2px solid transparent',
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
                {isLoading ? 'Loading...' : (isLogin ? 'Sign In' : 'Create Account')}
                {!isLoading && <ChevronRight size={20} style={{ marginLeft: '8px' }} />}
              </button>
            </form>

            {/* GOOGLE LOGIN BUTTON */}
            <div style={{ marginTop: '24px', borderTop: '1px solid #eee', paddingTop: '20px' }}>
              <a
                href="http://localhost:8080/oauth2/authorization/google"
                style={{
                  width: '100%',
                  padding: '12px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '10px',
                  backgroundColor: 'white',
                  border: '1px solid #ddd',
                  borderRadius: '12px',
                  textDecoration: 'none',
                  color: '#333',
                  fontWeight: '600',
                  transition: 'all 0.2s',
                  boxSizing: 'border-box'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = '#f5f5f5';
                  e.currentTarget.style.boxShadow = '0 2px 4px rgba(0,0,0,0.05)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = 'white';
                  e.currentTarget.style.boxShadow = 'none';
                }}
              >
                <img
                  src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg"
                  alt="Google"
                  style={{ width: '20px', height: '20px' }}
                />
                Continue with Google
              </a>
            </div>

            <div style={{ textAlign: 'center', marginTop: '24px' }}>
              <p style={{ margin: 0, color: '#666' }}>
                {isLogin ? "Don't have an account?" : "Already have an account?"}
                <button
                  onClick={toggleMode}
                  disabled={isLoading}
                  type="button"
                  style={{
                    background: 'none',
                    border: 'none',
                    color: '#ff6b35',
                    fontWeight: 'bold',
                    cursor: isLoading ? 'not-allowed' : 'pointer',
                    marginLeft: '8px',
                    textDecoration: 'none',
                    padding: 0,
                    opacity: isLoading ? 0.5 : 1
                  }}
                >
                  {isLogin ? 'Sign Up' : 'Sign In'}
                </button>
              </p>
            </div>
          </div>
        </div>

        <p style={{ textAlign: 'center', color: '#999', marginTop: '24px', fontSize: '14px' }}>
          © 2025 FocusFortress. Build your fortress of good habits.
        </p>
      </div>
    </div>
  );
};

export default AuthPage;