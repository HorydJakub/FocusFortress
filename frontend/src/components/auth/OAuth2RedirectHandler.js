import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

const OAuth2RedirectHandler = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const token = searchParams.get('token');
    // Parse the onboarding flag from URL (it comes as a string "true")
    const needsOnboarding = searchParams.get('onboarding') === 'true';

    if (token) {
      // Store token in localStorage
      localStorage.setItem('token', token);

      // Decode JWT to get user email for display purposes
      try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));

        const payload = JSON.parse(jsonPayload);
        const email = payload.sub;

        localStorage.setItem('user', email);
      } catch (e) {
        console.error("Token decoding error:", e);
        localStorage.setItem('user', 'Google User');
      }

      // Logic to redirect based on onboarding status
      if (needsOnboarding) {
        console.log("User needs onboarding, redirecting to interest selection...");
        window.location.href = '/onboarding';
      } else {
        // Standard flow for existing users
        window.location.href = '/dashboard';
      }

    } else {
      console.error("No token found in redirect URL");
      navigate('/login');
    }
  }, [navigate, searchParams]);

  return (
    <div style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      height: '100vh',
      fontSize: '18px',
      color: '#666'
    }}>
      Logging in...
    </div>
  );
};

export default OAuth2RedirectHandler;