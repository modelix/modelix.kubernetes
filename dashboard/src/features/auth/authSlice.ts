import { createSlice } from '@reduxjs/toolkit';

interface AuthState {
  accessToken: string | null
}

const initialState: AuthState = {
  accessToken: null,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setAccessToken: (state, action) => {
      state.accessToken = action.payload; // payload should be the token string or null
    },
    // Potentially add a clearAccessToken or logout reducer
    clearAccessToken: (state) => {
      state.accessToken = null;
    }
  },
});

export const { setAccessToken, clearAccessToken } = authSlice.actions;

// Selector to easily get the token
export const selectAccessToken = (state: { auth: AuthState }) => state.auth.accessToken;

export default authSlice.reducer;
