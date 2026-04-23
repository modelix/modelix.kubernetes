package org.modelix.services.workspaces

import io.kubernetes.client.common.KubernetesObject
import io.kubernetes.client.openapi.ApiCallback
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1DeploymentList
import io.kubernetes.client.openapi.models.V1DeploymentSpec
import io.kubernetes.client.openapi.models.V1Job
import io.kubernetes.client.openapi.models.V1JobSpec
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.kubernetes.client.openapi.models.V1PodSpec
import io.kubernetes.client.openapi.models.V1PodTemplateSpec
import io.kubernetes.client.openapi.models.V1ServiceList
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

fun KubernetesObject.metadata(body: V1ObjectMeta.() -> Unit): V1ObjectMeta {
    return (metadata ?: V1ObjectMeta().also { setMetadata(it) }).apply(body)
}
fun V1PodTemplateSpec.metadata(body: V1ObjectMeta.() -> Unit): V1ObjectMeta {
    return (metadata ?: V1ObjectMeta().also { setMetadata(it) }).apply(body)
}

fun KubernetesObject.setMetadata(data: V1ObjectMeta) {
    val method = this::class.java.getMethod("setMetadata", V1ObjectMeta::class.java)
    method.invoke(this, data)
}

fun V1Deployment.spec(body: V1DeploymentSpec.() -> Unit): V1DeploymentSpec {
    return (spec ?: V1DeploymentSpec().also { spec = it }).apply(body)
}

fun V1Job.spec(body: V1JobSpec.() -> Unit): V1JobSpec {
    return (spec ?: V1JobSpec().also { spec = it }).apply(body)
}

fun V1JobSpec.template(body: V1PodTemplateSpec.() -> Unit): V1PodTemplateSpec {
    return (template ?: V1PodTemplateSpec().also { template = it }).apply(body)
}

fun V1PodTemplateSpec.spec(body: V1PodSpec.() -> Unit): V1PodSpec {
    return (spec ?: V1PodSpec().also { spec = it }).apply(body)
}

class ContinuingCallback<T>(val continuation: Continuation<T>) : ApiCallback<T> {
    override fun onDownloadProgress(p0: Long, p1: Long, p2: Boolean) {}
    override fun onUploadProgress(p0: Long, p1: Long, p2: Boolean) {}

    override fun onFailure(
        ex: ApiException,
        p1: Int,
        p2: Map<String?, List<String?>?>?,
    ) {
        continuation.resumeWith(Result.failure<T>(ex))
    }

    override fun onSuccess(
        returnedValue: T,
        p1: Int,
        p2: Map<String?, List<String?>?>?,
    ) {
        continuation.resumeWith(Result.success(returnedValue))
    }
}

suspend fun AppsV1Api.APIcreateNamespacedDeploymentRequest.executeSuspending(): V1Deployment =
    suspendCoroutine { executeAsync(ContinuingCallback(it)) }
suspend fun CoreV1Api.APIlistNamespacedServiceRequest.executeSuspending(): V1ServiceList =
    suspendCoroutine { executeAsync(ContinuingCallback(it)) }
suspend fun AppsV1Api.APIlistNamespacedDeploymentRequest.executeSuspending(): V1DeploymentList =
    suspendCoroutine { executeAsync(ContinuingCallback(it)) }
