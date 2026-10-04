// Or from '@reduxjs/toolkit/query' if not using the auto-generated hooks
import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'
import {selectAccessToken} from "../features/auth/authSlice.ts";

// initialize an empty api service that we'll inject endpoints into later as needed
export const emptySplitApi = createApi({
  baseQuery: fetchBaseQuery({
    baseUrl: `${window.location.protocol}//${window.location.host}`,
    prepareHeaders: (headers, { getState }: any) => {
      // Get the token from the Redux store
      const token = selectAccessToken(getState()); // Use the selector
      // Or directly access: const token = getState().auth.accessToken;

      // If we have a token set in state, let's assume that we should be passing it.
      if (token) {
        headers.set('authorization', `Bearer ${token}`);
      }

      // You might want to set other default headers here too
      headers.set('Content-Type', 'application/json');
      headers.set('Accept', 'application/json');


      return headers;
    },
  }),
  endpoints: () => ({}),
})
