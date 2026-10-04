import React, { useEffect } from 'react';
import { useAuth } from 'react-oidc-context';
import { useDispatch } from 'react-redux';
import { setAccessToken, clearAccessToken } from '../features/auth/authSlice';

function AuthTokenManager() {
  const auth = useAuth();
  const dispatch = useDispatch();
  const currentToken = auth.user?.access_token;

  useEffect(() => {
    if (auth.isAuthenticated && currentToken) {
      // User is authenticated and token exists
      console.log("Dispatching new access token to Redux");
      dispatch(setAccessToken(currentToken));
    } else if (!auth.isAuthenticated) {
      // User is not authenticated or logged out
      console.log("Clearing access token in Redux");
      dispatch(clearAccessToken());
    }
    // Add dependencies: dispatch and the token itself (or derived boolean like isAuthenticated)
    // This ensures the effect runs when the auth state or token changes.
  }, [auth.isAuthenticated, currentToken, dispatch]);

  // This component doesn't render anything visual
  return null;
}

export default AuthTokenManager;