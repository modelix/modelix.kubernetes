import { createRoot } from 'react-dom/client';
import './assets/style.css';
import 'simplebar-react/dist/simplebar.min.css';
import './assets/third-party/react-table.css';

import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/300.css';
import '@fontsource/roboto/700.css';

import '@fontsource/inter/400.css';
import '@fontsource/inter/500.css';
import '@fontsource/inter/600.css';
import '@fontsource/inter/700.css';

import '@fontsource/poppins/400.css';
import '@fontsource/poppins/500.css';
import '@fontsource/poppins/600.css';
import '@fontsource/poppins/700.css';

import '@fontsource/public-sans/400.css';
import '@fontsource/public-sans/500.css';
import '@fontsource/public-sans/600.css';
import '@fontsource/public-sans/700.css';

import App from './App';
import {AuthProvider} from "react-oidc-context";
import {StrictMode} from "react";
import {Provider} from "react-redux";
import store from './app/store'
import AuthTokenManager from "./components/AuthTokenManager.ts";

const container = document.getElementById('root')!;
const root = createRoot(container);

const keycloakUrl = `https://${window.location.hostname}/realms/modelix/`
const appUrl = `${window.location.protocol}//${window.location.host}${import.meta.env.BASE_URL}`

root.render(
    <StrictMode>
      <AuthProvider
          authority={keycloakUrl}
          client_id={import.meta.env.VITE_OIDC_CLIENT_ID}
          redirect_uri={appUrl}
      >
        <Provider store={store}>
          <AuthTokenManager/>
          <App />
        </Provider>
      </AuthProvider>
    </StrictMode>
);
