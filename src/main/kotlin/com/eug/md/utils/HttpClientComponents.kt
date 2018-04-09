package com.eug.md.utils

import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy
import org.apache.http.impl.client.DefaultRedirectStrategy
import org.apache.http.protocol.HttpContext

class PermanentRedirectSupportRedirectStrategy : DefaultRedirectStrategy() {
    companion object {
        private const val SC_PERMANENT_REDIRECT = 308
    }

    override fun isRedirected(request: HttpRequest?, response: HttpResponse?, context: HttpContext?): Boolean {
        val statusCode = response?.statusLine?.statusCode
        return statusCode == SC_PERMANENT_REDIRECT
                || statusCode == HttpStatus.SC_USE_PROXY
                || super.isRedirected(request, response, context)
    }
}

class LimitedKeepAliveStrategy(private val defaultKeepAliveMills: Long) : DefaultConnectionKeepAliveStrategy() {

    override fun getKeepAliveDuration(response: HttpResponse?, context: HttpContext?): Long {
        val headerKeepAliveValue = super.getKeepAliveDuration(response, context)
        if (headerKeepAliveValue < 0) {
            return defaultKeepAliveMills
        }
        return headerKeepAliveValue
    }
}