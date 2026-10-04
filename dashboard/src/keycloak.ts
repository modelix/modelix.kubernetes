import Keycloak from "keycloak-js";

export const keycloakInstance: Keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL as string,
  realm: import.meta.env.VITE_KEYCLOAK_REALM as string,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT as string,
})

const authenticated = await keycloakInstance.init({
  onLoad: 'login-required',
})
