import { configureStore } from '@reduxjs/toolkit'
import {emptySplitApi} from "../api/emptyApi.ts";
import authReducer from '../features/auth/authSlice';

export default configureStore({
  reducer: {
    [emptySplitApi.reducerPath]: emptySplitApi.reducer,
    auth: authReducer
  },
  middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(emptySplitApi.middleware),
})
