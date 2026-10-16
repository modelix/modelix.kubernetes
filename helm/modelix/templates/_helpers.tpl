{{/*
Expand the name of the chart.
*/}}
{{- define "modelix.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "modelix.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "modelix.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "modelix.labels" -}}
helm.sh/chart: {{ include "modelix.chart" . }}
{{ include "modelix.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "modelix.selectorLabels" -}}
app.kubernetes.io/name: {{ include "modelix.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "modelix.pullSecret" -}}
{{- if .Values.dockerProxy.secret }}
imagePullSecrets:
- name: {{ .Values.dockerProxy.secret }}
{{- else -}}
{{- if .Values.dockerProxy.secret64 }}
imagePullSecrets:
- name: "{{ include "modelix.fullname" . }}-docker-secret"
{{- end }}
{{- end }}
{{- end }}

{{/*
Full reference of a container image, rewritten to use the configured registry mirrors.
Images without a registry host (Docker Hub) are prefixed with `dockerProxy.prefix`.
Images of other registries are rewritten if `dockerProxy.registryMirrors` contains their registry host.

Parameters:
- root: the root context
- image: a dict with `repository` and an optional `tag`
- defaultTag: used if the image has no tag (optional)
*/}}
{{- define "modelix.image" -}}
{{- $proxy := .root.Values.dockerProxy }}
{{- $tag := .image.tag | default .defaultTag }}
{{- $name := .image.repository }}
{{- if $tag }}
{{- $name = printf "%s:%s" $name $tag }}
{{- end }}
{{- $parts := splitn "/" 2 $name }}
{{- $host := $parts._0 }}
{{- if and $parts._1 (or (contains "." $host) (contains ":" $host) (eq $host "localhost")) }}
{{- $mirror := get ($proxy.registryMirrors | default dict) $host }}
{{- if $mirror }}
{{- printf "%s/%s" (trimSuffix "/" $mirror) $parts._1 }}
{{- else if and $proxy.prefix (eq $host "docker.io") }}
{{- printf "%s%s" $proxy.prefix $parts._1 }}
{{- else }}
{{- $name }}
{{- end }}
{{- else }}
{{- printf "%s%s" $proxy.prefix $name }}
{{- end }}
{{- end }}

{{- define "modelix.serviceAccountName" -}}
{{- if .Values.serviceAccount -}}
{{- .Values.serviceAccount -}}
{{- else -}}
{{- include "modelix.fullname" . -}}-sa
{{- end -}}
{{- end -}}

{{- define "modelix.externalUrl" -}}
https://{{ .Values.ingress.hostname }}{{ if .Values.ingress.port }}:{{ .Values.ingress.port }}{{ end }}/
{{- end }}

{{- define "modelix.externalKeycloakUrl" -}}
{{ include "modelix.externalUrl" . }}
{{- end }}
{{- define "modelix.internalKeycloakUrl" -}}
http://{{ include "modelix.fullname" . }}-keycloak:8080/
{{- end }}

{{- define "modelix.keycloakIdentityProviderSecretEnv" -}}
KEYCLOAK_IDP_{{ . }}_CLIENT_SECRET
{{- end }}

{{/*
The part of keycloak-realm.json that is applied to an existing realm by keycloak-config-cli (see keycloak-realm-sync.yaml),
plus the identity providers from the values.
It contains only the roles and clients of Modelix. The resources that Keycloak creates for every realm (built-in clients,
authentication flows, keys, ...) are left alone. IDs are removed, because the realm may have been created with other IDs.
*/}}
{{- define "modelix.keycloakRealmSyncConfig" -}}
{{- $realm := .Files.Get "keycloak-realm.json" | fromJson }}
{{- $builtInClients := list "account" "account-console" "admin-cli" "broker" "realm-management" "security-admin-console" }}
{{- $builtInRoles := list "offline_access" "uma_authorization" }}

{{- $clients := list }}
{{- $clientIds := list }}
{{- range $realm.clients }}
{{- if not (has .clientId $builtInClients) }}
{{- $client := omit . "id" }}
{{- if eq (toString .secret) "${REALM_CLIENT_SECRET}" }}
{{- $_ := set $client "secret" "$(env:REALM_CLIENT_SECRET)" }}
{{- end }}
{{- if .protocolMappers }}
{{- $mappers := list }}
{{- range .protocolMappers }}
{{- $mappers = append $mappers (omit . "id") }}
{{- end }}
{{- $_ := set $client "protocolMappers" $mappers }}
{{- end }}
{{- $clients = append $clients $client }}
{{- $clientIds = append $clientIds .clientId }}
{{- end }}
{{- end }}

{{- $realmRoles := list }}
{{- range $realm.roles.realm }}
{{- if not (has .name $builtInRoles) }}
{{- $realmRoles = append $realmRoles (omit . "id" "containerId") }}
{{- end }}
{{- end }}
{{- $clientRoles := dict }}
{{- range $clientId, $roles := $realm.roles.client }}
{{- if and $roles (has $clientId $clientIds) }}
{{- $converted := list }}
{{- range $roles }}
{{- $converted = append $converted (omit . "id" "containerId") }}
{{- end }}
{{- $_ := set $clientRoles $clientId $converted }}
{{- end }}
{{- end }}

{{- $result := dict "realm" $realm.realm "enabled" true "roles" (dict "realm" $realmRoles "client" $clientRoles) "clients" $clients }}

{{- $identityProviders := list }}
{{- $identityProviderMappers := list }}
{{- range $index, $idp := .Values.keycloak.identityProviders }}
{{- $alias := required (printf "keycloak.identityProviders[%d].alias is required" $index) $idp.alias }}
{{- $rep := omit $idp "clientSecret" "mappers" }}
{{- if not (hasKey $rep "enabled") }}
{{- $_ := set $rep "enabled" true }}
{{- end }}
{{- /* Keycloak expects all config values to be strings */}}
{{- $config := dict }}
{{- range $key, $value := ($idp.config | default dict) }}
{{- $_ := set $config $key (toString $value) }}
{{- end }}
{{- if $idp.clientSecret }}
{{- $_ := set $config "clientSecret" (printf "$(env:%s)" (include "modelix.keycloakIdentityProviderSecretEnv" $index)) }}
{{- end }}
{{- $_ := set $rep "config" $config }}
{{- $identityProviders = append $identityProviders $rep }}
{{- range ($idp.mappers | default list) }}
{{- $mapperConfig := dict }}
{{- range $key, $value := (.config | default dict) }}
{{- $_ := set $mapperConfig $key (toString $value) }}
{{- end }}
{{- $identityProviderMappers = append $identityProviderMappers (merge (dict "identityProviderAlias" $alias "config" $mapperConfig) (omit . "config")) }}
{{- end }}
{{- end }}
{{- if $identityProviders }}
{{- $_ := set $result "identityProviders" $identityProviders }}
{{- end }}
{{- if $identityProviderMappers }}
{{- $_ := set $result "identityProviderMappers" $identityProviderMappers }}
{{- end }}

{{- $result | toPrettyJson }}
{{- end }}

{{- define "modelix.authorizationConfig" -}}
- name: MODELIX_JWK_URI_KEYCLOAK
  value: "{{ include "modelix.internalKeycloakUrl" . }}realms/{{ .Values.keycloak.realm }}/protocol/openid-connect/certs"
- name: MODELIX_PERMISSION_CHECKS_ENABLED
  value: "{{ .Values.authorization.enabled }}"
{{- end }}

{{/*
Since PostgreSQL 18, the official image stores the data in /var/lib/postgresql/<major version>/docker by default.
PGDATA is set explicitly to keep using the existing data directory of deployments created with older versions.
*/}}
{{- define "modelix.db.env" -}}
- name: POSTGRES_PASSWORD
  value: "{{ .Values.db.password }}"
- name: POSTGRES_USER
  value: "{{ .Values.db.user }}"
- name: POSTGRES_DB
  value: "{{ .Values.db.db }}"
- name: PGDATA
  value: /var/lib/postgresql/data/pgdata
{{- end }}

{{- define "modelix.commonVariables" -}}
{{- if .Values.httpProxy }}
- name: MODELIX_HTTP_PROXY
  value: "{{ .Values.httpProxy }}"
{{- end }}
{{- end }}


{{/*
Address (host:port) from which the kubelet pulls images from the internal docker registry.
*/}}
{{- define "modelix.internalDockerRegistry.pullAddress" -}}
{{- $registry := .Values.internalDockerRegistry }}
{{- if $registry.pullAddress }}
{{- $registry.pullAddress }}
{{- else if eq $registry.service.type "NodePort" }}
{{- printf "localhost:%d" (int $registry.nodePort) }}
{{- else }}
{{- fail (printf "internalDockerRegistry.pullAddress is required if internalDockerRegistry.service.type is %s" $registry.service.type) }}
{{- end }}
{{- end }}
