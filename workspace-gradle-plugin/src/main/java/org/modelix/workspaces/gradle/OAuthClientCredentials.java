package org.modelix.workspaces.gradle;

import org.gradle.api.provider.Property;

/**
 * Credentials for the OAuth client credentials flow.
 */
public abstract class OAuthClientCredentials {
    /**
     * Token endpoint. Defaults to the Keycloak realm "modelix" of the Modelix instance:
     * &lt;serverUrl&gt;/realms/modelix/protocol/openid-connect/token
     */
    public abstract Property<String> getTokenUrl();

    public abstract Property<String> getClientId();

    /**
     * Defaults to the environment variable MODELIX_OAUTH_CLIENT_SECRET.
     */
    public abstract Property<String> getClientSecret();
}
