# Build context is the repository root (see proxy/Dockerfile.dockerignore).
# The build stages produce static files only, so they run on the build platform instead of being emulated.

FROM --platform=$BUILDPLATFORM node:24.22.2-alpine@sha256:8a32c8cf649a49915a66e81e7a609223c708d0adabf288ee3f4077d765542f85 AS openapi
WORKDIR /project/openapi/redocly
COPY openapi/redocly/package.json openapi/redocly/package-lock.json ./
RUN npm ci
COPY openapi/specifications/ /project/openapi/specifications/
COPY openapi/redocly/ ./
RUN npm run bundle

FROM --platform=$BUILDPLATFORM node:24.22.2-alpine@sha256:8a32c8cf649a49915a66e81e7a609223c708d0adabf288ee3f4077d765542f85 AS dashboard
ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0
RUN corepack enable
WORKDIR /project/dashboard
COPY dashboard/package.json dashboard/pnpm-lock.yaml dashboard/pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
# Generate the API clients before copying the sources, so that this layer stays cached when only the sources change.
COPY --from=openapi /project/openapi/redocly/build/bundled/ /project/openapi/redocly/build/bundled/
COPY dashboard/openapi-gen-config-*.ts dashboard/tsconfig*.json ./
COPY dashboard/src/api/emptyApi.ts ./src/api/
RUN pnpm run generate-openapi-workspaces \
 && pnpm run generate-openapi-maven-connector \
 && pnpm run generate-openapi-git-connector
COPY dashboard/ ./
RUN pnpm run build

FROM caddy:2.11@sha256:685ae3d7c34d9b5f8caeddc5d14051db7bbd88ff1f5a2fa0c16bd8ca328fb7af
COPY proxy/Caddyfile /etc/caddy/Caddyfile
COPY proxy/index.html /usr/share/caddy/
COPY proxy/public/ /usr/share/caddy/public/
COPY --from=dashboard /project/dashboard/dist/ /usr/share/caddy/modelix/dashboard/
