package com.skye.llmusage.data.api

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
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
