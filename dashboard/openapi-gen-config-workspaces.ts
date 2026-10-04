import type { ConfigFile } from '@rtk-query/codegen-openapi'

const config: ConfigFile = {
  schemaFile: "../openapi/redocly/build/bundled/workspaces-v1.yaml",
  apiFile: "./src/api/emptyApi.ts",
  apiImport: "emptySplitApi",
  outputFile: "./src/api/workspacesApi.ts",
  exportName: "workspacesApi",
  hooks: true,
  tag: true,
}

export default config
