# Modelix Kubernetes

Helm chart
and supporting OCI images to run [Modelix Workspaces](https://github.com/modelix/modelix.workspaces) in Kubernetes.

## Deploying Modelix Workspaces

You can deploy the [published Helm chart](https://artifacts.itemis.cloud/repository/helm-modelix/) like this:
```shell
helm install --repo https://artifacts.itemis.cloud/repository/helm-modelix/ dev modelix
```

> [!CAUTION]  
> If you already have the chart installed and want to upgrade its version
> use helm `helm upgrade` and **not** `helm uninstall`.
> In the default setup `helm uninstall` would remove the database and uploaded files.

> [!TIP]
> Locally you can run a Kubernetes Cluster (among other options) with Docker Desktop.
> 
> When doing so, you change the memory limit to 8 GB or more _Resources_.
>
> Also, if you use Docker Desktop >= 4.2.0 you have to add the option `"deprecatedCgroupv1": true`
to the file `~/Library/Group Containers/group.com.docker/settings.json`.
Otherwise, MPS (the JBR) will not use the correct memory limit.

> [!TIP]
> Helm allows you to deploy multiple instances of modelix to the same cluster.
> You could have one instance for testing and one production instance. 
> Just specify a different instance name and hostname when running helm:
>   * `helm install --repo https://artifacts.itemis.cloud/repository/helm-modelix/ xyz modelix --set ingress.hostname=xyz.127.0.0.1.nip.io`
>     * "xyz" is the name of your modelix instance.
>     * "xyz.127.0.0.1.nip.io" is the hostname used to access the modelix instance. In a development environment you can use nip.io to get different hostnames that resolve to 127.0.0.1.


## Developing Modelix Workspaces

This Helm chart is tightly coupled and has to be developed in together with [Modelix Workspace components](https://github.com/modelix/modelix.workspaces).

### Making changes to the Helm chart and OCI images in this repository

1. Set up the project by running:
   ```shell
   ./gradlew
   ```
2. Build your changes by running:
   ```shell
   ./gradlew build
   ```
3. Build OCI images with Docker by running:
   ```shell
   ./docker-build-local-and-publish-on-ci-all.sh
   ```
4. Update the versions in the local helm chart with:
   ```shell
   ./helm/update-versions.sh
   ```
5. Install the Helm chart with the changed images: 
   ```shell
   ./helm/install.sh
   ```

### Making changes to [Modelix Workspace components](https://github.com/modelix/modelix.workspaces)

1. Follow the [instructions in Modelix Workspace components](https://github.com/modelix/modelix.workspaces?tab=readme-ov-file#development) to build OCI images with local changes.

2. Update the `modelixWorkspacesVersion` in [versions.properties](versions.properties) to the version of the locally built images.  
   You can find out the version in the labels of the newly built images or in the `workspaces-version.txt` in your locally checked out [Modelix Workspace components](https://github.com/modelix/modelix.workspaces)
   where you build the images in.
   The versions.properties would the look like:
   ```properties
    # Modelix core version.
    modelixCoreVersion=2.10.5
    # Modelix Workspaces versions
    # 0.0.2-9-gb8651c9 is an example for the versions of Workspace components you want to run the Helm chart with.
    modelixWorkspacesVersion=0.0.2-9-gb8651c9
   ```
3. Update the versions in the local helm chart with:
   ```shell
   ./helm/update-versions.sh
   ```
4. Install the Helm chart with the changed images:
   ```shell
   ./helm/install.sh
   ```
   
   > [!NOTE]  
   > If you already have the chart installed, you can run: 
   > ```shell
   > ./helm/upgrade.sh
   > ```
   > This will only update all images while keeping all data.

## Additional Configuration

### Admin password

After Modelix is deployed, make sure to change the password of the admin account.
- Open http://localhost/admin/master/console/#/realms/master/users
- The default password for the user **admin** is **modelix**.
- Navigate to Edit > Credentials > Reset Password
- Enter a new password and disable *Temporary*

### Node Pools

If you are using a cloud provider that supports auto-scaling create a separate node pool for the MPS instances.
Add a taint "**workspace-client**" with the value "**true**" and the type "**NoExecute**".
This taint ensures that the pool can be scaled down after Modelix stopped the MPS instances
and the nodes don't keep running because some other small pod was scheduled to them.

An MPS instances with a lot of languages and plugins can require ~8 GB of memory.
16 GB of memory for a node is a reasonable size.

## Publishing

Commits published to `main` automatically trigger a new release.

Update the [versions.properties](versions.properties) to update the versions of:
* Modelix Model Server
* Modelix Workspace components