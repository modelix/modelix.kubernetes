# Changelog

## [1.6.0](https://github.com/modelix/modelix.kubernetes/compare/1.5.0...1.6.0) (2026-10-16)


### Features

* **helm:** make the exposure of the internal docker registry configurable ([9e7305a](https://github.com/modelix/modelix.kubernetes/commit/9e7305a19d5e4d9662531cbaed350c83afefb413))
* **helm:** replace the ClusterRoleBinding with a namespaced Role ([75b0fa3](https://github.com/modelix/modelix.kubernetes/commit/75b0fa3b3151b2bc63a7c9b7ae111afe67818bba))
* **helm:** support clusters without cluster-scoped RBAC, NodePorts or direct registry access ([89858a3](https://github.com/modelix/modelix.kubernetes/commit/89858a36efe0b84fb6344a4f80b4a3ed3bbe67d5))
* **helm:** support registry mirrors for images of other registries than Docker Hub ([5caf530](https://github.com/modelix/modelix.kubernetes/commit/5caf53086065aad4abf23e56354b8722ee422afc))
