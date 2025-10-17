import React, { useState } from 'react';
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
    confirmPassword: ''
  });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const { login, register } = useAuth();
  const navigate = useNavigate();

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

  const validateForm = () => {
    const newErrors = {};

    if (!isLogin) {
      if (!formData.name || formData.name.length < 2) {
        newErrors.name = 'Name must be at least 2 characters';
      }
      if (!formData.dateOfBirth) {
        newErrors.dateOfBirth = 'Date of birth is required';
      }
      if (formData.password !== formData.confirmPassword) {
        newErrors.confirmPassword = 'Passwords do not match';
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
        await login(formData.email, formData.password);
        navigate('/dashboard');
      } else {
        await register({
          name: formData.name,
          email: formData.email,
          dateOfBirth: formData.dateOfBirth,
          gender: formData.gender || null,
          password: formData.password,
          confirmPassword: formData.confirmPassword
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
            confirmPassword: ''
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
    setFormData({
      name: '',
      email: '',
      dateOfBirth: '',
      gender: '',
      password: '',
      confirmPassword: ''
    });
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '20px',
      background: 'linear-gradient(135deg, #fff5eb 0%, #ffe8d6 100%)'
    }}>
      <div style={{ maxWidth: '600px', width: '100%' }}>
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
                    Date of Birth
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
                  {errors.dateOfBirth && <div style={{ color: '#dc3545', fontSize: '14px', marginTop: '4px' }}>{errors.dateOfBirth}</div>}
                </div>

                <div style={{ marginBottom: '20px' }}>
                  <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                    Gender
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
            )}

            <button
              onClick={handleSubmit}
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

            <div style={{ textAlign: 'center', marginTop: '24px' }}>
              <p style={{ margin: 0, color: '#666' }}>
                {isLogin ? "Don't have an account?" : "Already have an account?"}
                <button
                  onClick={toggleMode}
                  disabled={isLoading}
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
          © 2024 FocusFortress. Build your fortress of good habits.
        </p>
      </div>
    </div>
  );
};

export default AuthPage;