import type { ConfigFile } from '@rtk-query/codegen-openapi'

const config: ConfigFile = {
  schemaFile: "../openapi/redocly/build/bundled/git-connector-v1.yaml",
  apiFile: "./src/api/emptyApi.ts",
  apiImport: "emptySplitApi",
  outputFile: "./src/api/gitConnectorApi.ts",
  exportName: "gitConnectorApi",
  hooks: true,
  tag: true,
}

export default config
