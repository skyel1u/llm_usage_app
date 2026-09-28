package com.skye.llmusage.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** 业务级错误,消息已本地化,可直接展示 */
class UsageException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal fun networkMessage(e: Throwable): String = when (e) {
    is UnknownHostException -> "无法连接服务器,请检查网络"
    is SocketTimeoutException -> "连接超时"
    else -> "网络错误: ${e.message ?: e.javaClass.simpleName}"
}

internal suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            cont.resume(response)
        }

        override fun onFailure(call: Call, e: IOException) {
            cont.resumeWithException(UsageException(networkMessage(e), e))
        }
    })
    cont.invokeOnCancellation { runCatching { cancel() } }
}

/**
 * 统一 GET:构建请求(认证头 + 附加头)→ 状态码归一 → 读响应体。
 * 401/403 统一报认证失败,其余非 2xx 抛 HTTP code;IO 层错误经 [networkMessage] 本地化。
 */
internal suspend fun httpGet(
    client: OkHttpClient,
    url: String,
    auth: String,
    extraHeaders: Map<String, String> = emptyMap(),
): String = withContext(Dispatchers.IO) {
    val builder = Request.Builder().url(url).header("Authorization", auth)
    extraHeaders.forEach { (name, value) -> builder.header(name, value) }
    client.newCall(builder.build()).await().use { resp ->
        when (resp.code) {
            401, 403 -> throw UsageException("认证失败 (HTTP ${resp.code}),请检查 API Key")
        }
        if (!resp.isSuccessful) throw UsageException("HTTP ${resp.code}")
        resp.body!!.string()
    }
}
