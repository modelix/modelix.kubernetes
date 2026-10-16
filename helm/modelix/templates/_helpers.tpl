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
